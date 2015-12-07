package bt.sc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jnibwapi.types.UnitType;
import scdb.Action;
import scdb.BuildTile;
import scdb.Resources;
import scdb.ScMap;
import bt.BtNode;

/** A BtNode that represents a Starcraft state and action,  */
public class CaseBtNode extends BtNode implements NodeWithActions {
	@Override
	public BtNode addChild(BtNode child) {
		throw new RuntimeException("Cannot add a child " + child + " to a CaseBtNode " + this);
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
	public List<Action> getActions() {
		return Arrays.asList(new Action[] {action});
	}
	
	@Override
	public Action getRandomAction() {
		return action;
	}
	
	@Override
	public long getActionHash() {
		return SimilarityMetrics.exactActionHash(action);
	}
	
	// Action
	public final Action action;
	/** The action's target's build tile information, if it exists. Otherwise null. */
	public final BuildTile buildTile;
	// TODO region information
	
	public final Set<UnitType> actionUnitTypes;
	// State
	public final ScMap map;
	public final Resources resources;
	public final Map<UnitType, Integer> unitTypeCounts;
	public final Map<UnitType, Integer> opponentUnitTypeCounts;
	
	public CaseBtNode(Action action, ScMap map, Resources resources,
			Map<UnitType, Integer> unitTypeCounts, Map<UnitType, Integer> opponentUnitTypeCounts) {
		this.action = action;
		buildTile = action.getBuildTile(map);
		actionUnitTypes = action.getUnitGroup().stream()
				.map(u -> u.getType()).collect(Collectors.toSet());
		this.map = map;
		this.resources = resources;
		this.unitTypeCounts = Collections.unmodifiableMap(unitTypeCounts);
		this.opponentUnitTypeCounts = Collections.unmodifiableMap(opponentUnitTypeCounts);
	}
	
	@Override
	public String toString() {
		return "CaseBtNode{" + action + ", " + buildTile + ", " + map + "}";
	}
	
	@Override
	public String toShortString() {
		return "Case " + action.getTargetIdAsString() + (hasBeenMerged() ? "(M)" : "");
	}

	@Override
	public BtNode duplicateWithoutChildren() {
		throw new RuntimeException("Not implemented");
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
