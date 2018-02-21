package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ValidationException;

/**
 * Class which handles receiving transactions.
 */
public class TransactionReceiver extends Thread {
	
	private final LocalStore localStore;
	private final LinkedBlockingQueue<ProofMessage> queue = new LinkedBlockingQueue<>();
	private volatile boolean stop;
	
	/**
	 * Creates a new TransactionReceiver.
	 * @param localStore - the local store
	 */
	public TransactionReceiver(LocalStore localStore) {
		super("transaction-receiver-" + localStore.getOwnNode().getId());
		this.localStore = localStore;
		this.start();
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				deliverOneTransaction();
			} catch (InterruptedException ex) {
				if (stop) break;
			} catch (Exception ex) {
				Log.log(Level.SEVERE, "Uncaught exception in transaction receiver!", ex);
			}
		}
	}

	/**
	 * Delivers a single transaction.
	 * @throws InterruptedException - If we are interrupted while waiting.
	 */
	protected void deliverOneTransaction() throws InterruptedException {
		ProofMessage proofMsg = queue.take();
		while (proofMsg.getRequiredHeight() > localStore.getMainChain().getCurrentHeight()) {
			Thread.sleep(SimulationMain.DELIVER_RECHECK_TIME);
		}
		deliverProof(proofMsg);
	}
	
	/**
	 * @param proofMsg - the proof message of the transaction
	 */
	public void receiveTransaction(ProofMessage proofMsg) {
		queue.add(proofMsg);
	}
	
	/**
	 * @return - the number of transactions currently waiting to be delivered
	 */
	public int transactionsWaiting() {
		return queue.size();
	}
	
	/**
	 * Sleeps until all transactions have been delivered.
	 * @throws InterruptedException - If we are interrupted while waiting.
	 */
	public void waitUntilDone() throws InterruptedException {
		while (transactionsWaiting() > 0) {
			Thread.sleep(1000L);
		}
	}
	
	/**
	 * Shuts down this Transaction receiver as quickly as possible.
	 * Pending transactions will not be delivered.
	 */
	public void shutdownNow() {
		stop = true;
		this.interrupt();
	}
	
	/**
	 * Delivers the given proof message.
	 * @param proofMsg - the proof message
	 * @return         - true if the transaction was accepted, false otherwise
	 */
	private boolean deliverProof(ProofMessage proofMsg) {
		Proof proof;
		try {
			proof = new Proof(proofMsg, localStore);
		} catch (IOException ex) {
			Log.log(Level.SEVERE, "Exception while handling proof message", ex);
			return false;
		}
		
		if (proof.getTransaction().getReceiver().getId() != localStore.getOwnNode().getId()) {
			Log.log(Level.WARNING, "Received a transaction that isn't for us: " + proof.getTransaction());
			return false;
		}
		
		try {
			localStore.getVerification().validateNewMessage(proof, localStore);
		} catch (ValidationException ex) {
			Log.log(Level.WARNING, "Received an invalid transaction/proof " + proof.getTransaction() + ": " + ex.getMessage());
			return false;
		}

		Log.log(Level.INFO, "Received and validated transaction: " + proof.getTransaction());
		proof.applyUpdates(localStore);
		TrackerHelper.registerTransaction(proof);

		if (proof.getTransaction().getAmount() > 0) {
			localStore.addUnspentTransaction(proof.getTransaction());
		}

		return true;
	}
}
