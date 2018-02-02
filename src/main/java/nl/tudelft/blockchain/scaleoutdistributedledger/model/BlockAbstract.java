package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
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
import java.util.Arrays;
import java.util.Objects;
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
			byte[] attrInBytes = BlockAbstract.calculateBytesForSignature(this.ownerNodeId, this.blockNumber, this.blockHash);

			return Ed25519Key.verify(attrInBytes, this.signature, signatureKey);
		} catch (SignatureException e) {
			return false;
		}
	}

	/**
	 * Convert attributes of abstract into an array of bytes, for the signature.
	 * Important to keep the order of writings.
	 * @param ownerId - id of the owner
	 * @param blockNumber - number of the block
	 * @param hash - hash of the block
	 * @return array of bytes
	 */
	public static byte[] calculateBytesForSignature(int ownerId, int blockNumber, Sha256Hash hash) {
		byte[] attrInBytes;
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			outputStream.write(Utils.intToByteArray(ownerId));
			outputStream.write(Utils.intToByteArray(blockNumber));
			outputStream.write(hash.getBytes());
			attrInBytes = outputStream.toByteArray();
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to write to outputstream", ex);
		}
		
		return attrInBytes;
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
	}

	private void readObject(ObjectInputStream stream) throws IOException,
			ClassNotFoundException {
		stream.defaultReadObject();
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 47 * hash + this.ownerNodeId;
		hash = 47 * hash + this.blockNumber;
		hash = 47 * hash + Objects.hashCode(this.blockHash);
		hash = 47 * hash + Arrays.hashCode(this.signature);
		hash = 47 * hash + Objects.hashCode(this.abstractHash);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof BlockAbstract)) return false;
		
		BlockAbstract other = (BlockAbstract) obj;
		if (ownerNodeId != other.ownerNodeId) return false;
		if (blockNumber != other.blockNumber) return false;
		if (!blockHash.equals(other.blockHash)) return false;
		if (!Arrays.equals(signature, other.signature)) return false;
		if (this.abstractHash == null) {
			if (other.abstractHash != null) return false;
		} else if (other.abstractHash == null || this.abstractHash.equals(other.abstractHash)) return false;
		
		return true;
	}
	
}
