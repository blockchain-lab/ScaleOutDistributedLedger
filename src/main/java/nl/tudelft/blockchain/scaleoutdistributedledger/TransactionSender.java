package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.*;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.io.IOException;
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
public class TransactionSender {
	
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final LocalStore localStore;
	private final SocketClient socketClient;
	private final AtomicInteger taskCounter = new AtomicInteger();
	private boolean stopping;
	
	/**
	 * Creates a new TransactionSender.
	 * @param localStore - the local store
	 */
	public TransactionSender(LocalStore localStore) {
		this.localStore = localStore;
		this.socketClient = new SocketClient();
	}
	
	/**
	 * Schedules the {@code toSend} block to be sent.
	 * The block will only be sent when at least {@link SimulationMain#REQUIRED_COMMITS} have
	 * been committed.
	 * @param toSend - the block to send
	 */
	public void scheduleBlockSending(Block toSend) {
		final Chain chain = toSend.getOwner().getChain();
		Runnable runnable = new Runnable() {
			//We need to start 
			private Block lastCheckedBlock = toSend.getPreviousBlock();
			private int committedBlocks;
			
			@Override
			public void run() {
				synchronized (chain) {
					//We iterate over all the blocks since the last checked block and count how many are on the main chain.
					//We have to check the same parts of the chain every time, since blocks can get committed after we checked them.
					ListIterator<Block> lit = chain.getBlocks().listIterator(lastCheckedBlock.getNumber() - Block.GENESIS_BLOCK_NUMBER + 1);
					while (lit.hasNext() && committedBlocks < SimulationMain.REQUIRED_COMMITS) {
						Block block = lit.next();
						if (block.isOnMainChain(localStore)) {
							committedBlocks++;
							lastCheckedBlock = block;
						}
					}
				}
				
				//If we didn't find enough blocks, then we reschedule to check again later.
				if (!canSend(committedBlocks)) {
					schedule(this);
					return;
				}
				
				//There are enough blocks on the chain, so we can send the block.
				sendBlock(toSend);
				taskCounter.decrementAndGet();
			}
		};
		taskCounter.incrementAndGet();
		executor.schedule(runnable, SimulationMain.INITIAL_SENDING_DELAY, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * @param committedBlocks - the number of blocks that have been committed
	 * @return - if a block can be sent
	 */
	public boolean canSend(int committedBlocks) {
		if (stopping && committedBlocks >= 1) {
			return true;
		}
		
		return committedBlocks >= SimulationMain.REQUIRED_COMMITS;
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
		while (taskCounter.get() != 0) {
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
		taskCounter.set(0);
	}
	
	/**
	 * Sends the transactions in the given block.
	 * @param block - the block
	 */
	private void sendBlock(Block block) {
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

		ProofConstructor proofConstructor = new ProofConstructor(transaction);
		Proof proof;
		synchronized (localStore.getOwnNode().getChain()) {
			proof = proofConstructor.constructProof();
		}

		ProofMessage msg = new ProofMessage(proof);
		long timeDelta = System.currentTimeMillis() - startingTime;
		if(timeDelta > 5 * 1000) {
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
	
	/**
	 * @param runnable - the runnable to schedule
	 * @return         - the ScheduledFuture
	 */
	private ScheduledFuture<?> schedule(Runnable runnable) {
		return executor.schedule(runnable, SimulationMain.SENDING_WAIT_TIME, TimeUnit.MILLISECONDS);
	}
}
