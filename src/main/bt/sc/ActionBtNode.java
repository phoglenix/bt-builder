package bt.sc;

import java.util.*;
import java.util.logging.Logger;

import scdb.Action;
import bt.BtNode;
import bt.BtSelNode;

/**
 * Represents one action to be executed but may encapsulate many similar actions from which it was
 * formed.
 */
public class ActionBtNode extends BtNode implements NodeWithActions {
	private static final Logger LOGGER = Logger.getLogger(ActionBtNode.class.getName());
	
	private final List<Action> actions;
	private final long hash;
	
	public ActionBtNode(Action a) {
		actions = new ArrayList<>(1);
		actions.add(a);
		hash = SimilarityMetrics.exactActionHash(a);
	}
	
	/** Construct from a list of nodes. Must not be empty. */
	public ActionBtNode(List<Action> actions) {
		// Ensure all actions have the same action hash
		this.actions = new ArrayList<>(actions.size());
		hash = SimilarityMetrics.exactActionHash(actions.get(0));
		for (Action a : actions) {
			add(a);
		}
	}
	
	/** Copy constructor. */
	public ActionBtNode(ActionBtNode o) {
		super(o);
		// Ensure all actions have the same action hash
		actions = new ArrayList<>(o.actions.size());
		hash = o.hash;
		for (Action a : o.actions) {
			add(a);
		}
	}

	@Override
	public List<Action> getActions() {
		// All actions have the same hash, might have different specifics
		return Collections.unmodifiableList(actions);
	}
	
	@Override
	public Action getRandomAction() {
		Random r = new Random();
		int idx = r.nextInt(actions.size());
		return actions.get(idx);
	}
	
	@Override
	public long getActionHash() {
		return hash;
	}
	
	public void add(Action a) {
		if (SimilarityMetrics.exactActionHash(a) != hash) {
			LOGGER.warning("Tried to add action " + a + " to ActionsBtNode with actions "
					+ actions.get(0));
			return;
		}
		actions.add(a);
	}

	@Override
	public String toString() {
		return "ActionBtNode{" + actions.get(0) + ", " + actions.size() + "}";
	}
	
	@Override
	public String toShortString() {
		return actions.get(0).getTargetIdAsString() + " x" + actions.size()
				+ (hasBeenMerged() ? "(M)" : "");
	}

	@Override
	public BtNode merge(BtNode o) {
		checkNotMerged();
		o.checkNotMerged();
		if (this == o) return this;
		
		if (canMergeIntoOne(o)) {
			o.setMergedInto(this);
			List<Action> oActions = ((NodeWithActions) o).getActions();
			for (Action a : oActions) {
				add(a);
			}
			setWeight(getWeight() + o.getWeight());
			return this;
		}
		// Merging an action node with a non-action node - need to make a selector
		BtNode merged = new BtSelNode().merge(this).merge(o);
		LOGGER.info("Merged action node " + this + " with " + o + ", making a selector " + merged);
		return merged;
	}
	
	@Override
	public boolean canMergeIntoOne(BtNode o) {
		if (o instanceof NodeWithActions)
			if (hash == ((NodeWithActions) o).getActionHash())
				return true;
		return false;
	}
	
	@Override
	public BtNode addChild(BtNode child) {
		throw new RuntimeException("Cannot add a child " + child + " to an ActionsBtNode " + this);
	}

	@Override
	public List<BtNode> getChildren() {
		return Collections.emptyList();
	}
	
	@Override
	public BtNode getRepresentativeChild() {
		// No children
		return null;
	}

	@Override
	public BtNode duplicateWithoutChildren() {
		return new ActionBtNode(this);
	}
	
	@Override
	public void updateChildren(Set<BtNode> seen) {
		// No children, nothing to do
		return;
	}
	
	@Override
	public void mergeChildren(Set<BtNode> seen) {
		// No children, nothing to do
		return;
	}
	
}
