package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.Arrays;
import java.util.ListIterator;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

/**
 * Class for determining requirements.
 */
public class Temp2 {
	private static int size = Settings.INSTANCE.totalNodesNumber;
	
//	public static int[] determineRequirements(LocalStore localStore, Transaction transaction) {
//		int[] requirements;
//		
//		//Genesis
//		final Node sender = transaction.getSender();
//		if (sender == null) {
//			requirements = new int[size];
//			transaction.setCachedRequirements(requirements);
//			return requirements;
//		}
//		
//		//Cached
//		requirements = transaction.getCachedRequirements();
//		if (requirements != null) return requirements;
//		
//		//Create
//		requirements = new int[size];
//		transaction.setCachedRequirements(new int[0]);
//		
//		requirements[sender.getId()] = transaction.getBlockNumber();
//		for (Transaction source : transaction.getSource()) {
//			maxArray(requirements, determineRequirements(localStore, source));
//		}
//		
//
//		addBlockRequirements(localStore, transaction, requirements);
//		//First set cache. If we hit this transaction, we don't need to do anything special for it?
//		transaction.setCachedRequirements(requirements);
//		
//		return requirements;
//	}
//	
//	public static void addBlockRequirements(LocalStore localStore, Transaction transaction, int[] requirements) {
//		//We know the complete height that we need, but we might have skipped parts of chains.
//		//E.g. adding a source of Block 3 N0 and later adding a source of Block 20 N0, we haven't checked the space in between.
//		int[] extraReq = Arrays.copyOf(requirements, requirements.length);
//		for (int i = 0; i < size; i++) {
//			int nHeight = requirements[i];
//			if (nHeight == 0) continue;
//			
//			Node node = localStore.getNode(i);
//			
//			//Add the requirements of all the blocks together
//			ListIterator<Block> lit = node.getChain().getBlocks().listIterator(1, nHeight + 1);
//			while (lit.hasNext()) {
//				Block block = lit.next();
//				
//				final int[] blockReq = blockRequirements(localStore, i, block);
//				maxArray(extraReq, blockReq);
//			}
//		}
//		
//		maxArray(requirements, extraReq);
//	}
//
//	private static int[] blockRequirements(LocalStore localStore, int i, Block block) {
//		int[] blockReq = block.getCachedRequirements();
//		if (blockReq != null) return blockReq;
//		
//		blockReq = new int[size];
//		for (Transaction blockTrans : block.getTransactions()) {
//			final int[] transReq = determineRequirements(localStore, blockTrans);
//			maxArray(blockReq, transReq);
//		}
//		
//		if (blockReq[i] > block.getNumber()) {
//			System.out.println("Blockreq of N" + i + "B" + block.getNumber() + " for node " + i + " is higher than its own number!");
//		}
//		
//		blockReq[i] = block.getNumber();
//		block.setCachedRequirements(blockReq);
//		
//		return blockReq;
//	}
	
	/**
	 * @param block - the block to get the requirements for
	 * @return      - an array of length |nodes| + 1 containing the requirements for block 
	 */
	public static int[] getRequirementsCopyWithSumRoom(Block block) {
		return Arrays.copyOf(getRequirements(block), size + 1);
	}
	
	//Go from low to high, don't explore paths that are not yet covered
	/**
	 * Determines the requirements for the given block.
	 * WARNING: Do not modify the returned array.
	 * @param block - the block to get the requirements for
	 * @return      - the requirements array of the given block
	 */
	public static int[] getRequirements(Block block) {
		int[] requirements = block.getCachedRequirements();
		if (requirements != null) return requirements;
		
		//Genesis
		requirements = new int[size];
		if (block.getNumber() == Block.GENESIS_BLOCK_NUMBER) {
			block.setCachedRequirements(requirements);
			return requirements;
		}
		
		//Normal
		final int blockOwner = block.getOwner().getId();
		requirements[blockOwner] = block.getNumber();
		
		Block prev = block.getPreviousBlock();
		if (prev == null) throw new IllegalStateException("Encountered block N" + blockOwner + "B" + block.getNumber() + " without previous!");
		
		//Include requirements from previous.
		maxArray(requirements, getRequirements(prev));
		
		if (requirements[blockOwner] > block.getNumber()) {
			System.out.println("N" + blockOwner + "B" + block.getNumber()
			+ " has requirements higher than the own block number (" + requirements[blockOwner] + ")");
		}
		
		//Include transaction sources requirements
		for (Transaction transaction : block.getTransactions()) {
			for (Transaction source : transaction.getSource()) {
				maxArray(requirements, getRequirements(source.getBlock()));
			}
		}
		
		if (requirements[blockOwner] > block.getNumber()) {
			System.out.println("N" + blockOwner + "B" + block.getNumber()
			+ " has requirements higher than the own block number (" + requirements[blockOwner] + ")");
		}
		
		block.setCachedRequirements(requirements);
		return requirements;
	}
	
	//0, 1 -> 0, 2 -> 1, 3 -> 2 + ext1, ext1 -> ext0, 4 -> 3, 5 -> 3 + ext4, ext4 -> ext3, ext3 -> ext2 + 1, 1 is incorrect at this point.
	//Because 1 will require more
	//However, in order for ext to refer to 1, then 1 must have already been committed in the past.
	//So the requirements of 1 should have been fixed.
	
	/**
	 * Updates the requirements of the given (committed) block and all previous blocks affected.
	 * @param block - the block that is being committed
	 * @return      - the new requirements
	 * @throws IllegalArgumentException - If the given block is not being committed.
	 */
	public static int[] updateRequirementsForCommit(Block block) {
		final int blockNr = block.getNumber();
		if (blockNr == Block.GENESIS_BLOCK_NUMBER) throw new IllegalArgumentException("You cannot commit a genesis block!");
		
		if (block.getNextCommittedBlock() != block) throw new IllegalArgumentException("This method may only be called on blocks that are being committed!");
		
		
		//Determine the index of the first block affected by this commit.
		final Node owner = block.getOwner();
		int index = owner.getChain().getLastCommittedBlock().getNumber() + 1;
		
//		//An alternative method to determine the first affected block would be:
//		int index = blockNr;
//		Block prev = block.getPreviousBlock();
//		while (prev != null && prev.getNextCommittedBlock() == block) {
//			index--;
//			prev = prev.getPreviousBlock();
//		}

		//Only the block itself is committed.
		if (index == blockNr) return getRequirements(block);
		
		//Starting from the furthest block, build up the requirements and chain them through.
		int[] requirements = new int[size];
		ListIterator<Block> it = owner.getChain().getBlocks().listIterator(index, blockNr);
		while (it.hasNext()) {
			Block otherBlock = it.next();
			maxArray(requirements, getRequirements(otherBlock));
			
			//Set the cache to the same array to reflect future updates
			otherBlock.setCachedRequirements(requirements);
		}
		
		if (requirements[owner.getId()] > blockNr) {
			System.out.println("N" + owner.getId() + "B" + blockNr
			+ " has requirements higher than the own block number (" + requirements[owner.getId()] + ")");
		}
		
		//Determine own requirements
		maxArray(requirements, getRequirements(block));
		requirements[owner.getId()] = blockNr;
		
		//Set own cache
		block.setCachedRequirements(requirements);
		
		return requirements;
	}
	
	/**
	 * For each index i, this function assigns the maximum of target[i] and toAdd[i] to target[i].
	 * @param target - the target array
	 * @param toAdd  - the array to use in the max
	 */
	private static void maxArray(int[] target, int[] toAdd) {
		for (int i = 0; i < size; i++) {
			target[i] = Math.max(target[i], toAdd[i]);
		}
	}
}
