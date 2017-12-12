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
	 * Only used by the node himself
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;

        return id == node.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
	
	/**
	 * Sign the signature of a message
	 * @param message - message to be signed
	 * @return check it the signature is correct
	 * @throws java.lang.Exception
	 */
	public byte[] sign(byte[] message) throws Exception {
		return RSAKey.sign(message, this.privateKey);
	}
	
	/**
	 * Verify the signature of a message made by this node
	 * @param message - message to be verified
	 * @param signature - signature of the message
	 * @return check it the signature is correct
	 * @throws java.lang.Exception
	 */
	public boolean verify(byte[] message, byte[] signature) throws Exception {
		return RSAKey.verify(message, signature, this.publicKey);
	}
	
}
