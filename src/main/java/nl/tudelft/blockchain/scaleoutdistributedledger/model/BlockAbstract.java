package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import com.google.protobuf.ByteString;
import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import java.io.ByteArrayOutputStream;
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
	private final Sha256Hash blockHash;

	@Getter
	private final byte[] signature;

	@Setter
	private Optional<Boolean> onMainChain; // any means unknown

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
        this.onMainChain = Optional.empty();
    }

    public byte[] toBytes() {
        //TODO: implement this method
        return new byte[0];
    }

    public static BlockAbstract fromBytes(byte[] tx) {
        //TODO: implement this method
        BlockAbstract block = new BlockAbstract(null, 0, null, new byte[0]);
        block.setOnMainChain(Optional.of(true));
		return block;
    }

	/**
	 * Returns the boolean onMainChain, and gets it if it is not present.
	 * @return - boolean identifiying if this abstract is on the main chain.
	 */
	public boolean isOnMainChain() {
		if (!this.onMainChain.isPresent()) {
			// TODO: check with tendermint if this is on the main chain.
			// this.onMainChain = Optional.of(TendermintChain.query(this));
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
		return this.blockHash.equals(block.getHash());
	}

	/**
	 * Checks if the signature included in this abstract is valid.
	 * @return - boolean identifying if the signature is valid.
	 */
	public boolean checkSignature() {
	    try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            outputStream.write(Utils.intToByteArray(this.owner.getId()));
            outputStream.write(Utils.intToByteArray(this.blockNumber));
            outputStream.write(this.blockHash.getBytes());
            byte[] attrInBytes = outputStream.toByteArray();

            return RSAKey.verify(attrInBytes, this.signature, this.owner.getPublicKey());
        } catch (Exception e) {
	        return false;
        }
	}
}
