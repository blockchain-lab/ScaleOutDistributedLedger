package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.logging.Level;

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
		//If we have seen this transaction before, reject it
		if (localStore.getVerification().isCached(proof.getTransaction())) {
			Log.log(Level.WARNING, "Received a transaction we already received before!");
			return false;
		}

		if (!localStore.getVerification().isValid(proof.getTransaction(), proof, localStore)) {
			Log.log(Level.WARNING, "Received an invalid transaction/proof: " + proof.getTransaction());
			return false;
		}
		
		proof.applyUpdates();

		Log.log(Level.INFO, "Received and validated transaction: " + proof.getTransaction());

		//TODO: Update metaKnowledge based on what we received?

		if (proof.getTransaction().getAmount() > 0) {
			localStore.addUnspentTransaction(proof.getTransaction());
		}

		return true;
	}
}
