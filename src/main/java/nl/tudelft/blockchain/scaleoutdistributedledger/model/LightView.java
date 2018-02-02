package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which provides an easy way of getting blocks.
 */
public class LightView  {
	private Chain chain;
	private List<Block> updates;
	
	/**
	 * @param chain   - the chain
	 * @param updates - the blocks of this chain that were sent with the proof
	 */
	public LightView(Chain chain, List<Block> updates) {
		this.chain = chain;
		this.updates = updates;
		if (this.updates == null) this.updates = new ArrayList<>();
	}
	
	/**
	 * @param number - the number
	 * @return - the block with the given number
	 * @throws IndexOutOfBoundsException - If the block with the given number does not exist (yet).
	 */
	public Block getBlock(int number) {
		if (number < chain.getBlocks().size()) {
			return chain.getBlocks().get(number);
		} else {
			int index = number - updates.get(0).getNumber();
			return updates.get(index);
		}
	}
}
