package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Helper class for communication.
 */
public final class CommunicationHelper {
	private CommunicationHelper() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Sends the given transaction to the receiver of the transaction.
	 * @param transaction - the transaction to send
	 */
	public static void sendTransaction(Transaction transaction) {
		Node to = transaction.getReceiver();
		String address = to.getAddress();
		//TODO Open socket connection to other
		//TODO Create message and send it
		throw new UnsupportedOperationException("TODO: Sending transactions is not yet implemented!");
	}
	
	/**
	 * @param verification - the verification object to verify transactions with
	 * @param transaction  - the transaction that was received
	 * @param proof        - the proof provided with the transaction
	 */
	public static void receiveTransaction(Verification verification, Transaction transaction, Proof proof) {
		//If we have seen this transaction before, reject it
		if (verification.isCached(transaction)) {
			Log.log(Level.WARNING, "Received a transaction we already received before!");
			return;
		}
		
		if (!verification.isValid(transaction, proof)) return;
		
		proof.applyUpdates();
	}
}
