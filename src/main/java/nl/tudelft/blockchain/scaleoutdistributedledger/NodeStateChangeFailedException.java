package nl.tudelft.blockchain.scaleoutdistributedledger;

/**
 * Exception for indicating that updating the running state of a node on the tracker failed.
 */
public class NodeStateChangeFailedException extends RuntimeException {
	/**
	 * Constructor
	 * @param id - the ID of the node for which it failed
	 * @param running - the running state that it could not be updated to
	 */
	public NodeStateChangeFailedException(int id, boolean running) {
		super("Failed to set the running state of node " + id + "to" + running);
	}
}
