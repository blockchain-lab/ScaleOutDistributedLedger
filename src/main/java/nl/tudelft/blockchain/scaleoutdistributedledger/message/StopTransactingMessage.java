package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Message to indicate that the receiver should stop transacting.
 */
public class StopTransactingMessage extends Message {

	@Override
	public void handle(LocalStore localStore) {
		localStore.getApplication().stopTransacting();
	}

	@Override
	public String toString() {
		return "StopTransactingMessage";
	}
}
