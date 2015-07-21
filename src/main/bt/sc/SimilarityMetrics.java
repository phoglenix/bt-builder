package bt.sc;

import java.util.Objects;
import java.util.logging.Logger;

import scdb.Action;
import jnibwapi.types.OrderType.OrderTypes;
import jnibwapi.types.UnitCommandType.UnitCommandTypes;
import bt.BtNode;
import bt.BtSelNode;
import bt.BtSeqNode;

public class SimilarityMetrics {
	private static final Logger LOGGER = Logger.getLogger(SimilarityMetrics.class.getName());
	
	public static interface NodeHash {
		long hash(BtNode n);
	}
	
	public static double exactActionSimilarity(BtNode n1, BtNode n2) {
		if (n1.getClass() == BtNode.class && n2.getClass() == BtNode.class) {
			return 1.0;
		}
		if (n1 instanceof CaseBtNode && n2 instanceof CaseBtNode) {
			CaseBtNode c1 = (CaseBtNode)n1;
			CaseBtNode c2 = (CaseBtNode)n2;
			if (c1.action.orderType == c2.action.orderType
					&& c1.action.unitCommandType == c2.action.unitCommandType
					&& c1.action.targetId == c2.action.targetId) {
				return 1.0;
			}
		}
		if (!(n1.getClass() == BtNode.class || n1 instanceof CaseBtNode )) {
			LOGGER.warning("Not sure how to compute similarity of node " + n1);
		}
		if (!(n2.getClass() == BtNode.class || n2 instanceof CaseBtNode )) {
			LOGGER.warning("Not sure how to compute similarity of node " + n2);
		}
		return 0.0;
	}
	
	public static long exactActionHash(BtNode n) {
		// For non-action nodes, just return a consistent value
		if (n.getClass() == BtSelNode.class || n.getClass() == BtSeqNode.class) {
			// Ensure a negative value so it doesn't conflict with exactActionHash
			return -Math.abs(Objects.hashCode(n));
		} else if (!(n instanceof NodeWithActions)) {
			throw new RuntimeException("Not sure how to deal with node: " + n);
		}
		return ((NodeWithActions)n).getActionHash();
	}
	
	public static long exactActionHash(Action a) {
		long hash = 0;
		hash += a.targetId; // usually unittype/techtype etc but could be unitreplayid
		hash *= OrderTypes.getAllOrderTypes().size();
		hash += a.orderType.getID();
		hash *= UnitCommandTypes.getAllUnitCommandTypes().size();
		hash += a.unitCommandType.getID();
		return hash;
	}
	
}
