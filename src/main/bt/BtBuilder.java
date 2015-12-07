package bt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jnibwapi.types.RaceType.RaceTypes;
import jnibwapi.types.UnitType;
import scdb.*;
import util.LogManager;
import util.Util;
import bt.sc.ActionBtNode;
import bt.sc.CaseBtNode;
import bt.sc.SimilarityMetrics;

// TODO: create way to look up state for given action ID
public class BtBuilder {
	private static final Logger LOGGER = Logger.getLogger(BtBuilder.class.getName());
	
	private final DbInterface dbi;
	
	/** Properties file to load */
	private static final String PROPERTIES_FILENAME = "btMakerConfig.properties";
	
	@SuppressWarnings("unused")
	private final String btFilename;
	private final String btActFilename;
	private final String btActFilenameBase;
	private final String btActFilenameExt;
	/** Save tree only every SAVE_INTERVAL replays so less time is spent writing out trees */
	private final int saveInterval;
	/**
	 * File from which to load usable characters for representing nodes in a sequence so they can be
	 * analysed by an external program (GLAM)
	 */
	private final String alphabetFilename;
	/** Output file for encoded action sequences from the BT */
	private final String encSeqFilename;
	private final String encSeqFilenameExt;
	
	/** Set to non-empty to add a special character to the sequence before the root node. */
	private final String rootEncoding;
	/** Set to non-empty to add a special character to the sequence after each leaf node. */
	private final String leafEncoding;
	/** This folder name will need a number appended each run, starting from 1 */
	private final String glamFoldernameBase;
	private final String glamTxtFilename;
	private final String glamProcessingFlagFile;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		LogManager.initialise("BtMaker");
		
		LOGGER.info("Starting BtMaker");
		try {
			BtBuilder btm = new BtBuilder();
			btm.run();
		} catch (IOException | InterruptedException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		
		LOGGER.info("Finished");
	}
	

	
	public BtBuilder() throws IOException {
		dbi = DbInterface.getInstance();
		
		// Load properties
		Properties p = Util.loadProperties(PROPERTIES_FILENAME);
		
		btFilename = Util.getPropertyNotNull(p, "bt_filename");
		btActFilename = Util.getPropertyNotNull(p, "bt_act_filename");
		btActFilenameBase = Util.getPropertyNotNull(p, "bt_act_filename_base");
		btActFilenameExt = Util.getPropertyNotNull(p, "bt_act_filename_ext");
		/** Save tree only every SAVE_INTERVAL replays so less time is spent writing out trees */
		saveInterval = Integer.parseInt(Util.getPropertyNotNull(p, "save_interval"));
		/**
		 * File from which to load usable characters for representing nodes in a sequence so they can be
		 * analysed by an external program (GLAM)
		 */
		alphabetFilename = Util.getPropertyNotNull(p, "alphabet_filename");
		/** Output file for encoded action sequences from the BT */
		encSeqFilename = Util.getPropertyNotNull(p, "enc_seq_filename");
		encSeqFilenameExt = Util.getPropertyNotNull(p, "enc_seq_filename_ext");
		
		/** Set to non-empty to add a special character to the sequence before the root node. */
		rootEncoding = Util.getPropertyNotNull(p, "root_encoding");
		/** Set to non-empty to add a special character to the sequence after each leaf node. */
		leafEncoding = Util.getPropertyNotNull(p, "leaf_encoding");
		/** This folder name will need a number appended each run, starting from 1 */
		glamFoldernameBase = Util.getPropertyNotNull(p, "glam_foldername_base");
		glamTxtFilename = Util.getPropertyNotNull(p, "glam_txt_filename");
		glamProcessingFlagFile = Util.getPropertyNotNull(p, "glam_processing_flag_file");
	}
	
	public void run() throws IOException, InterruptedException {
//		BehaviourTree tree = makeTree(dbi, new File(bt_filename), NodeType.CASE_BT_NODES);
		
		BehaviourTree tree = makeTree(dbi, new File(btActFilename), NodeType.ACTION_ONLY_NODES);
		
		BtGlamCodec dencoder = new BtGlamCodec(new File(alphabetFilename), rootEncoding,
				leafEncoding, n -> SimilarityMetrics.exactActionHash(n));
			
		for (int iteration = 1; iteration <= 50; iteration++) {
			LOGGER.info("Starting encoding for iteration " + iteration);
			GlamEncodingRecord e = dencoder.encodeToFile(tree,
					new File(encSeqFilename + iteration + encSeqFilenameExt));
			LOGGER.info("Encoded, awaiting GLAM run");
			
			File glamProcessing = new File(glamProcessingFlagFile);
			glamProcessing.createNewFile();
			do {
				Thread.sleep(5000);
			} while (glamProcessing.exists());
			
			LOGGER.info("GLAM finished, re-encoding tree");
			File glamFile = new File(glamFoldernameBase + iteration + "/" + glamTxtFilename);
			tree = dencoder.modifiedTreeFromGlamFile(e, glamFile);
			if (tree == null) {
				return;
			}
			LOGGER.info("Finished modifying tree, saving");
			BtXmlCodec.save(tree,
					new File(btActFilenameBase + iteration + btActFilenameExt));
		}
	}
	

	/**
	 * Creates a simple (unprocessed) tree from the database / treeFile. This should be a tree with
	 * a single choice (branch) node at the root, and all nodes in sequences without further
	 * branching.
	 */
	private BehaviourTree makeTree(DbInterface dbi, File treeFile, NodeType nodeType) throws IOException {
		BehaviourTree tree = null;
		if (treeFile.isFile()) {
			LOGGER.info("Tree file already exists. Loading / Resuming building.");
			try {
				tree = BtXmlCodec.load(treeFile);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, "Error loading tree", e);
				System.exit(1);
			}
		} else {
			LOGGER.info("Tree file doesn't exist. Starting a new one.");
			tree = new BehaviourTree();
		}
		// HACKED IN FOR CROSS VALIDATION TEST
		{
			List<String> randomReplays = Files.readAllLines(
					new File("ReplaysInRandomOrderPvP.txt").toPath());
			int foldNum = BtTester.FOLD_NUM;
			int numFolds = BtTester.NUM_FOLDS;
			int start = (int) ((foldNum - 1) / (double) numFolds * randomReplays.size());
			int end = (int) (foldNum / (double) numFolds * randomReplays.size());
			for (String replayName : randomReplays.subList(start, end)) {
				tree.setProcessed(replayName);
			}
		}
		// END OF HACKY BIT
		
		int numReplays = Replay.getReplays().size();
		int replayCount = 0;
		int playerReplayCount = 0;
		// Start/resume building BT from DB
		for (Replay replay : Replay.getReplays()) {
			replayCount++;
			if (tree.getProcessed().contains(replay.replayFileName)) {
				LOGGER.fine("Skipping replay (already processed): " + replay.replayFileName);
				continue;
			}
			LOGGER.info("Reading replay " + replay.replayFileName);
			
			ScMap map;
			try {
				map = replay.getMap();
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Failed to get map", e);
				continue;
			}
			// Add an action sequence for each (non-neutral) player
			for (PlayerReplay p : replay.getPlayers()) {
				if (!p.isNeutral()) {
					LOGGER.info("Adding playerReplayId " + p.playerReplayIdDb + " as seq #"
							+ playerReplayCount++);
					if (nodeType == NodeType.CASE_BT_NODES) {
						tree.addSequence(getBtNodes(p, replay, map));
					} else if (nodeType == NodeType.ACTION_ONLY_NODES) {
						tree.addSequence(getActionNodes(p));
					}
				}
			}
			tree.setProcessed(replay.replayFileName);
			if (replayCount % saveInterval == 0 || replayCount == numReplays) {
				try {
					LOGGER.info("Saving tree file");
					BtXmlCodec.save(tree, treeFile);
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, "Error while saving tree file", e);
				}
			}
		}
		LOGGER.info("Tree loaded");
		return tree;
	}
	
	private static List<BtNode> getActionNodes(PlayerReplay p) {
		// convert to BtNodes
		List<BtNode> result = p.getStrategicActionsAndEvents().stream()
				.map(a -> new ActionBtNode(a))
				.collect(Collectors.toList());
		
		return result;
	}

	/** Get all strategic actions as a list of BtNodes for this playerreplay */
	private static List<BtNode> getBtNodes(PlayerReplay p, Replay replay, ScMap map) {
		List<BtNode> result = new ArrayList<>();
		
		List<Unit> unitsAllGame = p.getUnits();
		List<Unit> opponentUnitsAllGame = null;
		for (PlayerReplay p2 : replay.getPlayers()) {
			if (!p2.equals(p) && p2.getRace() != RaceTypes.None) {
				opponentUnitsAllGame = p2.getUnits();
			}
		}
		if (opponentUnitsAllGame == null) {
			LOGGER.severe("Didn't find opponent units for player " + p.playerReplayIdDb);
			return result;
		}
		Set<Unit> seenOpponentUnits = new HashSet<>();
		
		for (Action a : p.getActions()) {
			if (!a.isStrategicUnitCommandType()) {
				continue;
			}
			
			// counts for each unit type and (seen) opponent unit type
			Map<UnitType, Integer> unitTypeCounts = unitsAllGame.stream()
					.filter(u -> u.isExisting(a.frame))
					.collect(Collectors.groupingBy(
							Unit::getType,
							Util.countingInt()));
			
			seenOpponentUnits.addAll(opponentUnitsAllGame.stream()
					.filter(u -> !seenOpponentUnits.contains(u) && p.hasSeen(u, a.frame))
					.collect(Collectors.toList()));
			Map<UnitType, Integer> opponentUnitTypeCounts = opponentUnitsAllGame.stream()
					.filter(u -> seenOpponentUnits.contains(u))
					.filter(u -> u.isExisting(a.frame))
					.collect(Collectors.groupingBy(
							Unit::getType,
							Util.countingInt()));
			
			try {
				Resources r = p.getResources(a.frame);
				result.add(new CaseBtNode(
						a, map, r, unitTypeCounts, opponentUnitTypeCounts));
			} catch (SQLException e) {
				LOGGER.log(Level.SEVERE, "Failed to get resources", e);
				continue;
			}
		}
		return result;
	}
	
	private enum NodeType {
		CASE_BT_NODES, ACTION_ONLY_NODES
	}
	
}
