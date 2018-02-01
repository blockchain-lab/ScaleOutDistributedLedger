package nl.tudelft.blockchain.scaleoutdistributedledger.exceptions;

/**
 * Exception for the tracker.
 */
public class TrackerException extends RuntimeException {
	private static final long serialVersionUID = -3346246279993022763L;

	/**
	 * Exception without message.
	 */
	public TrackerException() {
		super();
	}

	/**
	 * @param msg - the message
	 */
	public TrackerException(String msg) {
		super(msg);
	}
}
