package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Class which provides a view of a chain.
 * 
 * <p>NOTE: this implementation assumes that a chain is append only.
 */
public class ChainView implements Iterable<Block> {

	//TODO If the chain is updated after validation, the validation needs to be done again. if (valid) then valid = null;
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
		if(this.updates == null) this.updates = new ArrayList<>();
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
	 * If this ChainView was previously marked as valid, then it's validation will be cleared.
	 * <br><b>A ChainView that was previously marked as invalid will remain invalid.</b>
	 * 
	 * <p>This method needs to be called if the chain is updated after validation.
	 */
	public void resetValidation() {
		if (this.valid != null && this.valid) {
			this.valid = null;
		}
	}
	
	/**
	 * @return
	 * 		true if this ChainView is either invalid or doesn't contain any updates,
	 * 		false otherwise
	 */
	public boolean isRedundant() {
		return !isValid() || updates.isEmpty();
	}
	
	/**
	 * Checks the integrity of this chain view.
	 * 
	 * <p>If this method returns true, that means that the current state of the chain combined with
	 * the set of updates is consistent and that there are no gaps in the block numbers.
	 * 
	 * <p>Any overlapping parts are first checked to be consistent and are then removed.
	 * 
	 * @return
	 * 		true if this ChainView is valid, false otherwise
	 */
	private boolean checkIntegrity() {
		//No updates --> valid
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
	 * @throws IllegalStateException
	 * 		If this ChainView is invalid and the block with the given number is in the invalid part
	 * 		of this ChainView.
	 */
	public Block getBlock(int number) {
		if (number < chain.getBlocks().size()) {
			return chain.getBlocks().get(number);
		} else if (isValid()) {
			int index = number - chain.getBlocks().size();
			return updates.get(index);
		} else {
			throw new IllegalStateException(
					"This ChainView is invalid. The block with number " + number + " is not in the valid part of this ChainView.");
		}
	}
	
	@Override
	public ListIterator<Block> iterator() {
		return listIterator();
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
		private int currentIndex;
		
		ChainViewIterator() {
			chainIterator = chain.getBlocks().listIterator();
			updatesIterator = updates.listIterator();
			currentIndex = -1;
		}
		
		ChainViewIterator(int number) {
			int chainLength = chain.getBlocks().size();
			if (number < chainLength) {
				chainIterator = chain.getBlocks().listIterator(number);
				updatesIterator = updates.listIterator();
			} else {
				int index = number - chainLength;
				updatesIterator = updates.listIterator(index);
				chainIterator = chain.getBlocks().listIterator(chainLength);
				updatesReached = true;
			}
			
			currentIndex = number - 1;
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
			ListIterator<Block> it;
			if (!updatesReached) {
				if (chainIterator.hasNext()) {
					it = chainIterator;
				} else {
					updatesReached = true;
					it = updatesIterator;
				}
			} else {
				it = updatesIterator;
			}
			
			Block current = it.next();
			currentIndex = current.getNumber();
			return current;
		}
		
		@Override
		public Block previous() {
			ListIterator<Block> it;
			if (updatesReached) {
				if (updatesIterator.hasPrevious()) {
					it = updatesIterator;
				} else {
					updatesReached = false;
					it = chainIterator;
				}
			} else {
				it = chainIterator;
			}
			
			Block current = it.previous();
			currentIndex = current.getNumber() - 1;
			return current;
		}

		@Override
		public int nextIndex() {
			return currentIndex + 1;
		}

		@Override
		public int previousIndex() {
			return currentIndex;
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
