package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.NamedThreadFactory;

import lombok.Getter;

/**
 * Class which handles sending of transactions.
 */
public class TransactionSender implements Runnable {
	
	private final ScheduledExecutorService executor;
	private final LocalStore localStore;
	@Getter
	private final SocketClient socketClient;
	private final Chain chain;
	private int alreadySent;
	
	/**
	 * Creates a new TransactionSender.
	 * @param localStore - the local store
	 */
	public TransactionSender(LocalStore localStore) {
		this.localStore = localStore;
		this.socketClient = new SocketClient(localStore);
		this.chain = localStore.getOwnNode().getChain();
		NamedThreadFactory ntf = new NamedThreadFactory("transaction-sender-" + localStore.getOwnNode().getId());
		this.executor = Executors.newScheduledThreadPool(1, ntf);
		this.executor.schedule(this, Settings.INSTANCE.initialSendingDelay, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void run() {
		//Send all and reschedule
		try {
			sendAllBlocksThatCanBeSent();
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Uncaught exception in transaction sender!", ex);
		} finally {
			executor.schedule(this, Settings.INSTANCE.sendingWaitTime, TimeUnit.MILLISECONDS);
		}
	}
	
	/**
	 * Sends all blocks that can be sent.
	 */
	public void sendAllBlocksThatCanBeSent() {
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
		if (committed.size() < Settings.INSTANCE.requiredCommits) return;
		
		//Send all the blocks that we haven't sent up to the committed block (inclusive)
		int lastToSend = committed.get(committed.size() - Settings.INSTANCE.requiredCommits);
		for (int blockNr = alreadySent + 1; blockNr <= lastToSend; blockNr++) {
			sendBlock(chain.getBlocks().get(blockNr));
		}
	}
	
	/**
	 * @return - the number of blocks currently waiting to be sent
	 */
	public int blocksWaiting() {
		Block block = chain.getLastBlock();
		if (block == null) return 0;
		
		Block prev = block;
		while (block != null && block.getTransactions().isEmpty()) {
			prev = block;
			block = block.getPreviousBlock();
		}
		
		return prev.getNumber() - alreadySent;
	}
	
	/**
	 * Sleeps until all transactions have been sent.
	 * @throws InterruptedException - If we are interrupted while waiting.
	 */
	public void waitUntilDone() throws InterruptedException {
		while (blocksWaiting() > 0) {
			Thread.sleep(1000L);
		}
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
		long startingTime = System.currentTimeMillis();
		Node to = transaction.getReceiver();
		long requiredHeight = localStore.getMainChain().getCurrentHeight();

		ProofConstructor proofConstructor = new ProofConstructor(transaction, localStore);
		Proof proof = proofConstructor.constructProof();
		ProofMessage msg = new ProofMessage(proof);
		msg.setRequiredHeight(requiredHeight);
		
		//Check if the proof creation took a long time and log it.
		long timeDelta = System.currentTimeMillis() - startingTime;
		if (timeDelta > 5 * 1000) {
			Log.log(Level.WARNING, "Proof creation took " + timeDelta + " ms for transaction: " + transaction);
		}
		
		if (socketClient.sendMessage(to, msg)) {
			to.updateMetaKnowledge(proof);
			return true;
		}
		
		return false;
	}
}
