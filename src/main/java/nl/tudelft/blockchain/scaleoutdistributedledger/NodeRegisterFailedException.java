package nl.tudelft.blockchain.scaleoutdistributedledger;

/**
 * Exception for indicating that the user doesn't have enough money.
 */
public class NodeRegisterFailedException extends RuntimeException {

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
