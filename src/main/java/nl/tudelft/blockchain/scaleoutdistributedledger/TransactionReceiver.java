package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.validation.ValidationException;

/**
 * Class which handles receiving transactions.
 */
public class TransactionReceiver implements Runnable {
	
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final LocalStore localStore;
	private final LinkedBlockingQueue<ProofMessage> queue = new LinkedBlockingQueue<>();
	
	/**
	 * Creates a new TransactionReceiver.
	 * @param localStore - the local store
	 */
	public TransactionReceiver(LocalStore localStore) {
		this.localStore = localStore;
		this.executor.schedule(this, SimulationMain.DELIVER_RECHECK_TIME, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void run() {
		//Deliver all and reschedule
		try {
			deliverAllTransactionsThatCanBeDelivered();
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Uncaught exception in transaction receiver!");
		} finally {
			executor.schedule(this, SimulationMain.DELIVER_RECHECK_TIME, TimeUnit.MILLISECONDS);
		}
	}
	
	/**
	 * @param proofMsg - the proof message of the transaction
	 */
	public void receiveTransaction(ProofMessage proofMsg) {
		queue.add(proofMsg);
	}
	
	/**
	 * Delivers all transactions that can be delivered.
	 */
	public void deliverAllTransactionsThatCanBeDelivered() {
		ProofMessage proofMsg = queue.peek();
		while (proofMsg != null) {
			//We cannot deliver this proof message yet. To preserve FIFO, just stop.
			if (proofMsg.getRequiredHeight() > localStore.getMainChain().getCurrentHeight()) break;
			proofMsg = queue.poll();
			deliverProof(proofMsg);
			
			proofMsg = queue.peek();
		}
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
		executor.shutdownNow();
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
		
		Log.log(Level.FINE, "Received transaction: " + proof.getTransaction());
		
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
		Log.log(Level.FINE, "Transaction " + proof.getTransaction() + " is valid, applying updates...");
		proof.applyUpdates(localStore);
		TrackerHelper.registerTransaction(proof);

		if (proof.getTransaction().getAmount() > 0) {
			localStore.addUnspentTransaction(proof.getTransaction());
		}

		return true;
	}
}
