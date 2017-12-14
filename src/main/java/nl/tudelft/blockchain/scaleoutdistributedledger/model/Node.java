package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;

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

	/**
	 * Only used by the node himself.
	 * @return private key
	 */
	@Getter @Setter
	private transient byte[] privateKey;
	
	@Getter @Setter
	private String address;

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
	 */
	public Node(int id, byte[] publicKey, String address) {
		this.id = id;
		this.publicKey = publicKey;
		this.address = address;
		this.chain = new Chain(this);
	}

	/**
	 * @param message - the message to sign
	 * @return - the signed message
	 * @throws Exception - See {@link RSAKey#sign(byte[], byte[])}
	 */
	public byte[] sign(byte[] message) throws Exception {
		return RSAKey.sign(message, this.privateKey);
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
	
}
