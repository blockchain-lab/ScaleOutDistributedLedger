package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

/**
 * Abstract class.
 */
public class Abstract {

    @Getter
    private final Node owner;

    @Getter
    private final int blockNumber;

    @Getter
    private final byte[] blockHash, signature;

    /**
     * Constructor.
     * @param owner - the owner of the block this abstract is for.
     * @param blockNumber - the number of the block this abstract is for.
     * @param blockHash - the hash of the block this abstract is for.
     * @param signature - the signature for the block by the owner.
     */
    public Abstract(Node owner, int blockNumber, byte[] blockHash, byte[] signature) {
        this.owner = owner;
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.signature = signature;
    }
}
