package bt;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jnibwapi.types.*;
import jnibwapi.types.OrderType.OrderTypes;
import jnibwapi.types.UnitCommandType.UnitCommandTypes;
import jnibwapi.types.UnitType.UnitTypes;
import scdb.OfflineJNIBWAPI;
import util.Util;

public class BehaviourTree {
	private static final Logger LOGGER = Logger.getLogger(BehaviourTree.class.getName());

	
	// for nicer XML output, just list all the BWAPI types at the top
	static {
		// Ensure BWAPI data loaded
		try {
			OfflineJNIBWAPI.loadOfflineJNIBWAPIData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@SuppressWarnings("unused")
	private ArrayList<UnitType> allUnitTypes = new ArrayList<>(UnitTypes.getAllUnitTypes());
	@SuppressWarnings("unused")
	private ArrayList<OrderType> allOrderTypes = new ArrayList<>(OrderTypes.getAllOrderTypes());
	@SuppressWarnings("unused")
	private ArrayList<UnitCommandType> allUnitCommandTypes =
			new ArrayList<>(UnitCommandTypes.getAllUnitCommandTypes());
	
	private Set<String> replaysProcessed = new HashSet<>();
	private BtNode root;
	
	public BehaviourTree() {
		this(new BtSelNode());
	}
	
	public BehaviourTree(BtNode root) {
		this.root = root;
	}
	
	public Set<String> getProcessed() {
		return Collections.unmodifiableSet(replaysProcessed);
	}
	
	public void setProcessed(String replayName) {
		replaysProcessed.add(replayName);
	}
	
	public void addSequence(List<BtNode> seq) {
		BtNode seqNode = new BtSeqNode();
		for (BtNode node : seq) {
			seqNode.addChild(node);
		}
		root.addChild(seqNode);
	}
	

	
	public BtNode getRoot() {
		return root;
	}

	public boolean sanityCheck() {
		if (root.getChildren().isEmpty()) {
			LOGGER.info("Tree is empty");
		}
		return sanityCheck(root);
	}
	
	public boolean sanityCheck(BtNode node) {
		return sanityCheck(node, new HashSet<>());
	}
	
	public boolean sanityCheck(BtNode node, Set<BtNode> seen) {
		if (seen.add(node)) {
			return true;
		}
		if (node == null) {
			LOGGER.warning("Node was null");
			return false;
		}
		if (node.getWeight() <= 0) {
			LOGGER.warning("Node with 0 weight " + node);
		}
		node.checkNotMerged();
		boolean allGood = true;
		try {
			for (BtNode child : node.getChildren()) {
				allGood &= sanityCheck(child, seen);
			}
			if (!allGood) {
				LOGGER.warning("Problem with child of " + node);
			}
		} catch (NullPointerException e) {
			LOGGER.warning("Null pointer getting children of " + node);
			return false;
		}
		return true;
	}
	
	public static List<Map<Class<?>, Integer>> nodeTypeCountsEachLevel(BtNode root, int maxDepth) {
		List<Map<Class<?>, Integer>> typeCounts = new ArrayList<>();
		Set<BtNode> seen = new HashSet<>();
		for (Set<BtNode> nodesOnLevel : nodesEachLevel(root, maxDepth)) {
			typeCounts.add(nodesOnLevel.stream()
					.filter(n -> !seen.contains(n))
					.collect(Collectors.groupingBy(n -> n.getClass(), Util.countingInt())));
			seen.addAll(nodesOnLevel);
		}
		return typeCounts;
	}
	
	/** Return a list of the nodes at each depth in the tree, starting from the root */
	public static List<Set<BtNode>> nodesEachLevel(BtNode root, int maxDepth) {
		List<Set<BtNode>> nodes = new ArrayList<>();
		Set<BtNode> currentLevel = new HashSet<>();
		currentLevel.add(root);
		while (maxDepth > 0 && !currentLevel.isEmpty()) {
			maxDepth--;
			nodes.add(currentLevel);
			Set<BtNode> nextLevel = new HashSet<>();
			for (BtNode n : currentLevel) {
				nextLevel.addAll(n.getChildren());
			}
			currentLevel = nextLevel;
		}
		return nodes;
	}

	public int countNodes() {
		HashSet<BtNode> seen = new HashSet<>();
		findNodesDfs(getRoot(), seen);
		return seen.size();
	}
	
	/** DFS to find all nodes in the subTree rooted at the given node */
	public static Set<BtNode> findNodesDfs(BtNode subTree) {
		Set<BtNode> found = new LinkedHashSet<>();
		findNodesDfs(subTree, found);
		return found;
	}
	
	private static void findNodesDfs(BtNode subTree, Set<BtNode> found) {
		if (!found.contains(subTree)) {
			found.add(subTree);
			for (BtNode child : subTree.getChildren()) {
				findNodesDfs(child, found);
			}
		}
	}
	
	/** DFS to find all sequence nodes in the subTree rooted at the given node */
	public Set<BtSeqNode> findSeqNodesDfs(BtNode subTree) {
		Set<BtSeqNode> found = new LinkedHashSet<>();
		for (BtNode n : findNodesDfs(subTree)) {
			if (n.getClass() == BtSeqNode.class) {
				found.add((BtSeqNode) n);
			}
		}
		return found;
	}
	
}
