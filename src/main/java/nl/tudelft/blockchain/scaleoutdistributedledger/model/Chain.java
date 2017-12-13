package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

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
	 * Constructor.
	 * @param owner - the owner of this chain.
	 */
	public Chain(Node owner) {
		this.owner = owner;
		this.blocks = new ArrayList<>();
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
}
