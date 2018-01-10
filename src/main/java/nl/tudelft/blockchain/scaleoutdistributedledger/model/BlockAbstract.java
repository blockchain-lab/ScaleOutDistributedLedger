package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.ByteArrayOutputStream;
import java.security.SignatureException;
import java.util.Optional;
import java.util.logging.Level;

/**
 * BlockAbstract class.
 */
public class BlockAbstract implements Serializable {

	private static final long serialVersionUID = 1L;

	@Getter
	private int ownerNodeId;

	@Getter
	private int blockNumber;

	@Getter
	private Sha256Hash blockHash;

	@Getter
	private byte[] signature;

	@Setter
	private transient Optional<Boolean> onMainChain; // any means unknown

	@Setter	@Getter
	private Sha256Hash abstractHash;

	/**
	 * Constructor.
	 *
	 * @param ownerNodeId - the id of the owner of the block this abstract is for.
	 * @param blockNumber - the number of the block this abstract is for.
	 * @param blockHash   - the hash of the block this abstract is for.
	 * @param signature   - the signature for the block by the owner.
	 */
	public BlockAbstract(int ownerNodeId, int blockNumber, Sha256Hash blockHash, byte[] signature) {
		this.ownerNodeId = ownerNodeId;
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
		} catch (SerializationException | ClassCastException e) {
			Log.log(Level.WARNING, "Could not deserialize BlockAbstract from bytes", e);
			block = null;
		}
		return block;
	}

	/**
	 * Returns the boolean onMainChain, and gets it if it is not present.
	 *
	 * @return - boolean identifying if this abstract is on the main chain.
	 */
	public boolean isOnMainChain() {
		if (!this.onMainChain.isPresent()) {
			//TODO: this should check on the main chain, but there's no static way for now.
			this.onMainChain = Optional.of(true);
//			this.onMainChain = Optional.of(Application.getMainChain().isPresent(this.blockHash));
		}
		return this.onMainChain.get();
	}

	/**
	 * Checks if the given blocks corresponds with the blockHash in this abstract.
	 *
	 * @param block - the block to check
	 * @return - boolean identifying if the blockhash was correct or not.
	 */
	public boolean checkBlockHash(Block block) {
		return this.blockHash.equals(block.getHash());
	}

	/**
	 * Checks if the signature included in this abstract is valid.
	 *
	 * @param signatureKey - the key that we want to check signature with (public key of owner)
	 * @return - boolean identifying if the signature is valid.
	 */
	public boolean checkSignature(byte[] signatureKey) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(Utils.intToByteArray(this.ownerNodeId));
			outputStream.write(Utils.intToByteArray(this.blockNumber));
			outputStream.write(this.blockHash.getBytes());
			byte[] attrInBytes = outputStream.toByteArray();

			return RSAKey.verify(attrInBytes, this.signature, signatureKey);
		} catch (IOException | SignatureException e) {
			return false;
		}
	}

	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
		this.onMainChain = Optional.empty();
	}

}
