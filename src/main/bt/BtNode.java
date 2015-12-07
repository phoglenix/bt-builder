package bt;

import java.util.List;
import java.util.Set;

public abstract class BtNode {
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
			.getLogger(BtNode.class.getName());
	
	/** @deprecated pretty sure weight isn't used any more */
	private int weight;
	
	/** If this has been merged into another node, references that node */
	private BtNode mergedInto = null;
	
	public BtNode() {
		weight = 1;
	}
	
	public BtNode(BtNode toCopy) {
		if (toCopy.weight <= 0) {
			throw new RuntimeException("Trying to copy-construct from a node with negative weight");
		}
		weight = toCopy.weight;
	}
	
	/** Add a child to this BtNode. */
	public abstract BtNode addChild(BtNode child);
	
	public abstract List<BtNode> getChildren();
	
	/** Get a "representative" child, for checking the gamestate associated with this node */
	public abstract BtNode getRepresentativeChild();
	
	/**
	 * Get the weight of this node. Each node has a weight to indicate how many maximally-specific
	 * tree nodes are directly represented by this node
	 * */
	public int getWeight() {
		return weight;
	}
	
	public void setWeight(int newWeight) {
		checkNotMerged();
		if (newWeight <= 0) {
			throw new RuntimeException("Tried to set negative weight on " + this);
		}
		weight = newWeight;
	}
	
	/**
	 * Merge attributes of other BtNode and this BtNode, creating a <b>new</b> node if any changes
	 * were made
	 */
	public BtNode merge(BtNode o) {
		checkNotMerged();
		if (o.weight <= 0 || weight <= 0) {
			LOGGER.severe("Negative/zero weight, something very wrong " + o.weight + " " + weight);
		}
		if (this == o) {
			// Merging with self, nothing to do
			return this;
		} else {
			throw new RuntimeException("Not implemented");
		}
	}
	
	/** Whether the other node can be merged into this one */
	public boolean canMergeIntoOne(BtNode o) {
		checkNotMerged();
		return this == o;
	}
	
	/** Check this node hasn't been merged - warn if it has */
	public void checkNotMerged() {
		if (mergedInto != null) {
			LOGGER.warning(this + " has been merged already, shouldn't be in use.");
		}
	}
	
	/** Mark this node as having been merged. */
	public void setMergedInto(BtNode merged) {
		if (mergedInto != null) {
			LOGGER.warning("Setting merged into " + merged + " when already set to " + mergedInto);
		}
		if (mergedInto == this) {
			LOGGER.warning("Tried to set " + this + " merged into self");
			return;
		}
		mergedInto = merged;
	}
	
	public BtNode getMergedActual() {
		if (mergedInto != null)
			return mergedInto.getMergedActual();
		else
			return this;
	}
	
	public boolean hasBeenMerged() {
		return mergedInto != null;
	}
	
	public abstract BtNode duplicateWithoutChildren();
	
	/**
	 * Update the children of this node (recursively), making any merged nodes point to the
	 * actual node they have been merged into.<br>
	 * <code>seen</code> is used to prevent infinite recursion, and should just be a
	 * <code>new HashSet<>()</code> at the top level.
	 */
	public abstract void updateChildren(Set<BtNode> seen);
	
	/**
	 * Merge the children of this node that are able to be merged (recursively).<br>
	 * <code>seen</code> is used to prevent infinite recursion, and should just be a
	 * <code>new HashSet<>()</code> at the top level.
	 */
	public abstract void mergeChildren(Set<BtNode> seen);
	
	public String toString() {
		return "BtNode{children:" + getChildren().size() + " weight: " + weight + " " +
				(hasBeenMerged() ? "(M)" : "") + "}";
	}
	
	/** Provide a short summary of the object. */
	public String toShortString() {
		return "BtNode: " + weight + (hasBeenMerged() ? "(M)" : "");
	}
	
	public String toStringRecursive(Set<BtNode> seen) {
		String val = "{" + toShortString();
		if (hasBeenMerged())
			val += "(m)";
		if (seen.add(this)) {
			// Not seen before
			for (BtNode child : getChildren()) {
				val += child.toStringRecursive(seen);
			}
			return val + "}";
		} else {
			return val + " (recursing)}";
		}
		
	}
}
