package nl.tudelft.blockchain.scaleoutdistributedledger.model;

/**
 * Class to represent the genesis node.
 */
public class GenesisNode extends Node {
	public static final int GENESIS_NODE_ID = -1;
	
	/**
	 * Creates a new genesis node.
	 */
	public GenesisNode() {
		super(GENESIS_NODE_ID);
	}
	
	@Override
	public final boolean isGenesis() {
		return true;
	}
	
	@Override
	public String toString() {
		return "GENESIS";
	}
}
