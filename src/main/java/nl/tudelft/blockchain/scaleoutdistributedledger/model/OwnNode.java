package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Class to represent our own node.
 */
public class OwnNode extends Node {
	/**
	 * Only used by the node himself.
	 * @return private key
	 */
	@Getter @Setter
	private transient byte[] privateKey;

	/**
	 * @param id - the id of our node
	 */
	public OwnNode(int id) {
		super(id);
	}

	/**
	 * @param id - the id of our node
	 * @param publicKey - the public key
	 * @param address - the address
	 * @param port - the port
	 */
	public OwnNode(int id, byte[] publicKey, String address, int port) {
		super(id, publicKey, address, port);
	}

	/**
	 * @param message - the message to sign
	 * @return - the signed message
	 * @throws Exception - See {@link Ed25519Key#sign(byte[], byte[])}
	 */
	public byte[] sign(byte[] message) throws Exception {
		return Ed25519Key.sign(message, this.privateKey);
	}
	
	@Override
	public String toString() {
		return "(OwnNode) " + super.toString();
	}
}
