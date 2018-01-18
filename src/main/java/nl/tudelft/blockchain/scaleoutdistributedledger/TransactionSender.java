package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.util.ListIterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Class which handles sending of transactions.
 */
public class TransactionSender {
	/**
	 * The initial delay in milliseconds to wait before checking for the first time.
	 */
	public static final long INITIAL_DELAY = 2000;
	
	/**
	 * The time in milliseconds to wait before checking again.
	 */
	public static final long WAIT_TIME = 5000;
	
	/**
	 * The number of blocks (with the same or higher block number) that need to be committed before
	 * we send a certain block.
	 */
	public static final int REQUIRED_COMMITS = 1;
	
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private final LocalStore localStore;
	private final SocketClient socketClient;
	private final AtomicInteger taskCounter = new AtomicInteger();
	
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
	 * The block will only be sent when at least {@link TransactionSender#REQUIRED_COMMITS} have
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
					while (lit.hasNext() && committedBlocks < REQUIRED_COMMITS) {
						Block block = lit.next();
						if (block.isOnMainChain(localStore)) {
							committedBlocks++;
							lastCheckedBlock = block;
						}
					}
				}
				
				//If we didn't find enough blocks, then we reschedule to check again later.
				if (committedBlocks < REQUIRED_COMMITS) {
					schedule(this);
					return;
				}
				
				//There are enough blocks on the chain, so we can send the block.
				sendBlock(toSend);
				taskCounter.decrementAndGet();
			}
		};
		taskCounter.incrementAndGet();
		executor.schedule(runnable, INITIAL_DELAY, TimeUnit.MILLISECONDS);
	}
	
	/**
	 * @return - the number of blocks currently waiting to be sent
	 */
	public int blocksWaiting() {
		return taskCounter.get();
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
		Node to = transaction.getReceiver();
		Proof proof = Proof.createProof(transaction);
		if (socketClient.sendMessage(to, new ProofMessage(proof))) {
			to.updateMetaKnowledge(proof);
			return true;
		}
		
		return false;
	}
	
	/**
	 * @param runnable - the runnable to schedule
	 * @return         - the ScheduledFuture
	 */
	private ScheduledFuture<?> schedule(Runnable runnable) {
		return executor.schedule(runnable, WAIT_TIME, TimeUnit.MILLISECONDS);
	}
}
