package bt;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.Charsets;

import util.Util.Pair;
import bt.GlamEncodingRecord.EncodedPath;
import bt.GlamResult.Alignment;

public class BtGlamCodec {
	private static final Logger LOGGER = Logger.getLogger(BtGlamCodec.class.getName());
	
	public static final String SEQ_TAG = ">seq";
	
	/**
	 * A value in the range [0, 1] denoting the amount of generalisation that should occur in any
	 * accuracy vs generalisation tradeoff. Low values indicate less generalisation so may result in
	 * a larger BT formed when a tree is reduced using a GLAM motif.<br>
	 * For example, this could represent the frequency proportion required of a character to be
	 * included in the alignment
	 */
	private static final double GENERALISATION_FACTOR = 0.3;
	/**
	 * GLAM includes any motif with a positive score, which seems too permissive and includes some
	 * poorly-matching alignments. Filter out any alignments scoring lower than this value.
	 */
	private static final double REQUIRED_SCORE = 50;

	private static final char UNUSED_CHAR = ' ';
	
	private static final char GLAM_GAP = '.';
	private static final char GLAM_WILDCARD = '?';
	
	private final char[] alphabet;
	private final String rootEncoding;
	private final String leafEncoding;
	private final Function<BtNode, Long> hashFn;
	
	public BtGlamCodec(File alphabetFile, String rootEncoding, String leafEncoding,
			Function<BtNode, Long> hashFn) throws IOException {
		if (rootEncoding == null || leafEncoding == null)
			throw new NullPointerException("use empty string for no root/leaf encoding");
		if (rootEncoding.length() > 1 || leafEncoding.length() > 1)
			throw new RuntimeException("root/leaf encoding should be max one character each");
		this.rootEncoding = rootEncoding;
		this.leafEncoding = leafEncoding;
		this.hashFn = hashFn;
		alphabet = loadAlphabet(alphabetFile);
	}
	
	/** Produce a modified tree using the GLAM output */
	public BehaviourTree modifiedTreeFromGlamFile(GlamEncodingRecord er, File glamTxtFile)
			throws IOException {
		
		GlamResult gr = new GlamResult(glamTxtFile, alphabet, er.numCharsUsed());
		
		int numSequences = er.numEncodedPaths();
		
		// Build the aligned sequence
		LOGGER.info("Creating merged aligned sequence");
		// First, find all the characters used at each position
		List<Map<Character, Double>> allCharProps = gr.calcCharProportions(GENERALISATION_FACTOR);
		List<List<Character>> allUsingChars = new ArrayList<>();
		for (int alignPos = 0; alignPos < gr.getNumAlignedPos(); alignPos++) {
			Map<Character, Double> charProportions = allCharProps.get(alignPos);
			List<Character> usingChars = new ArrayList<>();
			for (Entry<Character, Double> charProportion : charProportions.entrySet()) {
				if (!isSpecialChar(charProportion.getKey())
						&& charProportion.getValue() > GENERALISATION_FACTOR) {
					usingChars.add(charProportion.getKey());
				}
			}
			allUsingChars.add(usingChars);
		}
		// Find/merge the nodes used at each position (note some might remain empty)
		BtNode[] usingNodes = new BtNode[gr.getNumAlignedPos()];
		Map<BtNode, Alignment> modifiedParentToAlignment = new HashMap<>();
		for (int seqId = 0; seqId < numSequences; seqId++) {
			Alignment a = gr.getAlignment(seqId);
			EncodedPath encPath = er.getEncodedPath(seqId);
			if (a != null && a.score > REQUIRED_SCORE) {
				modifiedParentToAlignment.put(encPath.parent, a);
				for (int alignPos = 0; alignPos < gr.getNumAlignedPos(); alignPos++) {
					List<Character> usingChars = allUsingChars.get(alignPos);
					// Find corresponding path position (may be nonexistent)
					int pathPos = findPathIdx(a, alignPos);
					LOGGER.finest(String.format("For seq %d alignPos %d translates to pathPos %d",
							seqId, alignPos, pathPos));
					if (pathPos == -1) {
						continue;
					}
					// Just sanity checking
					char c1 = er.getHashToChar().get(
							hashFn.apply(encPath.getFullPath().get(pathPos)));
					char c2 = er.getEncodedPath(seqId).getFullEnc().charAt(pathPos);
					char c3 = a.align.charAt(alignPos);
					if (c1 != c2 || c2 != c3) {
						LOGGER.warning(String.format("Something wrong with chars: %c, %c, %c"
								+ " with pathpos=%d alignpos=%d seqId=%d align=%s",
								c1, c2, c3, pathPos, alignPos, seqId, a.align));
					}
					
					if (usingChars.contains(c3)) {
						BtNode toMerge = encPath.getFullPath().get(pathPos);
						if (usingNodes[alignPos] == null) {
							usingNodes[alignPos] = toMerge;
						} else {
							BtNode old1 = usingNodes[alignPos];
							BtNode old2 = toMerge;
							old1.checkNotMerged();
							old2.checkNotMerged();
							BtNode newNode = old1.merge(old2);
							newNode.checkNotMerged();
							usingNodes[alignPos] = newNode;
						}
					}
					
				}
			}
		}
		// Create the aligned sequence node (with a selector at the end)
		BtSeqNode alignedSeq = new BtSeqNode();
		for (BtNode n : usingNodes) {
			if (n != null) {
				alignedSeq.addChild(n);
			}
		}
		// TODO make main stopping point clearer!
		if (alignedSeq.getChildren().size() == 0) {
			LOGGER.info("Nothing merged. Stopping.");
			return null;
		}
		BtSelNode lastChild = new BtSelNode();
		alignedSeq.addChild(lastChild);
		LOGGER.info("Aligned seq: " + alignedSeq.toStringRecursive(new HashSet<>()) + " hash: "
				+ Objects.hash(alignedSeq));
		// Create a replacement for each modified parent node
		LOGGER.info("Creating modified parent nodes");
		Map<BtNode, BtNode> replacementParents = new HashMap<>();
		for (Entry<BtNode, Alignment> e : modifiedParentToAlignment.entrySet()) {
			BtNode parent = e.getKey();
			Alignment a = e.getValue();
			BtNode replacementStart = parent.duplicateWithoutChildren();
			BtNode replacementEnd = parent.duplicateWithoutChildren();
			for (int i = 0; i < parent.getChildren().size(); i++) {
				if (i < a.start) {
					replacementStart.addChild(parent.getChildren().get(i));
				} if (i > a.end) {
					replacementEnd.addChild(parent.getChildren().get(i));
				} else if (i == a.start) {
					replacementStart.addChild(alignedSeq);
				}
			}
			if (a.start == 0) {
				// replacementStart has only one child: alignedSeq, so use alignedSeq directly
				replacementParents.put(parent, alignedSeq);
				parent.setMergedInto(alignedSeq);
			} else {
				replacementParents.put(parent, replacementStart);
				parent.setMergedInto(replacementStart);
			}
			if (replacementEnd.getChildren().size() > 0) {
				lastChild.addChild(replacementEnd);
			}
		}
		
		// Replacements may themselves have replacements to be made
		LOGGER.info("Updating replacements");
		Set<BtNode> seen = new HashSet<>();
		for (BtNode replacement : replacementParents.values()) {
			if (replacement.hasBeenMerged()) {
				LOGGER.warning("Replacement value " + replacement + " is set to be replaced again");
			}
			replacement.updateChildren(seen);
		}
		
		// Sanity checking
		LOGGER.info("Sanity checking");
		for (BtNode replacement : new HashSet<>(replacementParents.values())) {
			if (replacement.getWeight() <= 0) {
				throw new RuntimeException("Negative / zero weight on " + replacement);
			}
			Set<BtNode> replacementsInReplacements = BehaviourTree.findNodesDfs(replacement);
			replacementsInReplacements.retainAll(replacementParents.keySet());
			if (!replacementsInReplacements.isEmpty()) {
				LOGGER.warning("Replacement " + replacement + " had "
						+ replacementsInReplacements.size() + " unique nodes to replace "
						+ "maxsize: " + replacementParents.size());
			}
		}

		// Rebuild tree
		LOGGER.info("Creating modified tree");
		BtNode newRoot = er.tree.getRoot();
		if (newRoot.hasBeenMerged()) {
			newRoot = newRoot.getMergedActual();
			LOGGER.info("Replaced root " + er.tree.getRoot() + " with " + newRoot);
		}
		newRoot.updateChildren(seen);
		newRoot.mergeChildren(new HashSet<>());
		// Ensure all children are updated after any merges
		newRoot.updateChildren(new HashSet<>());

		BehaviourTree result = new BehaviourTree(newRoot);
		
		// Add the processed replay records (not really necessary)
		for (String replay : er.tree.getProcessed()) {
			result.setProcessed(replay);
		}
		
		return result;
	}
	
	/** Convert an alignment index, from a gapped alignment string, into the corresponding index
	 * for an ungapped encoding. Returns -1 if there is no corresponding index. */
	private int findPathIdx(Alignment a, int alignIdx) {
		if (a.align.charAt(alignIdx) == GLAM_GAP) {
			// If there's a gap here there can't be a corresponding path index
			return -1;
		}
		int numGaps = 0;
		for (char c : a.align.substring(0, alignIdx).toCharArray()) {
			if (c == GLAM_GAP)
				numGaps++;
		}
		return alignIdx - numGaps + a.start;
	}
	
	private String seqIdToName(int seqId) {
		return SEQ_TAG + seqId;
	}
	
	public GlamEncodingRecord encodeToFile(BehaviourTree tree, File encodedOut) throws IOException {
		Map<Long, Character> hashToChar = makeNodeHashToCharMap(tree);
		
		List<EncodedPath> encoded = encodeFastaFormat(tree, hashToChar);
		
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(encodedOut)) ) {
			int seqCounter = 0;
			for (EncodedPath p : encoded) {
				if (p.encLength > 0) {
					bw.write(seqIdToName(seqCounter));
					bw.newLine();
					bw.write(p.getFullEnc());
					bw.newLine();
				}
				seqCounter++;
			}
		}
		
		return new GlamEncodingRecord(tree, hashToChar, encoded);
	}
	
	/**
	 * Returns one {@link EncodedPath} for each sequence node in the tree. 
	 */
	private List<EncodedPath> encodeFastaFormat(BehaviourTree tree,
			Map<Long, Character> hashToChar) throws IOException {
		List<EncodedPath> result = new ArrayList<>();
		// TODO maybe try using all nodes with children - need better hash function first!
		
		Set<BtSeqNode> sequences = tree.findSeqNodesDfs(tree.getRoot());
		
		for (BtSeqNode seq : sequences) {
			// skip any previously aligned sequences (likely have selectors as children)
			if (seq.getChildren().stream().anyMatch(c -> c instanceof BtSelNode))
				continue;
			StringBuilder encoded = new StringBuilder();
			for (BtNode n : seq.getChildren()) {
				long hash = hashFn.apply(n);
				char c = hashToChar.get(hash);
				encoded.append(c);
			}
			if (encoded.length() == 0) {
				LOGGER.warning("Found seq node with 0 children");
			}
			result.add(new EncodedPath("", Collections.emptyList(),
					encoded.toString(), seq.getChildren(),
					"", Collections.emptyList(), seq));
		}
		
		
		LOGGER.info("Total number of nodes in the tree: " + tree.countNodes());
		return result;
	}
	
	/**
	 * Generate a mapping from each unique (as defined by the hash function) action to a character
	 * in the alphabet. More common actions will be mapped to earlier characters and less common
	 * actions may be left out if the alphabet is not big enough.
	 */
	private Map<Long, Character> makeNodeHashToCharMap(BehaviourTree tree) {
		Map<Long, Integer> hashToCount = new HashMap<>();
		Map<Long, BtNode> hashToNode = new HashMap<>();
		
		Set<BtNode> found = BehaviourTree.findNodesDfs(tree.getRoot());
		for (BtNode node : found) {
			long hash = hashFn.apply(node);
			if (!hashToCount.containsKey(hash)) {
				hashToCount.put(hash, 0);
				if (LOGGER.isLoggable(Level.FINE)) {
					hashToNode.put(hash, node);
				}
			}
			hashToCount.put(hash, hashToCount.get(hash) + 1);
		}
		// pairs of <count,hash>
		List<Pair<Integer, Long>> countHashPairs = hashToCount.entrySet().stream()
				.map(e -> new Pair<>(e.getValue(), e.getKey()))
				.sorted((countHash1, countHash2) -> countHash2.first - countHash1.first)
				.collect(Collectors.toList());
		
		char[] alphabetNoSpecials = withoutSpecialChars(alphabet);
		Map<Long, Character> hashToChar = new HashMap<>();
		int alphaLen = alphabetNoSpecials.length;
		// only use up to alphabet.length - 2, last character is wildcard - assign to any left over
		for (int i = 0; i < countHashPairs.size(); i++) {
			char c = i < alphaLen ? alphabetNoSpecials[i] : GLAM_WILDCARD;
			LOGGER.fine("Putting " + countHashPairs.get(i).second + " with " + c);
			hashToChar.put(countHashPairs.get(i).second, c);
		}
		// any remaining are wildcards
		if (countHashPairs.size() > alphaLen) {
			int leftover = countHashPairs.size() - alphaLen;
			LOGGER.info("Assigned " + leftover + " hashes to wildcard. Max count on those hashes "
					+ countHashPairs.get(alphaLen).first);
		}
		LOGGER.info("There were " + hashToCount.size()
				+ " unique hashes and usable alphabet size was " + alphaLen);
		if (LOGGER.isLoggable(Level.FINE)) {
			String result = "";
			for (Pair<Integer, Long> h : countHashPairs) {
				result += hashToChar.get(h.second) + " -> " + hashToNode.get(h.second) + "\n";
			}
			LOGGER.fine("Char to Node map: \n" + result);
		}
		return hashToChar;
	}
	
	private boolean isSpecialChar(char c) {
		return !rootEncoding.isEmpty() && c == rootEncoding.charAt(0)
				|| !leafEncoding.isEmpty() && c == leafEncoding.charAt(0)
				|| c == GLAM_GAP || c == UNUSED_CHAR || c == GLAM_WILDCARD;
	}
	
	/** Return a new char[] without any special characters, such as the root and leaf encodings. */
	protected char[] withoutSpecialChars(char[] alphabet) {
		int countSpecials = 0;
		for (char c : alphabet) {
			if (isSpecialChar(c)) {
				countSpecials++;
			}
		}
		char[] result = new char[alphabet.length - countSpecials];
		int index = 0;
		for (char c : alphabet) {
			if (isSpecialChar(c)) {
				// skip
			} else {
				result[index] = c;
				index++;
			}
		}
		if (index != result.length) {
			LOGGER.warning("Index didn't reach the end: " + index + " vs " + result.length);
		}
		return result;
	}
	
	/**
	 * Load alphabet from a file. Reading first character on each line in an encoding that allows a
	 * few extra characters. Last character is wildcard/unknown symbol.
	 */
	private char[] loadAlphabet(File alphabetFile) throws IOException {
		List<String> lines = new ArrayList<>();
		try (	BufferedReader br = new BufferedReader(new InputStreamReader(
						new FileInputStream(alphabetFile), Charsets.ISO_8859_1)) ) {
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		}
		char[] result = new char[lines.size()];
		Set<Character> check = new HashSet<>();
		int i = 0;
		for (String line : lines) {
			result[i] = line.charAt(0);
			i++;
			if (!check.add(line.charAt(0))) {
				LOGGER.warning("Char appeared twice in alphabet " + line.charAt(0) + " / " + line
						+ " / " + line.codePointAt(0));
			}
		}
		return result;
	}
	
	protected char[] getAlphabet() {
		return Arrays.copyOf(alphabet, alphabet.length);
	}
	
}
