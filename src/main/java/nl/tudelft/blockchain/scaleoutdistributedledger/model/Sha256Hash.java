package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;

/**
 * Class to wrap a sha256 hash
 */
public class Sha256Hash {

	@Getter
	private byte[] bytes;
	
	public Sha256Hash(byte[] bytesAux) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			this.bytes = digest.digest(bytesAux);
		} catch (NoSuchAlgorithmException ex) {
			this.bytes = null;
			Logger.getLogger(Sha256Hash.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	public Sha256Hash(String message) {
		byte[] messageBytes = Base64.getDecoder().decode(message);
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			this.bytes = digest.digest(messageBytes);
		} catch (NoSuchAlgorithmException ex) {
			this.bytes = null;
			Logger.getLogger(Sha256Hash.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(this.bytes);
	}
	
	@Override
    public boolean equals(Object other) {
		if (!(other instanceof Sha256Hash)) {
			return false;
		}
		return Arrays.equals(this.bytes, ((Sha256Hash) other).bytes);
    }
	
	@Override
	public String toString() {
		return Utils.bytesToHexString(this.bytes);
	}
	
}
