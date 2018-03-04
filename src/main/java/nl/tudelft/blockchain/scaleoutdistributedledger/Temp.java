package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.Arrays;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

public class Temp {
	/*
	 
	 
	 */
	
	
	/**
	 * Determines the blocks that are required for the given transaction.
	 * This method will set the cache on all transactions that are not already cached.
	 * 
	 * <pre>
	 * Base case: block in cache
	 * Base case: transaction in cache
	 * Base case: genesis block -> empty requirements
	 * Normal case: previous block requirements + own transaction requirements
	 * </pre>
	 * 
	 * This method uses {@link #getBlockRequirementsBackward} on the previous block and then
	 * {@link #getBlockRequirementsBackward} again on the block of each source transaction
	 * (in that order).
	 * 
	 * @param size        - the number of nodes
	 * @param transaction - the transaction to check the sources of
	 * @return            - the array corresponding to this transaction
	 */
	public static int[] setTransactionRequirements(final int size, Transaction transaction) {
		Block block = transaction.getBlock();
		int[] req;
//		
//		//Base case: block already computed
//		int[] req = block.getCachedRequirements();
//		if (req != null) {
////			transaction.setCachedRequirements(req);
//			return req;
//		}
		
		//Base case: If genesis, then stop, return empty array.
		final Block prevBlock = block.getPreviousBlock();
		if (prevBlock == null) {
			if (block.getNumber() != Block.GENESIS_BLOCK_NUMBER) {
				throw new IllegalStateException("Non genesis!");
			}
			
			req = new int[size];
//			block.setCachedRequirements(req);
			transaction.setCachedRequirements(req);
			return req;
		}
		
		//Normal case: compute from previous block, add sources
		//All this is guaranteed to be before us, but might need to be updated forward.
		req = new int[size];
		
		//Add all the requirements of sources
		for (Transaction source : transaction.getSource()) {
			maxArray(req, getTransactionRequirements(size, source));
		}
		
		final int senderId = transaction.getSender().getId();
		req[senderId] = Math.max(req[senderId], block.getNumber());
		
		maxArray(req, getBlockRequirementsBackward(size, prevBlock));
		
		//Set the cache
//		transaction.setCachedRequirements(req);
		return req;
	}
	
//	/**
//	 * Fills in the given requirements array with the requirements of the given transaction.
//	 * @param size           - the number of nodes
//	 * @param transaction    - the transaction to check the sources of
//	 * @param blocksRequired - the array to add the block requirements to
//	 */
//	public static void fillTransactionRequirements(final int size, Transaction transaction, int[] blocksRequired) {
//		//Base case: If genesis, then stop
//		if (transaction.getBlockNumber() == Block.GENESIS_BLOCK_NUMBER) return;
//		
//		Block block = transaction.getBlock();
//		
//		//Base case: block cached
//		int[] cached = block.getCachedRequirements();
//		if (cached != null) {
//			maxArray(blocksRequired, cached);
//			return;
//		}
//		
//		//Base case: transaction already cached
//		cached = transaction.getCachedRequirements();
//		if (cached != null) {
//			maxArray(blocksRequired, cached);
//			return;
//		}
//		
//		//Determine the requirements of the previous block
//		Block prevBlock = block.getPreviousBlock();
//		cached = Arrays.copyOf(getBlockRequirementsBackward(size, prevBlock), size);
//
//		final int senderId = transaction.getSender().getId();
//		cached[senderId] = Math.max(cached[senderId], block.getNumber());
//		
//		//Add the requirements of the sources 
//		for (Transaction source : transaction.getSource()) {
//			fillTransactionRequirements(size, source, cached);
//		}
//		
//		//Set the requirements on the transaction
//		transaction.setCachedRequirements(cached);
//		
//		//Update the blocks required with the new array
//		maxArray(blocksRequired, cached);
//	}
	
	//t3 -> t2 -> t1
	//req t2 = req block(t2)
	
	/**
	 * Gets the requirements of a transaction.
	 * 
	 * <pre>
	 * Base case: in cache
	 * Normal case: set transaction requirements from previous block + sources
	 * </pre>
	 * 
	 * This method uses {@link #setTransactionRequirements}.
	 * 
	 * @param size        - the total number of nodes
	 * @param transaction - the transaction to get the requirements of
	 * @return            - the requirements of the given transaction
	 */
	public static int[] getTransactionRequirements(final int size, Transaction transaction) {
		int[] cached = transaction.getCachedRequirements();
		if (cached != null) return cached;
		
		//Set cache on transaction and sources
		int[] req = setTransactionRequirements(size, transaction);
		transaction.setCachedRequirements(req);
		return req;
	}
	
	/**
	 * Gets block requirements backwards.
	 * 
	 * <pre>
	 * Base case: in cache
	 * Normal case: set block requirements backwards, then return cached.
	 * </pre>
	 * @param size  - the size
	 * @param block - the block
	 * @return      - the cached requirements array
	 * @see #setBlockRequirementsBackward 
	 */
	public static int[] getBlockRequirementsBackward(final int size, Block block) {
		int[] cached = block.getCachedRequirements();
		if (cached != null) return cached;
		
		setBlockRequirementsBackward(size, block);
		return block.getCachedRequirements();
	}
	
	/**
	 * Fills block requirements in a backward fashion.
	 * 
	 * <pre>
	 * Base case: genesis block -> empty requirements
	 * Normal case: normal block -> previous block requirements + own transaction requirements
	 * </pre>
	 * 
	 * This method uses {@link #getBlockRequirementsBackward} on the previous block and
	 * {@link #getTransactionRequirements} on each transaction in the block (in that order).
	 * @param size  - the number of nodes in the system
	 * @param block - the block to set requirements of
	 */
	public static void setBlockRequirementsBackward(final int size, Block block) {
		final int[] required = new int[size];
		
		//Base case: Genesis Block, empty requirements.
		final Block prev = block.getPreviousBlock();
		if (prev == null) {
			if (block.getNumber() != Block.GENESIS_BLOCK_NUMBER) throw new IllegalStateException("Non genesis block with no previous!");
			
			block.setCachedRequirements(required);
			return;
		}

		//Normal case: Non genesis
		
		//Set own block id as initial requirement
		required[block.getOwner().getId()] = block.getNumber();
		
		//Add the requirements of the transactions in this block
		for (Transaction transaction : block.getTransactions()) {
			//Transaction requirements = previous block (cached) + sources
			maxArray(required, getTransactionRequirements(size, transaction));
		}
		
		//Add the requirements of the previous block
		int[] prevReq = getBlockRequirementsBackward(size, prev);
		maxArray(required, prevReq);
		
		//The requirements for this block have now been finalized
		block.setCachedRequirements(required);
	}
	
	protected static void setBlockRequirementsForward(final int size, Block block) {
		Block next = block.getNextCommittedBlock();
		
		//We need to fix all the requirements
		if (next == null) {
			throw new IllegalStateException("We cannot fill in block requirements forward when there is no next committed block! "
					+ "B" + block.getOwner().getId() + ":" + block.getNumber());
		}
		
		if (next == block) {
			//This block is itself committed, this means that all blocks before it should have the same requirements
			
			//Fill all the caches backwards
			int[] blockReq = getBlockRequirementsBackward(size, next);
			
			Block prev = next.getPreviousBlock();
			
			//Genesis block, we are done
			if (prev == null) return;
			
			//Scan backwards from the commit till the block that is no longer in the requirements that need to be updated
			while (prev.getNextCommittedBlock() == next) {
				//Adapt the requirements of all previous blocks and transactions
				prev.setCachedRequirements(blockReq);
				for (Transaction t : prev.getTransactions()) {
					t.setCachedRequirements(blockReq);
				}
				
				prev = prev.getPreviousBlock();
				if (prev == null) break;
			}
		} else {
			throw new IllegalArgumentException("Cannot set block requirements forward on non-committed block!");
		}
	}
	
	public static void fillInBlockRequirementsForCommit(Block block) {
		//Do a fix forward of the last block covered by the given block.
		//All blocks before this block WILL already be finalized and have caches.
		//The job of this method is to now fix all the caches to be the same.
		final int size = Settings.INSTANCE.totalNodesNumber;
		
		setBlockRequirementsForward(size, block);
	}
	
	private static void maxArray(int[] target, int[] max) {
		final int length = target.length;
		for (int i = 0; i < length; i++) {
			target[i] = Math.max(target[i], max[i]);
		}
	}
	
	//for (transaction ti -> transaction tj, where j = next committed block) {
	//PHASE 1: Collect, B1T1 -> values, B1T2 -> values, B2T1 -> values
	//PHASE 2: Join mixed requirements
	//PHASE 3: 
}
