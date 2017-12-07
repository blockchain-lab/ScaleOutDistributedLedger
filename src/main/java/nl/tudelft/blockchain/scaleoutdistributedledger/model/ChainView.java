package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.List;
import java.util.ListIterator;

/**
 * Class which provides a view of a chain.
 * 
 * <p>NOTE: this implementation assumes that a chain is append only.
 */
public class ChainView implements Iterable<Block> {

	//TODO If the chain is updated after validation, the validation needs to be done again. (valid = null)
	private Chain chain;
	private List<Block> updates;
	private Boolean valid;
	
	/**
	 * @param chain
	 * 		the chain
	 * @param updates
	 * 		the blocks of this chain that were sent with the proof
	 */
	public ChainView(Chain chain, List<Block> updates) {
		this.chain = chain;
		this.updates = updates;
	}
	
	/**
	 * @return
	 * 		true if this view is valid, false otherwise
	 */
	public boolean isValid() {
		if (this.valid != null) return this.valid.booleanValue();
		
		return checkIntegrity();
	}
	
	/**
	 * @return
	 * 		true if this ChainView is valid, false otherwise
	 */
	public boolean checkIntegrity() {
		//No update --> valid
		if (updates.isEmpty()) {
			this.valid = true;
			return true;
		}
		
		//If we had no blocks, then we only need to check for gaps
		List<Block> blocks = chain.getBlocks();
		int firstUpdateNumber = updates.get(0).getNumber();
		if (blocks.isEmpty()) {
			return checkNoGaps(1, firstUpdateNumber);
		}
		
		int lastOwnNumber = blocks.get(blocks.size() - 1).getNumber();
		if (firstUpdateNumber - lastOwnNumber > 1) {
			//We are missing blocks between what we know and what we were sent! Can never happen with an honest node.
			this.valid = false;
			return false;
		} else if (lastOwnNumber >= firstUpdateNumber) {
			//There is overlap, check if exactly matches our view
			//At the same time, we will remove the overlapping elements
			int overlap = lastOwnNumber + 1 - firstUpdateNumber;
			int baseI = blocks.size() - overlap;
			for (int i = 0; i < overlap && !updates.isEmpty(); i++) {
				Block ownBlock = blocks.get(baseI + i);
				Block updatedBlock = updates.get(0);
				
				//TODO we might need a special equality check
				if (!ownBlock.equals(updatedBlock)) {
					this.valid = false;
					return false;
				}
				
				updates.remove(0);
			}
			
			return checkNoGaps(0, lastOwnNumber);
		} else {
			//The first updated block number follows directly after the last block we knew about.
			return checkNoGaps(0, lastOwnNumber);
		}
	}
	
	/**
	 * Checks that there are no gaps between the blocks received (updates).
	 * This method changes this.valid to either true or false.
	 * 
	 * @param startIndex
	 * 		the index to start checking
	 * @param previousNr
	 * 		the number of the previous block
	 * 
	 * @return
	 * 		true if there are no gaps, false otherwise
	 */
	private boolean checkNoGaps(int startIndex, int previousNr) {
		for (int i = startIndex; i < updates.size(); i++) {
			if (updates.get(i).getNumber() - previousNr != 1) {
				this.valid = false;
				return false;
			}
			previousNr++;
		}
		
		this.valid = true;
		return true;
	}
	
	/**
	 * @param number
	 * 		the number
	 * 
	 * @return
	 * 		the block with the given number
	 * 
	 * @throws IndexOutOfBoundsException
	 * 		If the block with the given number does not exist (yet).
	 */
	public Block getBlock(int number) {
		if (number < chain.getBlocks().size()) {
			return chain.getBlocks().get(number);
		} else {
			int index = number - chain.getBlocks().size();
			return updates.get(index);
		}
	}
	
	@Override
	public ListIterator<Block> iterator() {
		return new ChainViewIterator();
	}
	
	/**
	 * @return
	 * 		a ListIterator over this ChainView
	 */
	public ListIterator<Block> listIterator() {
		return new ChainViewIterator();
	}
	
	/**
	 * @param number
	 * 		the number of the block to start at
	 * 
	 * @return
	 * 		a ListIterator starting at the block with the given number
	 */
	public ListIterator<Block> listIterator(int number) {
		return new ChainViewIterator(number);
	}
	
	/**
	 * ListIterator for ChainViews.
	 */
	private class ChainViewIterator implements ListIterator<Block> {
		private ListIterator<Block> chainIterator;
		private ListIterator<Block> updatesIterator;
		private boolean updatesReached;
		private Block current;
		
		ChainViewIterator() {
			chainIterator = chain.getBlocks().listIterator();
			updatesIterator = updates.listIterator();
		}
		
		ChainViewIterator(int number) {
			int chainLength = chain.getBlocks().size();
			if (number < chainLength) {
				chainIterator = chain.getBlocks().listIterator(number);
				updatesIterator = updates.listIterator();
			} else {
				int index = number - chainLength;
				chainIterator = chain.getBlocks().listIterator(chainLength);
				updatesIterator = updates.listIterator(index);
				updatesReached = true;
			}
			
		}
		
		@Override
		public boolean hasNext() {
			if (!updatesReached) {
				if (chainIterator.hasNext()) return true;
				
				updatesReached = true;
			}
			
			return updatesIterator.hasNext();
		}
		
		@Override
		public boolean hasPrevious() {
			if (updatesReached) {
				if (updatesIterator.hasPrevious()) return true;
				
				updatesReached = false;
			}
			
			return chainIterator.hasPrevious();
		}
		
		@Override
		public Block next() {
			if (!updatesReached) {
				if (chainIterator.hasNext()) return chainIterator.next();

				updatesReached = true;
			}
			
			current = updatesIterator.next();
			return current;
		}
		
		@Override
		public Block previous() {
			if (updatesReached) {
				if (updatesIterator.hasPrevious()) return updatesIterator.previous();
				
				updatesReached = false;
			}
			
			current = chainIterator.previous();
			return current;
		}

		@Override
		public int nextIndex() {
			return current.getNumber() + 1;
		}

		@Override
		public int previousIndex() {
			return current.getNumber() - 1;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void set(Block e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(Block e) {
			throw new UnsupportedOperationException();
		}
	}
}
