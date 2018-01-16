package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Message to indicate that the receiver can start transacting.
 */
public class StartTransactingMessage extends Message {
	private static final long serialVersionUID = 1L;

	@Override
	public void handle(LocalStore localStore) {
		try {
			localStore.updateNodes();
			localStore.getApplication().startTransacting();
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Unable to start node " + localStore.getOwnNode().getId(), ex);
		}
	}

	@Override
	public String toString() {
		return "StartTransactingMessage";
	}
}
