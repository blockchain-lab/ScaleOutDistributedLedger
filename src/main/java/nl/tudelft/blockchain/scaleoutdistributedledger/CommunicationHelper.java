package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ValidationException;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Helper class for communication.
 */
public final class CommunicationHelper {
	private CommunicationHelper() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * @param proof         - the proof provided with the transaction
	 * @param localStore	- the localstore of the node
	 * @return               true if the transaction was accepted, false otherwise
	 */
	public static boolean receiveTransaction(Proof proof, LocalStore localStore) {
		Log.log(Level.INFO, "Received transaction: " + proof.getTransaction());
		
		if (proof.getTransaction().getReceiver().getId() != localStore.getOwnNode().getId()) {
			Log.log(Level.WARNING, "Received a transaction that isn't for us: " + proof.getTransaction());
			return false;
		}
		
		try {
			localStore.getVerification().validateNewMessage(proof, localStore);
		} catch (ValidationException ex) {
			Log.log(Level.WARNING, "Received an invalid transaction/proof.", ex);
			return false;
		}
		
		Log.log(Level.INFO, "Transaction " + proof.getTransaction() + " is valid, applying updates...");
		proof.applyUpdates(localStore);

		if (proof.getTransaction().getAmount() > 0) {
			localStore.addUnspentTransaction(proof.getTransaction());
		}

		return true;
	}
}
