package nl.tudelft.blockchain.scaleoutdistributedledger.model;


import lombok.Getter;
import lombok.Setter;

/**
 * Created by Bart on 05/12/2017.
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

    public Node(int id) {
        this.id = id;
        this.chain = new Chain(this);
    }

    public Node(int id, byte[] publicKey, String address) {
        this.id = id;
        this.publicKey = publicKey;
        this.address = address;
        this.chain = new Chain(this);
    }
}
