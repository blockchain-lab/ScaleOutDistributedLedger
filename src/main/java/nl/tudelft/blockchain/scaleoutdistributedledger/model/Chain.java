package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Chain class.
 */
public class Chain {

	@Getter
	private final Node owner;

	@Getter
	private final List<Block> blocks;
	
	/**
	 * TODO The last committed Block has to be set somewhere.
	 * @return the last block that was committed to the main chain
	 */
	@Getter @Setter
	private Block lastCommittedBlock;

	/**
	 * Constructor.
	 * @param owner - the owner of this chain.
	 */
	public Chain(Node owner) {
		this.owner = owner;
		this.blocks = new ArrayList<>();
	}

	/**
	 * Add a genesis block to the chain.
	 * @param block - the genesis block
	 */
	public void setGenesisBlock(Block block) {
		if (!this.blocks.isEmpty()) {
			throw new IllegalStateException("Adding genesis block to non-empty chain");
		}
		this.blocks.add(block);
		this.lastCommittedBlock = block;
	}

	/**
	 * Constructor.
	 * @param owner - the owner of this chain.
	 * @param blocks - list of blocks in this chain.
	 */
	public Chain(Node owner, List<Block> blocks) {
		this.owner = owner;
		this.blocks = blocks;
	}
	
	/**
	 * Updates this chain with the given updates.
	 * @param updates - the new blocks to append
	 */
	public void update(List<Block> updates) {
		if (updates.isEmpty()) return;
		
		if (blocks.isEmpty()) {
			blocks.addAll(updates);
		} else {
			Block lastBlock = blocks.get(blocks.size() - 1);
			int nextNr = lastBlock.getNumber() + 1;
			for (Block block : updates) {
				if (block.getNumber() != nextNr) continue;
				blocks.add(block);
				nextNr++;
			}
		}
	}
	
	/**
	 * @return the last block in this chain
	 */
	public Block getLastBlock() {
		if (blocks.isEmpty()) return null;

		return blocks.get(blocks.size() - 1);
	}
}
