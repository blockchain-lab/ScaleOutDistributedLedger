package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.logging.Level;
import javax.crypto.Cipher;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

/**
 * Class to wrap a RSA key pair + Utils to handle RSA keys.
 */
public class RSAKey {

	public static final int KEY_LENGTH = 1024;

	@Getter
	private byte[] privateKey;

	@Getter
	private byte[] publicKey;

	/**
	 * Constructor.
	 */
	public RSAKey() {
		try {
			KeyPair keyPair = generateKeys();
			this.privateKey = keyPair.getPrivate().getEncoded();
			this.publicKey = keyPair.getPublic().getEncoded();
		} catch (NoSuchAlgorithmException e) {
			Log.log(Level.SEVERE, null, e);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param privateKey - RSA private key
	 * @param publicKey  - RSA public key
	 */
	public RSAKey(byte[] privateKey, byte[] publicKey) {
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	/**
	 * Generate a random RSA key pair.
	 *
	 * @return key pair of RSA keys
	 * @throws NoSuchAlgorithmException - unsupported algorithm
	 */
	public static KeyPair generateKeys() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(KEY_LENGTH);
		return keyGen.genKeyPair();
	}

	/**
	 * Encrypt array of bytes with public key.
	 *
	 * @param message   - an array of bytes of the message
	 * @param publicKey - public RSA key
	 * @return encrypted message
	 * @throws Exception - exception while encrypting
	 */
	public static byte[] encrypt(byte[] message, byte[] publicKey) throws Exception {
		PublicKey publicKeyObject = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKeyObject);
		return cipher.doFinal(message);
	}

	/**
	 * Encrypt array of bytes with RSA key pair.
	 *
	 * @param message - an array of bytes of the message
	 * @return encrypted message
	 * @throws Exception - exception while encrypting
	 */
	public byte[] encrypt(byte[] message) throws Exception {
		return encrypt(message, this.publicKey);
	}

	/**
	 * Decrypt a message with private key.
	 *
	 * @param encryptedMessage - array of bytes of the message
	 * @param privateKey       - private RSA key
	 * @return decrypted message
	 * @throws Exception - exception while decrypting
	 */
	public static byte[] decrypt(byte[] encryptedMessage, byte[] privateKey) throws Exception {
		PrivateKey privateKeyObject = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.DECRYPT_MODE, privateKeyObject);
		return cipher.doFinal(encryptedMessage);
	}

	/**
	 * Decrypt a message with an RSA key pair.
	 *
	 * @param message - array of bytes of the message
	 * @return decrypted message
	 * @throws Exception - exception while decrypting
	 */
	public byte[] decrypt(byte[] message) throws Exception {
		return decrypt(message, this.privateKey);
	}

	/**
	 * Verify an array of bytes with signature and public key.
	 *
	 * @param message   - array of bytes of the message
	 * @param signature - signature of the message
	 * @param publicKey - public RSA key
	 * @return whether is correct or not
	 * @throws SignatureException - exception while verifying
	 */
	public static boolean verify(byte[] message, byte[] signature, byte[] publicKey) throws SignatureException {
		try {
			PublicKey publicKeyObject = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
			Signature publicSignature = Signature.getInstance("SHA256withRSA");
			publicSignature.initVerify(publicKeyObject);
			publicSignature.update(message);
			return publicSignature.verify(signature);
		} catch (SecurityException | NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
			//TODO: implement a custom exception here (or remove this TODO if not needed).
			throw new SignatureException(e);
		}
	}

	/**
	 * Verify an array of bytes with signature and public key.
	 *
	 * @param message   - array of bytes of the message
	 * @param signature - signature of the message
	 * @return whether is correct or not
	 * @throws Exception - exception while verifying
	 */
	public boolean verify(byte[] message, byte[] signature) throws Exception {
		return verify(message, signature, this.publicKey);
	}

	/**
	 * Sign an array of bytes with a private key.
	 *
	 * @param message    - array of bytes of the message
	 * @param privateKey - private RSA key
	 * @return signature of the message
	 * @throws java.lang.Exception - exception while signing
	 */
	public static byte[] sign(byte[] message, byte[] privateKey) throws Exception {
		PrivateKey privateKeyObject = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
		Signature privateSignature = Signature.getInstance("SHA256withRSA");
		privateSignature.initSign(privateKeyObject);
		privateSignature.update(message);
		return privateSignature.sign();
	}

	/**
	 * Sign an array of bytes with a private key.
	 *
	 * @param message - array of bytes of the message
	 * @return signature of the message
	 * @throws java.lang.Exception - exception while signing
	 */
	public byte[] sign(byte[] message) throws Exception {
		return sign(message, this.privateKey);
	}

	@Override
	public boolean equals(Object other) {
		if (other == this) return true;
		if (!(other instanceof RSAKey)) return false;
		return Arrays.equals(this.publicKey, ((RSAKey) other).publicKey)
				&& Arrays.equals(this.privateKey, ((RSAKey) other).privateKey);
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + Arrays.hashCode(this.privateKey);
		hash = 37 * hash + Arrays.hashCode(this.publicKey);
		return hash;
	}

	@Override
	public String toString() {
		StringBuilder stringBuffer = new StringBuilder(32 + this.publicKey.length * 2 + this.privateKey.length * 2);
		stringBuffer.append("Public Key: \n")
				.append(keyToString(this.publicKey))
				.append("\nPrivate Key: \n")
				.append(keyToString(this.privateKey));
		return stringBuffer.toString();
	}

	/**
	 * Convert a key into a string.
	 *
	 * @param keyBytes - an array of bytes of the key
	 * @return string - hexadecimal representation of the key
	 */
	public static String keyToString(byte[] keyBytes) {
		return Utils.bytesToHexString(keyBytes);
	}

}
