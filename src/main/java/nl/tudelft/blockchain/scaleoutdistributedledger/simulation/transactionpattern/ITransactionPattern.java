package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.TrackerHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.TransactionCreator;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Interface for a transaction pattern.
 */
public interface ITransactionPattern extends Serializable {
	/**
	 * @return the name of this transaction pattern
	 */
	public String getName();
	
	/**
	 * @return - the number of blocks to create before committing to the main chain
	 */
	public int getCommitEvery();
	
	/**
	 * Selects the node to send money to.
	 * @param localStore - the local store
	 * @return           - the selected node
	 */
	public Node selectNode(LocalStore localStore);
	
	/**
	 * Selects the amount of money to send.
	 * @param localStore - the local store
	 * @return           - the selected amount or -1 if not enough money
	 */
	public long selectAmount(LocalStore localStore);

	/**
	 * Perform one or more actions.
	 * @param localStore - the local store
	 * @throws InterruptedException - if the action is interrupted
	 */
	public default void doAction(LocalStore localStore) throws InterruptedException {
		Log.log(Level.FINER, "Start doAction of node " + localStore.getOwnNode().getId());
		
		//Select receiver and amount
		long amount = selectAmount(localStore);
		if (amount == -1) {
			Log.log(Level.INFO, "Not enough money to make transaction!");
			return;
		}
		
		Node receiver = selectNode(localStore);
		
		OwnNode ownNode = localStore.getOwnNode();
		Block newBlock;
		Log.log(Level.FINE, "Going to make transaction: $ " + amount + " from " + ownNode.getId() + " -> " + receiver.getId());
		synchronized (ownNode.getChain()) {
			//Create the transaction
			TransactionCreator creator = new TransactionCreator(localStore, receiver, amount);
			Transaction transaction = creator.createTransaction();
			
			//Create the block
			//TODO how many transactions do we put in one block?
			List<Transaction> transactions = new ArrayList<>();
			transactions.add(transaction);
			
			//Add block to local chain
			newBlock = ownNode.getChain().appendNewBlock(transactions);
		}
		
		//Ensure that the block is sent at some point
		localStore.getApplication().getTransactionSender().scheduleBlockSending(newBlock);
		
		//Check if we want to commit the new block, and commit it if we do.
		commitBlocks(localStore, false);
	}
	
	/**
	 * @param uncommittedBlocks - the number of blocks that have not yet been committed
	 * @return                  - true if we should commit now
	 */
	public default boolean shouldCommitBlocks(int uncommittedBlocks) {
		return uncommittedBlocks >= getCommitEvery();
	}
	
	/**
	 * Commits blocks to the main chain if necessary.
	 * @param localStore - the local store
	 * @param force      - if true, then committing is forced
	 * @throws InterruptedException - if sending transactions is interrupted
	 */
	public default void commitBlocks(LocalStore localStore, boolean force) throws InterruptedException {
		Chain ownChain = localStore.getOwnNode().getChain();
		synchronized (ownChain) {
			Block lastBlock = ownChain.getLastBlock();
			Block lastCommitted = ownChain.getLastCommittedBlock();
			//TODO The last committed block should never be null (should be at least the genesis block)
			
			//Don't commit if we don't have anything to commit
			if (lastBlock == lastCommitted) return;
			
			if (!force && !shouldCommitBlocks(lastBlock.getNumber() - lastCommitted.getNumber())) return;
			
			//Commit to main chain
			BlockAbstract blockAbstract = lastBlock.calculateBlockAbstract();
			localStore.getApplication().getMainChain().commitAbstract(blockAbstract);
			ownChain.setLastCommittedBlock(lastBlock);
		}
	}

	/**
	 * @param localStore - the local store
	 * @return             the time until the next action
	 */
	public long timeUntilNextAction(LocalStore localStore);
	
	/**
	 * Called once whenever we stop running.
	 * @param localStore - the local store
	 */
	public default void onStop(LocalStore localStore) {
		try {
			commitBlocks(localStore, true);
		} catch (InterruptedException ex) {
			Log.log(Level.SEVERE, "Interrupted while committing blocks!");
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Unable to commit blocks onStop: ", ex);
		} finally {
			//TODO: Call this method somewhere else
			localStore.getApplication().onStopTransacting();
		}
	}

	/**
	 * Creates a CancellableInfiniteRunnable for executing this transaction pattern.
	 * @param localStore - the local store
	 * @return           - the runnable
	 */
	public default CancellableInfiniteRunnable<LocalStore> getRunnable(LocalStore localStore) {
		return new CancellableInfiniteRunnable<LocalStore>(
				localStore,
				this::doAction,
				this::timeUntilNextAction,
				this::onStop
		);
	}
}
