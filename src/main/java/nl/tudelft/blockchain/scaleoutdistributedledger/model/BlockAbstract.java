package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Optional;
import java.util.logging.Level;

/**
 * BlockAbstract class.
 */
public class BlockAbstract implements Serializable {

	private static final long serialVersionUID = 1L;

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

	/**
	 * Convert this abstract to a byte array.
	 * Performs the inverse of {@link BlockAbstract#fromBytes(byte[])}.
	 *
	 * @return - the byte array conversion; or null if serialization fails.
	 */
	public byte[] toBytes() {
		byte[] ret;
		try {
			ret = SerializationUtils.serialize(this);
		} catch (SerializationException e) {
			Log.log(Level.WARNING, "Could not serialize the BlockAbstract to bytes", e);
			ret = null;
		}
		return ret;
    }

	/**
	 * Construct a {@link BlockAbstract} from a byte array.
	 * Performs the inverse of {@link BlockAbstract#toBytes()}.
	 *
	 * @param bytes - the data to construct from
	 * @return - the abstract represented by the bytes; null if the deserialization fails.
	 */
	public static BlockAbstract fromBytes(byte[] bytes) {
		BlockAbstract block;
		try {
			block = SerializationUtils.deserialize(bytes);
		} catch (SerializationException e) {
			Log.log(Level.WARNING, "Could not deserialize BlockAbstract from bytes", e);
			block = null;
		}
		return block;
    }

	/**
	 * Returns the boolean onMainChain, and gets it if it is not present.
	 * @return - boolean identifiying if this abstract is on the main chain.
	 */
	public boolean isOnMainChain() {
		if (!this.onMainChain.isPresent()) {
			this.onMainChain = Optional.of(Application.getMainChain().isPresent(this));
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
	    	//TODO: we are potentially swallowing a huge stack of exceptions here; this should really only be catching relevant exceptions (e.g. signature exception)
	        return false;
        }
	}
}
