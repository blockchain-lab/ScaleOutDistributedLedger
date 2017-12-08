package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import com.sun.javafx.scene.control.behavior.OptionalBoolean;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

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

    @Setter
    private Optional<Boolean> onMainChain; // any means unknown

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
        this.onMainChain = Optional.empty();
    }

    /**
     * Returns the boolean onMainChain, and gets it if it is not present.
     * @return - boolean identifiying if this abstract is on the main chain.
     */
    public boolean isOnMainChain() {
        if (!this.onMainChain.isPresent()) {
            // TODO: check with tendermint if this is on the main chain.
            this.onMainChain = Optional.of(false);
        }
        return this.onMainChain.get();
    }

    /**
     * Checks if the given blocks corresponds with the blockHash in this abstract.
     * @param block - the block to check
     * @return - boolean identifying if the blockhash was correct or not.
     */
    public boolean checkBlockHash(Block block) {
        // TODO: check if the hash of the given block corresponds with the blockHash in this abstract.
        return true;
    }

    /**
     * Checks if the signature included in this abstract is valid.
     * @return - boolean identifying if the signature is valid.
     */
    public boolean checkSignature() {
        // TODO: check if the signature is valid
        return true;
    }
}
