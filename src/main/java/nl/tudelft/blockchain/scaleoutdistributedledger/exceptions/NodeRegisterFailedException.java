package nl.tudelft.blockchain.scaleoutdistributedledger;

/**
 * Exception for indicating that the user doesn't have enough money.
 */
public class NodeRegisterFailedException extends RuntimeException {
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
