package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import com.google.protobuf.ByteString;
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
    private final byte[] blockHash, signature;

    /**
     * Constructor.
     * @param owner - the owner of the block this abstract is for.
     * @param blockNumber - the number of the block this abstract is for.
     * @param blockHash - the hash of the block this abstract is for.
     * @param signature - the signature for the block by the owner.
     */
    public BlockAbstract(Node owner, int blockNumber, byte[] blockHash, byte[] signature) {
        this.owner = owner;
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.signature = signature;
    }

    public byte[] toBytes() {
        //TODO: implement this method
        return new byte[0];
    }

    public static BlockAbstract fromBytes(byte[] tx) {
        //TODO: implement this method
        return new BlockAbstract(null, 0, new byte[0], new byte[0]);
    }
}
