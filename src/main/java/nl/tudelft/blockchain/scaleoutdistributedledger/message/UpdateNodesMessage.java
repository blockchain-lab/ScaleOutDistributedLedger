package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Message to indicate that the receiver should update their node list.
 */
public class UpdateNodesMessage extends Message {
	public static final int MESSAGE_ID = 5;

	@Override
	public void handle(LocalStore localStore) {
		try {
			localStore.updateNodes();
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Unable to update node list of " + localStore.getOwnNode().getId(), ex);
		}
	}
	
	@Override
	public int getMessageId() {
		return MESSAGE_ID;
	}

	@Override
	public String toString() {
		return "UpdateNodesMessage";
	}
}
