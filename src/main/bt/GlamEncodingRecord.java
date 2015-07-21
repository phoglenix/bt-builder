package bt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


/** Holds together a tree and its encoding for later decoding. */
public class GlamEncodingRecord {
	private final Map<Long, Character> hashToChar;
	public final BehaviourTree tree;
	private final List<EncodedPath> encodedPaths; // only needed if encoding is nondeterministic
	private final int numCharsUsed;
	
	// maybe should hold hash function as well
	
	public GlamEncodingRecord(BehaviourTree tree, Map<Long, Character> hashToChar,
			List<EncodedPath> encodedPaths) {
		this.hashToChar = hashToChar;
		this.tree = tree;
		this.encodedPaths = encodedPaths;
		numCharsUsed = new HashSet<>(hashToChar.values()).size();
	}
	
	public Map<Long, Character> getHashToChar() {
		return Collections.unmodifiableMap(hashToChar);
	}
	
	public List<EncodedPath> getEncodedPaths() {
		return Collections.unmodifiableList(encodedPaths);
	}
	
	public EncodedPath getEncodedPath(int seqId) {
		return encodedPaths.get(seqId);
	}
	
	public int numCharsUsed() {
		return numCharsUsed;
	}
	
	public int numEncodedPaths() {
		return encodedPaths.size();
	}
	
	/**
	 * Represents an execution path through the tree and the string encoding of that path.<br>
	 * Also contains a prefix and suffix path (and their string encodings) that
	 * are before and after the main path.<br>
	 * The prefix and suffix encodings/paths may contain special characters/null nodes, while the
	 * main path will not contain special characters/null nodes. 
	 */
	// This is very overkill for the simpler encoding of just seq nodes
	// - leave it in for now so we can try again with prefixes later
	public static class EncodedPath {
		private final String enc;
		private final List<BtNode> path;
		public final int prefixLength;
		public final int encLength;
		public final int suffixLength;
		public final BtNode parent;
		
		public EncodedPath(String prefixEnc, List<BtNode> prefixPath, String enc,
				List<BtNode> path, String suffixEnc, List<BtNode> suffixPath, BtNode parent) {
			if (prefixEnc.length() != prefixPath.size()) {
				throw new IllegalArgumentException("prefix sizes must match");
			}
			if (enc.length() != path.size()) {
				throw new IllegalArgumentException("encoding sizes must match");
			}
			if (suffixEnc.length() != suffixPath.size()) {
				throw new IllegalArgumentException("suffix sizes must match");
			}
			this.prefixLength = prefixEnc.length();
			this.encLength = enc.length();
			this.suffixLength = suffixEnc.length();
			
			this.enc = prefixEnc + enc + suffixEnc;
			this.path = new ArrayList<>(prefixLength + encLength + suffixLength);
			this.path.addAll(prefixPath);
			this.path.addAll(path);
			this.path.addAll(suffixPath);
			this.parent = parent;
		}
		
		public String getFullEnc() {
			return enc;
		}
		
		/** The whole path: including the prefix and suffix */
		public List<BtNode> getFullPath() {
			return Collections.unmodifiableList(path);
		}
		
		/** Just the main part of the path: excluding the prefix and suffix */
		public List<BtNode> getEncPath() {
			return Collections.unmodifiableList(
					path.subList(prefixLength, prefixLength + encLength));
		}
	}
}
