package nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.SimulationMain;
import nl.tudelft.blockchain.scaleoutdistributedledger.TransactionCreator;
import nl.tudelft.blockchain.scaleoutdistributedledger.TransactionSender;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.*;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.io.Serializable;
import java.util.logging.Level;

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

		// Make sure we have some room
		if (localStore.getApplication().getTransactionSender().blocksWaiting() >= SimulationMain.MAX_BLOCKS_PENDING) {
			Log.log(Level.FINE, "Too many blocks pending, skipping transaction creation!");
			return;
		}
		
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

			//TODO how many transactions do we put in one block?
			//Add block to local chain
			newBlock = ownNode.getChain().appendNewBlock();
			newBlock.addTransaction(transaction);
			Log.log(Level.FINE, "Node " + ownNode.getId() + " added transaction " + transaction.getNumber() + " in block " + newBlock.getNumber());
		}
		
		//Ensure that the block is sent at some point
		localStore.getApplication().getTransactionSender().scheduleBlockSending(newBlock);
		
		//Check if we want to commit the new block, and commit it if we do.
		commitBlocks(localStore, false);
	}
	
	/**
	 * @param lastBlock     - the last block in the chain
	 * @param lastCommitted - the last committed block
	 * @return              - true if we should commit now
	 */
	public default boolean shouldCommitBlocks(Block lastBlock, Block lastCommitted) {
		int uncommitted = lastBlock.getNumber() - lastCommitted.getNumber();
		return uncommitted >= getCommitEvery();
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
			
			//Don't commit if we don't have anything to commit
			if (lastBlock == lastCommitted) return;
			
			if (force || shouldCommitBlocks(lastBlock, lastCommitted)) {
				lastBlock.commit(localStore);
			}
		}
	}
	
	/**
	 * Commits extra empty blocks.
	 * @param localStore - the local store
	 */
	public default void commitExtraEmpty(LocalStore localStore) {
		Chain ownChain = localStore.getOwnNode().getChain();
		for (int i = 0; i < SimulationMain.REQUIRED_COMMITS; i++) {
			synchronized (ownChain) {
				Block block = ownChain.appendNewBlock();
				block.commit(localStore);
			}
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
			commitExtraEmpty(localStore);
		} catch (InterruptedException ex) {
			Log.log(Level.SEVERE, "Interrupted while committing blocks!");
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Unable to commit blocks onStop: ", ex);
		} finally {
			localStore.getApplication().finishTransactionSending();
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
