package bt.sc;

import java.util.List;

import scdb.Action;

/** A node with one or many actions, all with the same hash */
public interface NodeWithActions {
	public List<Action> getActions();
	
	public Action getRandomAction();
	
	public long getActionHash();
}
