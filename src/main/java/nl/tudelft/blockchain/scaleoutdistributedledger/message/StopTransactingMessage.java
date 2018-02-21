package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Message to indicate that the receiver should stop transacting.
 */
public class StopTransactingMessage extends Message {
	public static final int MESSAGE_ID = 4;

	@Override
	public void handle(LocalStore localStore) {
		localStore.getApplication().stopTransacting();
	}
	
	@Override
	public int getMessageId() {
		return MESSAGE_ID;
	}

	@Override
	public String toString() {
		return "StopTransactingMessage";
	}
}
