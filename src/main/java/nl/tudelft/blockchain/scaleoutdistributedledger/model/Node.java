package nl.tudelft.blockchain.scaleoutdistributedledger.model;


import lombok.Getter;
import lombok.Setter;

/**
 * Node class.
 */
public class Node {

    @Getter
    final int id;

    @Getter
    final Chain chain;

    @Getter @Setter
    byte[] publicKey;

    @Getter @Setter
    String address;

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
}
