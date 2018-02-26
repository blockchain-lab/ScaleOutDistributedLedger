package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

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
	
	private boolean[] disallowedChains;

	/**
	 * @param id - the id of our node
	 */
	public OwnNode(int id) {
		super(id);
		fillDisallowedChains();
	}

	/**
	 * @param id - the id of our node
	 * @param publicKey - the public key
	 * @param address - the address
	 * @param port - the port
	 */
	public OwnNode(int id, byte[] publicKey, String address, int port) {
		super(id, publicKey, address, port);
		fillDisallowedChains();
	}

	/**
	 * @param message - the message to sign
	 * @return - the signed message
	 * @throws Exception - See {@link Ed25519Key#sign(byte[], byte[])}
	 */
	public byte[] sign(byte[] message) throws Exception {
		return Ed25519Key.sign(message, this.privateKey);
	}
	
	private void fillDisallowedChains() {
		int total = Settings.INSTANCE.totalNodesNumber;
		disallowedChains = new boolean[total];
		int g = Settings.INSTANCE.getTransactionPattern().getNodeSelector().getLimit();
		
		outer:
			for (int i = 0; i < disallowedChains.length; i++) {
				for (int j = i; j < i + g; j++) {
					if (j % total == this.id) {
						disallowedChains[i] = false;
						continue outer;
					}
				}
				
				disallowedChains[i] = true;
			}
	}
	
	/**
	 * @param disallowedChains - an array of booleans, true is not allowed to send to us
	 */
	public void setDisallowedChains(boolean[] disallowedChains) {
		this.disallowedChains = disallowedChains;
	}
	
	/**
	 * @param nodeId - the node id
	 * @return - true if the given nodeId is in the disallowed chains set
	 */
	public boolean isDisallowedChain(int nodeId) {
		return this.disallowedChains[nodeId];
	}
	
	@Override
	public String toString() {
		return "(OwnNode) " + super.toString();
	}
	
}
