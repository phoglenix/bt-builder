package bt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.Charsets;

public class GlamResult {
	private static final Logger LOGGER = Logger.getLogger(GlamResult.class.getName());
	
	public String consensus;
	/**
	 * Whether a position in the full alignment is a key position (ie strong indication of
	 * particular char(s) at this position according to GLAM)
	 */
	public boolean[] keyPos;
	public String scoreLine;
	/**
	 * Note: may not contain an alignment for each ID. Excluded by GLAM if below certain score
	 * threshold (0?)
	 */
	private final Map<Integer, Alignment> idToAlignment = new HashMap<>();
	/**
	 * List of [array of character frequencies for each character], for each key position in the
	 * aligned sequence
	 */
	public final List<int[]> frequencies = new ArrayList<>();
	/** All characters used and unused, in order from most to least used */
	private final char[] alphabet;
	/** Total number of positions in the alignment - key and non-key positions */
	private int numAlignedPos;
	
	public GlamResult(File glamTxtFile, char[] alphabet, int numCharsUsed) throws IOException {
		LOGGER.info("Parsing result. Alphabet length " + alphabet.length + " numCharsUsed "
				+ numCharsUsed);
		this.alphabet = alphabet;
		// Need to specify charset to read all the extended ASCII chars correctly
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(glamTxtFile), Charsets.ISO_8859_1)) ) {
			int leftMargin = -1;
			int stage = 0;
			String line;
			while ((line = br.readLine()) != null) {
				// read score line eg. "Score: 94654.3  Columns: 23  Sequences: 784"
				if (stage == 0) {
					if (line.startsWith("Score")) {
						this.scoreLine = line;
						stage++;
					}
					continue;
				}
				// read key positions eg. "      ****.**.*.*.**.*.****.***.*****"
				if (stage == 1) {
					String lineTrim = line.trim();
					if (!lineTrim.isEmpty()) {
						leftMargin = line.indexOf('*');
						if (line.indexOf('.') != -1 && line.indexOf('.') < leftMargin) {
							LOGGER.warning("Did actually need to check for . on " + line);
							leftMargin = line.indexOf('.');
						}
						this.keyPos = new boolean[lineTrim.length()];
						for (int i = 0; i < lineTrim.length(); i++) {
							this.keyPos[i] = lineTrim.charAt(i) == '*';
						}
						numAlignedPos = keyPos.length;
						stage++;
					}
					continue;
				}
				// read alignments eg. "seq0       1 R"""."&.".".)"."."&!"."3".!"&"$  23 + 137."
				if (stage == 2) {
					if (!line.isEmpty()) {
						String[] split = line.split(" +");
						if (split.length != 6) {
							LOGGER.warning("Couldn't parse seq " + line);
						} else {
							Alignment a = new Alignment(split);
							Alignment prev = idToAlignment.put(seqNameToId(a.seqName), a);
							if (prev != null) {
								LOGGER.warning("Same alignment id twice: " + a + " and " + prev);
							}
						}
					} else {
						stage++;
					}
					continue;
				}
				// read consensus eg. ' R""" "& " " )" " """" """ """""'
				if (stage == 3) {
					if (!line.isEmpty()) {
						if (leftMargin >= 0 && this.consensus == null) {
							// record only first consensus string if multiple
							this.consensus = line.substring(leftMargin);
						}
					} else {
						stage++;
					}
					continue;
				}
				// read alphabet eg. "!  " $ % & ' ( ) * + , - / 0 Del Ins Score"
				if (stage == 4) {
					if (!line.startsWith(" " + alphabet[0])) {
						LOGGER.warning("Expected alphabet line, instead had: " + line);
					} else {
						String[] split = line.trim().split(" +");
						if (split.length != alphabet.length + 2) { // +3 "Del Ins Score" -1 "?"
							LOGGER.warning("Expected " + alphabet.length + "+2 alphabet items "
									+ "but found " + split.length);
						}
						for (int i = 0; i < split.length - 3 && i < alphabet.length - 1; i++) {
							if (split[i].charAt(0) != alphabet[i]) {
								LOGGER.warning("Alphabet char mismatch " + split[i].charAt(0)
										+ " vs " + alphabet[i]);
							}
						}
					}
					stage++;
					continue;
				}
				// read freq counts eg. " 0  3  0  0  0  0 780  0  0  0  0   1      4.94e+03"
				if (stage == 5) {
					LOGGER.finer("Freq line: " + line);
					String[] split = line.trim().split(" +");
					LOGGER.finer("NumCharsUsed = " + numCharsUsed + " alpha length "
							+ alphabet.length + " split length " + split.length);
					if (split.length > alphabet.length) {
						// freq for each char in alphabet (except wildcard) + Del + Score
						int[] freq = new int[alphabet.length];
						for (int i = 0; i < alphabet.length - 1; i++) { // ignore Del, Score
							freq[i] = Integer.parseInt(split[i]);
						}
						LOGGER.fine("Freq parsed: " + Arrays.toString(freq));
						this.frequencies.add(freq);
					} else if (split.length > 1) {
						// ignore Ins, Score
						if (split.length > 5) {
							LOGGER.warning("Unexpected freq entry " + line);
						}
					} else {
						stage++;
					}
					continue;
				}
				// finished reading first result, ignore the rest
				if (stage == 6) {
					break;
				}
			}
		}
	}
	
	private int seqNameToId(String seqName) {
		// -1 because '>' is removed in the output
		return Integer.parseInt(seqName.substring(BtGlamCodec.SEQ_TAG.length() - 1));
	}
	
	public List<Map<Character, Double>> calcCharProportions(double requiredScore) {
		List<Map<Character, Integer>> charFreqs = calcCharFreqs(requiredScore);
		// Sum the char frequencies for each position (should all be the same)
		int sum = charFreqs.get(0).values().stream().collect(Collectors.summingInt(x->x));
		for (int i = 0; i < numAlignedPos; i++) {
			int sumI = charFreqs.get(i).values().stream().collect(Collectors.summingInt(x->x));
			if (sumI != sum) {
				LOGGER.warning("Column " + i + " had different sum: " + sumI + " expected " + sum);
			}
		}
		
		double sumD = (double) sum;
		List<Map<Character, Double>> charProportions = new ArrayList<>(numAlignedPos);
		for (int i = 0; i < numAlignedPos; i++) {
			Map<Character, Double> prop = charFreqs.get(i).entrySet().stream()
					.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue() / sumD));
			charProportions.add(prop);
		}
		return charProportions;
	}
	
	/** Get a map of character frequencies for each (key and non-key) position in the alignment */
	public List<Map<Character, Integer>> calcCharFreqs(double requiredScore) {
		// Recalculate frequencies for all positions (not just key pos)
		List<Map<Character, Integer>> myFrequencies = new ArrayList<>(numAlignedPos);
		for (int i = 0; i < numAlignedPos; i++) {
			myFrequencies.add(new HashMap<>());
		}
		boolean allAlignmentsIncluded = true;
		for (Alignment a : idToAlignment.values()) {
			if (a.score < requiredScore) {
				LOGGER.info("Excluding alignment " + a.seqName);
				allAlignmentsIncluded = false;
			} else {
				for (int i = 0; i < numAlignedPos; i++) {
					char c = a.align.charAt(i);
					myFrequencies.get(i).merge(c, 1, (x, y) -> x + y);
				}
			}
		}
		// Check that this matches the GLAM frequencies (if we haven't excluded any)
		if (allAlignmentsIncluded) {
			for (int i = 0, offset = 0; i + offset < numAlignedPos; i++) {
				// offset to move over insertions in keypos (not present in freqs)
				while (!keyPos[i + offset]) {
					offset++;
				}
				for (int j = 0; j < alphabet.length; j++) {
					char c = alphabet[j];
					LOGGER.finer("Checking " + c + " i=" + i + " j=" + j);
					int myFreq = myFrequencies.get(i + offset).getOrDefault(c, 0);
					int glamFreq = frequencies.get(i)[j];
					if (myFreq != glamFreq) {
						LOGGER.warning(String.format(
								"Frequency counts mismatch at %d : %d (%c). Mine: %d, Theirs: %d",
								i, j, c, myFreq, glamFreq));
					}
				}
			}
		}
		
		return myFrequencies;
	}
	
	public int getNumAlignedPos() {
		return numAlignedPos;
	}
	
	/**
	 * Gets alignment with the given sequence ID. Note can be null if no alignment for that sequence
	 * was found by GLAM
	 */
	public Alignment getAlignment(int alignmentId) {
		return idToAlignment.get(alignmentId);
	}
	
	public class Alignment {
		public final int start, end;
		public final String seqName, align;
		public final double score;
		
		public Alignment(String[] line) {
			seqName = line[0];
			start = Integer.parseInt(line[1]) - 1; // converting from 1 based indexing
			align = line[2];
			end = Integer.parseInt(line[3]) - 1; // converting from 1 based indexing
			// ignore line[4] == "+"
			score = Double.parseDouble(line[5]);
		}
	}


}