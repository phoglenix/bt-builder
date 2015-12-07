package bt;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;

// TODO maybe this should be abstract
public class BtSelNode extends BtNode {
	private static final Logger LOGGER = Logger.getLogger(BtSelNode.class.getName());
	
	private final Map<BtNode, Integer> childCounts = new HashMap<>(1);
	
	public BtSelNode() {}
	
	public BtSelNode(BtSelNode toCopy) {
		super(toCopy);
		childCounts.putAll(toCopy.getChildCounts());
	}

	@Override
	public BtNode merge(BtNode o) {
		checkNotMerged();
		o.checkNotMerged();
		if (this == o) return this;
		
		if (o instanceof BtSelNode) {
			// Merging two selector nodes. Just add all children.
			for (BtNode child : o.getChildren()) {
				addChild(child);
			}
			o.setMergedInto(this);
		} else {
			// Merging a selector with a non-selector node. Add it as a child
			addChild(o);
		}
		return this;
	}
	
	@Override
	public boolean canMergeIntoOne(BtNode o) {
		return o instanceof BtSelNode;
	}
	
	@Override
	public BtNode addChild(BtNode child) {
		return addChild(child, 1);
	}

	public BtNode addChild(BtNode child, int freq) {
		child = child.getMergedActual();
		child.checkNotMerged();
		if (getWeight() > 8000) {
			LOGGER.warning("Suspiciously high weight");
		}
		if (child == this) {
			LOGGER.warning("Tried to add sel node as child of itself " + this);
			return null;
		}
		updateChildrenNonRecursive();
		if (childCounts.containsKey(child)) {
			childCounts.put(child, childCounts.get(child) + freq);
			return child;
		}
		BtNode foundChild = null;
		for (BtNode myChild : childCounts.keySet()) {
			if (myChild.canMergeIntoOne(child)) {
				foundChild = myChild;
			}
		}
		if (foundChild != null) {
			LOGGER.fine("Adding child " + child + " by merging, potential infinite loop here "
					+ this);
			int count = childCounts.get(foundChild);
			childCounts.remove(foundChild);
			BtNode newChild = foundChild.merge(child);
			childCounts.put(newChild, count + freq);
			return newChild;
		}
		// didn't match with anything, just add it as a child
		childCounts.put(child, freq);
		return child;
	}
	
	@Override
	public List<BtNode> getChildren() {
		return Collections.unmodifiableList(new ArrayList<>(childCounts.keySet()));
	}
	
	public Map<BtNode, Integer> getChildCounts() {
		return Collections.unmodifiableMap(childCounts);
	}
	
	@Override
	public BtNode getRepresentativeChild() {
		// Use most-frequent child
		Optional<Entry<BtNode, Integer>> a = childCounts.entrySet().stream()
				.max(Comparator.comparingInt(e -> e.getValue()));
		if (a.isPresent()) {
			return a.get().getKey();
		}
		return null;
	}

	@Override
	public BtNode duplicateWithoutChildren() {
		BtSelNode copy = new BtSelNode(this);
		copy.childCounts.clear();
		return copy;
	}
	
	@Override
	public void updateChildren(Set<BtNode> seen) {
		if (seen.add(this)) {
			updateChildrenNonRecursive();
			for (BtNode child : childCounts.keySet()) {
				child.updateChildren(seen);
			}
		}
	}
	
	private void updateChildrenNonRecursive() {
		List<BtNode> children = new ArrayList<>(childCounts.keySet());
		for (BtNode child : children) {
			if (child.hasBeenMerged()) {
				int prevCount = childCounts.getOrDefault(child, 0);
				childCounts.remove(child);
				BtNode newChild = child.getMergedActual();
				int newCount = childCounts.getOrDefault(newChild, 0);
				childCounts.put(newChild, prevCount + newCount);
				child = newChild;
			}
		}
	}
	
	@Override
	public void mergeChildren(Set<BtNode> seen) {
		if (seen.add(this)) {
			// Be a bit paranoid - update children before modifying them
			checkNotMerged();
			updateChildrenNonRecursive();
			// Use existing merge-on-addChild to merge children
			Map<BtNode, Integer> childrenCopy = new HashMap<>(childCounts);
			childCounts.clear();
			for (Entry<BtNode, Integer> e : childrenCopy.entrySet()) {
				addChild(e.getKey(), e.getValue());
			}
			for (BtNode child : childCounts.keySet()) {
				child.mergeChildren(seen);
			}
		}
	}
	
	private transient Random random = null;
	
	/**
	 * Get a child chosen uniformly randomly from the children of this node. Returns null if there
	 * are no children.
	 */
	// TODO should be taking into account the counts of the children
	public BtNode getRandomChild() {
		if (childCounts.size() == 0) {
			LOGGER.warning("Returning null child from RandomSelNode with no children");
			return null;
		}
		if (random == null) {
			random = new Random();
		}
		int idx = random.nextInt(childCounts.size());
		return new ArrayList<>(childCounts.keySet()).get(idx);
	}
	
	public String toString() {
		return "Selector" + super.toString();
	}
	
	public String toShortString() {
		return "Selector" + (getWeight() > 1 ? ": " + getWeight() : "")
				+ (hasBeenMerged() ? " (M)" : "");
	}
}
