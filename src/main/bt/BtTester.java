package bt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import scdb.*;
import util.LogManager;
import util.Util;
import util.Util.Pair;
import bt.sc.ActionBtNode;
import bt.sc.NodeWithActions;

/**
 * Code for evaluating the prediction accuracy of a BT using cross validation.
 */
public class BtTester {
	private static final Logger LOGGER = Logger.getLogger(BtTester.class.getName());
	
	private static final int FRAME_CUTOFF = 20 * 60 * 24; // 20 min, in frames
	public static final int FOLD_NUM = 5;
	public static final int NUM_FOLDS = 10;
	
	private final List<String> randomReplays;
	private final String treeFileName = "behaviour_tree_actions_final.xml.gz";
	private final String resultsByActNumFileName = "BtTesterResultsByActNum.csv";
	private final String resultsByTimeFileName = "BtTesterResultsByTime.csv";

	public static void main(String[] args) {
		LogManager.initialise("BtTester");
		
		LOGGER.info("Starting BtTester");
		try {
			BtTester btt = new BtTester();
			btt.run();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}
		
		LOGGER.info("Finished");
	}
	
	public BtTester() throws IOException {
		List<String> allRandomReplays = Files.readAllLines(
				new File("ReplaysInRandomOrderPvP.txt").toPath());
		int start = (int) ((FOLD_NUM - 1) / (double) NUM_FOLDS * allRandomReplays.size());
		int end = (int) (FOLD_NUM / (double) NUM_FOLDS * allRandomReplays.size());
		randomReplays = allRandomReplays.subList(start, end);
	}
	
	public void run() throws IOException {
		File treeFile = new File(treeFileName);
		BehaviourTree tree = BtXmlCodec.load(treeFile);
		List<TestResult> allResults = new ArrayList<>();
		
		LOGGER.info("Tree loaded. Starting replays");
		for (Replay replay : getReplaysToTest()) {
			LOGGER.info("Processing replay " + replay.replayFileName);
			for (PlayerReplay playerRep : replay.getPlayers()) {
				if (!playerRep.isNeutral()) {
					TestResult tr = testOnReplay(tree, playerRep);
					allResults.add(tr);
				}
			}
			// Write out results after each replay just in case
			writeOutResultsByActNum(allResults, new File(resultsByActNumFileName));
			writeOutResultsByTime(allResults, new File(resultsByTimeFileName), 24 * 10); // 10s intervals
		}
	}
	
	private void writeOutResultsByActNum(List<TestResult> allResults, File file) throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
			out.write("similarities by action#");
			out.newLine();
			for (TestResult tr : allResults) {
				out.write(Util.join(",", tr.scores));
				out.newLine();
			}
		}
	}
	
	private void writeOutResultsByTime(List<TestResult> allResults, File file, int frameInterval)
			throws IOException {
		try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
			out.write("similarities by time");
			out.newLine();
			for (int i = 0; i < FRAME_CUTOFF; i += frameInterval) {
				out.write(i + ",");
			}
			out.newLine();
			
			int[] scoreCountsTotal = new int[FRAME_CUTOFF/frameInterval + 1];
			double[] scoreSumsTotal = new double[FRAME_CUTOFF/frameInterval + 1];
			for (TestResult tr : allResults) {
				int idx = 0;
				for (int curMax = frameInterval; curMax <= FRAME_CUTOFF; curMax += frameInterval) {
					int scoreCount = 0;
					double scoreSum = 0;
					while (idx < tr.frames.size() && tr.frames.get(idx) < curMax) {
						scoreCount++;
						scoreSum += tr.scores.get(idx);
						idx++;
					}
					if (scoreCount > 0) {
						out.write((scoreSum/scoreCount) + ",");
					} else {
						out.write(",");
					}
					scoreCountsTotal[curMax/frameInterval -1] += scoreCount;
					scoreSumsTotal[curMax/frameInterval -1] += scoreSum;
				}
				out.newLine();
			}
			out.newLine();
			out.write("counts");
			out.newLine();
			out.write(Util.join(",", Arrays.stream(scoreCountsTotal).boxed()));
			out.newLine();
			out.write("averages");
			out.newLine();
			for (int i = 0; i < scoreCountsTotal.length; i++) {
				if (scoreCountsTotal[i] > 0)
					out.write((scoreSumsTotal[i] / scoreCountsTotal[i]) + ",");
			}
			out.newLine();
		}
	}

	private List<Replay> getReplaysToTest() {
		Set<String> replayNames = new HashSet<>();
		replayNames.addAll(randomReplays);
		return Replay.getReplays().stream()
				.filter(r -> replayNames.contains(r.replayFileName))
				.collect(Collectors.toList());
	}
	
	private TestResult testOnReplay(BehaviourTree tree, PlayerReplay playerRep) {
		TestResult result = new TestResult();
		BtHandler handler = new BtHandler(tree);
		int count = 0;
		int totalActions = playerRep.getStrategicActionsAndEvents().size();
		
		for (Action actExpected : playerRep.getStrategicActionsAndEvents()) {
			if (++count%10 == 0) {
				LOGGER.info("Processed " + count + " actions of " + totalActions);
			}
			if (actExpected.frame > FRAME_CUTOFF) {
				LOGGER.info("Stopping at frame cutoff");
				break;
			}
			// Compare chosen action with expected action
			try {
				Action chosen = handler.nextAction(actExpected);
				result.recordComparison(actExpected.frame, sim(actExpected, chosen));
			} catch (SQLException e) {
				LOGGER.severe("Exception getting case from action: " + e.getMessage());
				e.printStackTrace();
			}
		}
		return result;
	}
	
	private static double sim(Action a1, Action a2) {
		int totalSim = 0;
		// Getting the order/command right is most important, just use that for now
		if (Objects.equals(a1.unitCommandType, a2.unitCommandType)) totalSim++;
		if (Objects.equals(a1.orderType, a2.orderType)) totalSim++;
		if (Objects.equals(a1.getTargetIdAsString(), a2.getTargetIdAsString())) totalSim++;
		// could also compare unit group, target pos / unit, delayed
		return totalSim == 3 ? 1 : 0;
	}
	
	private static double sim(State state1, State state2) {
		double mapSim = 0;
		if (Objects.equals(state1.map.mapName,	state2.map.mapName)) mapSim++;
		mapSim += sim(state1.map.numStartPos,	state2.map.numStartPos, 8);
		mapSim += sim(state1.map.xSize,			state2.map.xSize, 127);
		mapSim += sim(state1.map.ySize,			state2.map.ySize, 127);
		int numMapItems = 5;
		double resourceSim = 0;
		// obsrange is arbitrarily chosen for these, not true observed range
		resourceSim += sim(state1.resources.frame, state2.resources.frame, FRAME_CUTOFF);
		resourceSim += sim(state1.resources.minerals, state2.resources.minerals, 1000);
		resourceSim += sim(state1.resources.gas, state2.resources.gas, 1000);
		resourceSim += sim(state1.resources.supply, state2.resources.supply, 200);
		resourceSim += sim(state1.resources.totalMinerals, state2.resources.totalMinerals, 10*1000);
		resourceSim += sim(state1.resources.totalGas, state2.resources.totalGas, 10*1000);
		resourceSim += sim(state1.resources.totalSupply, state2.resources.totalSupply, 200);
		int numResourceItems = 7;
		double unitTypesSim = 0;
		int numUnitTypes = 0;
		double oppUnitTypesSim = 0;
		int numOppUnitTypes = 0;
		for (UnitType ut : UnitTypes.getAllUnitTypes()) {
			if (state1.unitTypeCounts.containsKey(ut) || state2.unitTypeCounts.containsKey(ut)) {
				numUnitTypes++;
				unitTypesSim += sim(state1.unitTypeCounts.getOrDefault(ut, 0),
						state2.unitTypeCounts.getOrDefault(ut, 0), 20);
			}
			if (state1.opponentUnitTypeCounts.containsKey(ut)
					|| state2.opponentUnitTypeCounts.containsKey(ut)) {
				numOppUnitTypes++;
				oppUnitTypesSim += sim(state1.opponentUnitTypeCounts.getOrDefault(ut, 0),
						state2.opponentUnitTypeCounts.getOrDefault(ut, 0), 20);
			}
		}
		// if both 0 opponent units seen, that is a match, avoid dividing by 0
		if (numOppUnitTypes == 0) {
			oppUnitTypesSim = 1;
			numOppUnitTypes = 1;
		}
		// unit type matchings weighted more because there are many
		double unitTypeWeight = 1;
		double totalSim = mapSim / numMapItems
				+ resourceSim / numResourceItems
				+ unitTypeWeight * unitTypesSim / numUnitTypes
				+ unitTypeWeight * oppUnitTypesSim / numOppUnitTypes;
		int totalWeight = 12;
		return totalSim / totalWeight;
	}
	
	
	/** Use inverse percent difference to calculate similarity between numbers */
	public static double sim(int num1, int num2, double obsRange) {
		if (Math.max(num1, num2) == 0) {
			if (num1 == num2) {
				return 1;
			} else {
				return 0;
			}
		}
		double result = 1 - Math.abs(num1 - num2) / obsRange;
		if (result < 0 || result > 1) {
			LOGGER.fine("PercentDifference: result was out of bounds " + num1 + " " + num2
					+ " " + obsRange);
		}
		return Math.max(0f, Math.min(1f, result)); // force into [0, 1]
	}
	
	private static State stateFromAction(Action act) throws SQLException {
		PlayerReplay p = PlayerReplay.fromId(act.playerReplayIdDb);
		Replay replay = p.getReplay();
		ScMap map = replay.getMap();
		
		List<Unit> opponentUnitsAllGame = null;
		for (PlayerReplay p2 : replay.getPlayers()) {
			if (!p2.equals(p) && !p2.isNeutral()) {
				opponentUnitsAllGame = p2.getUnits();
			}
		}
		if (opponentUnitsAllGame == null) {
			LOGGER.severe("Didn't find opponent units for player " + p.playerReplayIdDb);
			return null;
		}
		
		Map<UnitType, Integer> unitTypeCounts = p.getUnitsExisting(act.frame).stream()
				.collect(Collectors.groupingBy(
						Unit::getType,
						Util.countingInt()));
		
		Set<Unit> seenOpponentUnits = opponentUnitsAllGame.stream()
				.filter(u -> p.hasSeen(u, act.frame))
				.collect(Collectors.toSet());
		Map<UnitType, Integer> opponentUnitTypeCounts = opponentUnitsAllGame.stream()
				.filter(u -> seenOpponentUnits.contains(u))
				.filter(u -> u.isExisting(act.frame))
				.collect(Collectors.groupingBy(
						Unit::getType,
						Util.countingInt()));
		
		Resources r = p.getResources(act.frame);
		return new State(map, r, unitTypeCounts, opponentUnitTypeCounts);
	}

	private static class BtHandler {
		private final BehaviourTree bt;
		private final List<BtNode> stack = new ArrayList<>();
		BtHandler(BehaviourTree tree) {
			bt = tree;
			stack.add(bt.getRoot());
		}
		
		public Action nextAction(Action stateAct) throws SQLException {
			BtNode current;
			while (true) {
				if (stack.isEmpty()) {
					LOGGER.warning("Stack became empty, restarting!");
					stack.add(bt.getRoot());
				}
				current = stack.remove(stack.size() - 1);
				if (current instanceof NodeWithActions) {
					break;
				}
				if (current instanceof BtSeqNode) {
					// Seq: add all children to stack in reverse order
					// (so they are popped first-to-last)
					List<BtNode> children = new ArrayList<>(current.getChildren());
					Collections.reverse(children);
					stack.addAll(children);
				} else if (current instanceof BtSelNode) {
					// Select based on child weighting and case similarity
					BtNode next = executeSelect((BtSelNode)current, stateFromAction(stateAct));
					if (next != null) {
						stack.add(next);
					}
				} else {
					throw new RuntimeException("Unknown node type: " + current);
				}
			}
			return ((NodeWithActions) current).getRandomAction();
		}

		private BtNode executeSelect(BtSelNode sel, State state) {
			BtNode bestChild = null;
			double bestScore = -1;
			// avoid finding state for tons of child nodes: find top freq ones
			List<Pair<BtNode, Integer>> childCounts = sel.getChildCounts().entrySet().stream()
					.map(e -> new Pair<>(e.getKey(), e.getValue()))
					.collect(Collectors.toList());
			Collections.shuffle(childCounts); // in case all freq 1
			Collections.sort(childCounts, (x,y) -> (y.second - x.second));
			childCounts = childCounts.subList(0, Math.min(childCounts.size(), 10));
			for (Pair<BtNode, Integer> childEntry : childCounts) {
				BtNode repChild = childEntry.first;
				// Quick 'n' dirty way of associating a state with a child
				while (repChild != null && !(repChild instanceof NodeWithActions)) {
					repChild = repChild.getRepresentativeChild();
				}
				if (repChild == null) {
					LOGGER.warning("No representative child found for " + childEntry.first);
					continue;
				}
				State childState = null;
				try {
					childState = stateFromAction(((ActionBtNode) repChild).getRandomAction());
				} catch (SQLException e) {
					LOGGER.severe("Exception getting case from action: " + e.getMessage());
					e.printStackTrace();
				}
				if (childState == null) continue;
				// score is frequency * similarity
				double score = childEntry.second * sim(state, childState);
				if (score > bestScore) {
					bestChild = childEntry.first; // use original child not repChild
					bestScore = score;
				}
			}
			return bestChild;
		}

	}
	
	private static class State {
		public final ScMap map;
		public final Resources resources;
		public final Map<UnitType, Integer> unitTypeCounts;
		public final Map<UnitType, Integer> opponentUnitTypeCounts;
		
		public State(ScMap map, Resources resources, Map<UnitType, Integer> unitTypeCounts,
				Map<UnitType, Integer> opponentUnitTypeCounts) {
			this.map = map;
			this.resources = resources;
			this.unitTypeCounts = Collections.unmodifiableMap(unitTypeCounts);
			this.opponentUnitTypeCounts = Collections.unmodifiableMap(opponentUnitTypeCounts);
		}
	}
	
	private static class TestResult {
		private List<Integer> frames = new ArrayList<>();
		private List<Double> scores = new ArrayList<>();
				
		public void recordComparison(int frame, double score) {
			frames.add(frame);
			scores.add(score);
		}
	}
}
