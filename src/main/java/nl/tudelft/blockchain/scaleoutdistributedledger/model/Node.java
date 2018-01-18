package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Node class.
 */
public class Node {

	@Getter
	private final int id;

	@Getter
	private final Chain chain;

	@Getter @Setter
	private byte[] publicKey;
	
	@Getter @Setter
	private String address;

	@Getter @Setter
	private int port;
	
	/**
	 * @return a map containing what we know that this node knows
	 */
	@Getter
	private Map<Node, Integer> metaKnowledge = new HashMap<>();

	/**
	 * Constructor.
	 * @param id - the id of this node.
	 */
	public Node(int id) {
		this.id = id;
		this.chain = new Chain(this);
	}

	/**
	 * Constructor.
	 * @param id - the id of this node.
	 * @param publicKey - the public key of this node.
	 * @param address - the address of this node.
	 * @param port = the port of this node.
	 */
	public Node(int id, byte[] publicKey, String address, int port) {
		this.id = id;
		this.publicKey = publicKey;
		this.address = address;
		this.port = port;
		this.chain = new Chain(this);
	}

	/**
	 * Add a genesis block to the chain.
	 * @param block - the genesis block
	 */
	public void setGenesisBlock(Block block) {
		synchronized (this.chain) {
			if (!this.chain.getBlocks().isEmpty()) {
				throw new IllegalStateException("Adding genesis block to non-empty chain");
			}
			this.chain.getBlocks().add(block);
			this.chain.setLastCommittedBlock(block);
		}
	}
	
	/**
	 * Verify the signature of a message made by this node.
	 * @param message - message to be verified
	 * @param signature - signature of the message
	 * @return - the signature
	 * @throws Exception - See {@link Ed25519Key#verify(byte[], byte[], byte[])}
	 */
	public boolean verify(byte[] message, byte[] signature) throws Exception {
		return Ed25519Key.verify(message, signature, this.publicKey);
	}
	
	/**
	 * Updates the knowledge that we have about what this node knows with the information in the
	 * given proof.
	 * @param proof - the proof to update with
	 */
	public void updateMetaKnowledge(Proof proof) {
//		Map<Node, List<Block>> updates = proof.getChainUpdates();
//		for (Entry<Node, List<Block>> entry : updates.entrySet()) {
//			int lastBlockNr = getLastBlockNumber(entry.getValue());
//			metaKnowledge.merge(entry.getKey(), lastBlockNr, Math::max);
//		}
	}
	
	/**
	 * @param blocks - the list of blocks
	 * @return         the number of the last block in the given list
	 */
	private static int getLastBlockNumber(List<Block> blocks) {
		if (blocks.isEmpty()) return -1;
		
		Block lastBlock = blocks.get(blocks.size() - 1);
		return lastBlock.getNumber();
	}
	
	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		//We only have one Node object for each id, so we can compare with ==
		return obj == this;
	}
	
	@Override
	public String toString() {
		return this.id + " at " + this.address + ":" + this.port;
	}
}
