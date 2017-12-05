package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

/**
 * Created by Bart on 05/12/2017.
 */
public class Abstract {

    @Getter
    final Node owner;

    @Getter
    final int blockNumber;

    @Getter
    final byte[] blockHash, signature;

    public Abstract(Node owner, int blockNumber, byte[] blockHash, byte[] signature) {
        this.owner = owner;
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.signature = signature;
    }
}
