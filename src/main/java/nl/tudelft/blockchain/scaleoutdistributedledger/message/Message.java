package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.Serializable;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Message abstract super class.
 */
public abstract class Message implements Serializable {
	private static final long serialVersionUID = 1L;

	public abstract void handle(LocalStore localStore);
}
