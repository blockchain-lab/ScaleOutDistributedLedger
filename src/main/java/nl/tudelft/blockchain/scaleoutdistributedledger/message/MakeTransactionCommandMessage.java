package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Message for the "make transaction" command from the simulation server.
 */
public class MakeTransactionCommandMessage extends Message {
	private int receiverId;
	private long amount;
	
	/**
	 * @param receiverId - the id of the receiver of the transaction
	 * @param amount     - the amount to send
	 */
	public MakeTransactionCommandMessage(int receiverId, long amount) {
		this.receiverId = receiverId;
		this.amount = amount;
	}
	
	@Override
	public void handle(LocalStore localStore) {
		//TODO Do we want centralized simulation?
	}

}
