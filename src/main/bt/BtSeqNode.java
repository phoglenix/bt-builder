package bt;

import java.util.*;
import java.util.logging.Logger;


public class BtSeqNode extends BtNode {
	private static final Logger LOGGER = Logger.getLogger(BtSeqNode.class.getName());
	
	private final List<BtNode> children = new ArrayList<>(1);
	
	public BtSeqNode() {}
	
	public BtSeqNode(BtSeqNode toCopy) {
		super(toCopy);
		children.addAll(toCopy.getChildren());
	}
	
	@Override
	public List<BtNode> getChildren() {
		return Collections.unmodifiableList(children);
	}
	
	@Override
	public BtNode getRepresentativeChild() {
		// Represented by first child
		if (children.size() > 0) {
			return children.get(0);
		} else {
			return null;
		}
	}

	/** Adds the child to the end of this sequence. */
	@Override
	public BtNode addChild(BtNode child) {
		child = child.getMergedActual();
		children.add(child);
		return child;
	}

	@Override
	public BtNode merge(BtNode o) {
		checkNotMerged();
		o.checkNotMerged();
		if (this == o) return this;
		
		if (canMergeIntoOne(o)) {
			updateChildrenNonRecursive();
			o.setMergedInto(this);
			List<BtNode> mergedChildren = new ArrayList<>();
			for (int i = 0; i < getChildren().size(); i++) {
				mergedChildren.add(getChildren().get(i).merge(o.getChildren().get(i).getMergedActual()));
			}
			children.clear();
			for (BtNode child : mergedChildren) {
				addChild(child);
			}
			return this;
		}
		LOGGER.severe("Tried to merge seq node " + this + " with node " + o + ". Don't know how");
		return this;
	}
	
	@Override
	public boolean canMergeIntoOne(BtNode o) {
		if (this == o) {
			return true;
		} else if (o instanceof BtSeqNode &&
				getChildren().size() == o.getChildren().size()) {
			for (int i = 0; i < getChildren().size(); i++) {
				if (!getChildren().get(i).getMergedActual()
						.canMergeIntoOne(o.getChildren().get(i).getMergedActual())) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public BtNode duplicateWithoutChildren() {
		BtSeqNode copy = new BtSeqNode(this);
		copy.children.clear();
		return copy;
	}
	
	@Override
	public void updateChildren(Set<BtNode> seen) {
		if (seen.add(this)) {
			updateChildrenNonRecursive();
			for (BtNode child : children) {
				child.updateChildren(seen);
			}
		}
	}
	
	private void updateChildrenNonRecursive() {
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i).hasBeenMerged()) {
				children.set(i, children.get(i).getMergedActual());
			}
		}
	}
	
	@Override
	public void mergeChildren(Set<BtNode> seen) {
		if (seen.add(this)) {
			checkNotMerged();
			// Not going to merge any children of a seq node
			for (BtNode child : children) {
				child.getMergedActual().mergeChildren(seen);
			}
			
		}
	}
	
	public String toString() {
		return "Sequence" + super.toString() + "hash: " + Objects.hash(this);
	}
	
	public String toShortString() {
		return "Sequence" + (getWeight() > 1 ? ": " + getWeight() : "")
				+ (hasBeenMerged() ? " (M)" : "");
	}
	
}
