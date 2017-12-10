package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;

/**
 * BlockAbstract class.
 */
public class BlockAbstract {

    @Getter
    private final Node owner;

    @Getter
    private final int blockNumber;

	@Getter
	private final Sha256Hash blockHash;

	@Getter
	private final byte[] signature;

    /**
     * Constructor.
     * @param owner - the owner of the block this abstract is for.
     * @param blockNumber - the number of the block this abstract is for.
     * @param blockHash - the hash of the block this abstract is for.
     * @param signature - the signature for the block by the owner.
     */
    public BlockAbstract(Node owner, int blockNumber, Sha256Hash blockHash, byte[] signature) {
        this.owner = owner;
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.signature = signature;
    }

}
