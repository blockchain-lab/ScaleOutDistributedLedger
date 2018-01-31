package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.*;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Class which handles sending of transactions.
 */
public class TransactionSender implements Runnable {
	
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final LocalStore localStore;
	private final SocketClient socketClient;
	private final AtomicInteger taskCounter = new AtomicInteger();
	private final Chain chain;
	private boolean stopping;
	private int alreadySent = -1;
	
	/**
	 * Creates a new TransactionSender.
	 * @param localStore - the local store
	 */
	public TransactionSender(LocalStore localStore) {
		this.localStore = localStore;
		this.socketClient = new SocketClient();
		this.chain = localStore.getOwnNode().getChain();
		
		this.executor.schedule(this, SimulationMain.INITIAL_SENDING_DELAY, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * Schedules the {@code toSend} block to be sent.
	 * The block will only be sent when at least {@link SimulationMain#REQUIRED_COMMITS} have
	 * been committed.
	 * @param toSend - the block to send
	 */
	public void scheduleBlockSending(Block toSend) {
		if (alreadySent == -1) {
			alreadySent = 0;
		}
		
		taskCounter.incrementAndGet();
	}
	
	@Override
	public void run() {
		//Send all and reschedule
		try {
			sendAllBlocksThatCanBeSent();
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Uncaught exception in transaction sender!");
		} finally {
			executor.schedule(this, SimulationMain.SENDING_WAIT_TIME, TimeUnit.MILLISECONDS);
		}
	}
	
	/**
	 * Sends all blocks that can be sent.
	 */
	public void sendAllBlocksThatCanBeSent() {
		if (alreadySent == -1) return;
		
		//[0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
		//Committed: [0, 2, 4, 6, 8]
		//Last sent: 2
		//Committed found: [3 -> 4, 5 -> 6, 7 -> 8] = [4, 6, 8]
		//We can send up to 6 (inclusive)
		//3 commits found, 2 required
		
		int lastBlockNr = chain.getLastBlockNumber();
		
		//Determine what blocks have been committed since the last batch we sent
		List<Integer> committed = new ArrayList<>();
		for (int index = alreadySent + 1; index <= lastBlockNr; index++) {
			Block current = chain.getBlocks().get(index);
			Block next = current.getNextCommittedBlock();
			if (next == null || !next.isOnMainChain(localStore)) break;
			
			committed.add(next.getNumber());
			index = next.getNumber();
		}
		
		//Not enough commits
		if (committed.size() < SimulationMain.REQUIRED_COMMITS) return;
		
		//Send all the blocks that we haven't sent up to the committed block (inclusive)
		int lastToSend = committed.get(committed.size() - SimulationMain.REQUIRED_COMMITS);
		for (int blockNr = alreadySent + 1; blockNr <= lastToSend; blockNr++) {
			sendBlock(chain.getBlocks().get(blockNr));
		}
	}
	
	/**
	 * @return - the number of blocks currently waiting to be sent
	 */
	public int blocksWaiting() {
		return taskCounter.get();
	}
	
	/**
	 * Sleeps until all transactions have been sent.
	 * @throws InterruptedException - If we are interrupted while waiting.
	 */
	public void waitUntilDone() throws InterruptedException {
		while (alreadySent < chain.getLastBlockNumber()) {
			Thread.sleep(1000L);
		}
	}
	
	/**
	 * Indicates that we want to stop. This reduces the REQUIRED_COMMITS to 1, to ensure that all
	 * remaining blocks get flushed whenever that is possible.
	 */
	public void stop() {
		stopping = true;
	}
	
	/**
	 * Shuts down this Transaction sender as quickly as possible.
	 * Pending blocks will not be sent.
	 */
	public void shutdownNow() {
		executor.shutdownNow();
		socketClient.shutdown();
	}
	
	/**
	 * Sends the transactions in the given block.
	 * @param block - the block
	 */
	private void sendBlock(Block block) {
		alreadySent = block.getNumber();
		for (Transaction transaction : block.getTransactions()) {
			try {
				sendTransaction(transaction);
			} catch (Exception ex) {
				Log.log(Level.SEVERE, "Unable to send transaction " + transaction, ex);
			}
		}
	}
	
	/**
	 * Sends the given transaction.
	 * Can block up to 60 seconds.
	 * @param transaction - the transaction to send
	 * @return            - if the sending succeeded
	 * @throws InterruptedException - If the current thread was interrupted while sending.
	 */
	private boolean sendTransaction(Transaction transaction) throws InterruptedException, IOException {
		Log.log(Level.FINE, "Node " + transaction.getSender().getId() + " starting sending transaction: " + transaction);
		long startingTime = System.currentTimeMillis();
		Node to = transaction.getReceiver();

		//TODO IMPORTANT Removed synchronization
		ProofConstructor proofConstructor = new ProofConstructor(transaction);
		Proof proof = proofConstructor.constructProof();

		ProofMessage msg = new ProofMessage(proof);
		long timeDelta = System.currentTimeMillis() - startingTime;
		if (timeDelta > 5 * 1000) {
			Log.log(Level.WARNING, "Proof creation took " + timeDelta + " ms for transaction: " + transaction);
		}
		Log.log(Level.FINE, "Node " + transaction.getSender().getId() + " now actually sending transaction: " + transaction);
		if (socketClient.sendMessage(to, msg)) {
			to.updateMetaKnowledge(proof);
			Log.log(Level.FINE, "Node " + transaction.getSender().getId() + " done sending transaction: " + transaction);
			return true;
		}
		
		return false;
	}
}
