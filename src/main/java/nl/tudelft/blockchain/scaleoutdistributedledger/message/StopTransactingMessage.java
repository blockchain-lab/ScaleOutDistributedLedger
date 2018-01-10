package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Message to indicate that the receiver should stop transacting.
 */
public class StopTransactingMessage extends Message {
	private static final long serialVersionUID = 1L;

	@Override
	public void handle(LocalStore localStore) {
		localStore.getApplication().stopTransacting();
	}

	@Override
	public String toString() {
		return "StopTransactingMessage";
	}
}
