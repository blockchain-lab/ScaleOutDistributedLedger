package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.Serializable;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Message abstract super class.
 */
public abstract class Message implements Serializable {

	/**
	 * Handle the message received.
	 * @param localStore - local store of the current node
	 */
    public abstract void handle(LocalStore localStore);
	
}
