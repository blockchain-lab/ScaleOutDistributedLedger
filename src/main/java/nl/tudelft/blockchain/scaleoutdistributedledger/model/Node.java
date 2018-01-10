package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;

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

	@Getter
	private final MainChain mainChain;

	@Getter @Setter
	private byte[] publicKey;

	/**
	 * Only used by the node himself.
	 * @return private key
	 */
	@Getter @Setter
	private transient byte[] privateKey;
	
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
		mainChain = new TendermintChain();
	}

	/**
	 * Constructor.
	 * @param id - the id of this node.
	 * @param publicKey - the public key of this node.
	 * @param address - the address of this node.
	 * @param port = the port of this node. Note, also port+1, port+2 and port+3 are used (for tendermint: p2p.laddr, rpc.laddr, ABCI server).
	 */
	public Node(int id, byte[] publicKey, String address, int port) {
		this.id = id;
		this.publicKey = publicKey;
		this.address = address;
		this.port = port;
		this.chain = new Chain(this);
		this.mainChain = new TendermintChain(port+3);
	}

	/**
	 * @param message - the message to sign
	 * @return - the signed message
	 * @throws Exception - See {@link RSAKey#sign(byte[], byte[])}
	 */
	public byte[] sign(byte[] message) throws Exception {
		return RSAKey.sign(message, this.privateKey);
	}
	
	/**
	 * Verify the signature of a message made by this node.
	 * @param message - message to be verified
	 * @param signature - signature of the message
	 * @return - the signature
	 * @throws Exception - See {@link RSAKey#verify(byte[], byte[], byte[])}
	 */
	public boolean verify(byte[] message, byte[] signature) throws Exception {
		return RSAKey.verify(message, signature, this.publicKey);
	}
	
	/**
	 * Updates the knowledge that we have about what this node knows with the information in the
	 * given proof.
	 * @param proof - the proof to update with
	 */
	public void updateMetaKnowledge(Proof proof) {
		Map<Node, List<Block>> updates = proof.getChainUpdates();
		for (Entry<Node, List<Block>> entry : updates.entrySet()) {
			int lastBlockNr = getLastBlockNumber(entry.getValue());
			metaKnowledge.merge(entry.getKey(), lastBlockNr, Math::max);
		}
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
}
