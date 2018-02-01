package nl.tudelft.blockchain.scaleoutdistributedledger.exceptions;

/**
 * Exception for indicating that registering with the tracker failed.
 */
public class NodeRegisterFailedException extends TrackerException {
	private static final long serialVersionUID = 8271135988867023425L;

	/**
	 * Exception without message.
	 */
	public NodeRegisterFailedException() {
		super();
	}

	/**
	 * @param msg - the message
	 */
	public NodeRegisterFailedException(String msg) {
		super(msg);
	}
}
