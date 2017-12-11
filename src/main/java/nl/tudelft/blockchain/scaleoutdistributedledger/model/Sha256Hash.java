package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.logging.Level;
import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Class to wrap a SHA256 hash
 */
public class Sha256Hash {

	@Getter
	private byte[] bytes;
	
	/**
	 * Constructor
	 * @param bytesAux - the array of bytes to be hashed
	 */
	public Sha256Hash(byte[] bytesAux) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			this.bytes = digest.digest(bytesAux);
		} catch (NoSuchAlgorithmException ex) {
			Log.log(Level.SEVERE, null, ex);
		}
	}
	
	/**
	 * Constructor
	 * @param message - the string to be hashed
	 */
	public Sha256Hash(String message) {
		byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			this.bytes = digest.digest(messageBytes);
		} catch (NoSuchAlgorithmException ex) {
			Log.log(Level.SEVERE, null, ex);
		}
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.bytes);
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (!(other instanceof Sha256Hash)) return false;
		
		return Arrays.equals(this.bytes, ((Sha256Hash) other).bytes);
	}
	
	@Override
	public String toString() {
		return Utils.bytesToHexString(this.bytes);
	}
	
}
