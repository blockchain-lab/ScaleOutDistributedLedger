package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import lombok.Getter;

/**
 * Class to wrap a RSA key pair + Utils to handle RSA keys
 */
public class RSAKey {
	
	public static int KEY_LENGTH = 1024;
	
	@Getter
	private byte[] privateKey;
	
	@Getter
	private byte[] publicKey;
	
	public RSAKey() {
		try {
			KeyPair keyPair = generateKeys();
			this.privateKey = keyPair.getPrivate().getEncoded();
			this.publicKey = keyPair.getPublic().getEncoded();
		} catch (NoSuchAlgorithmException ex) {
			Logger.getLogger(RSAKey.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
	
	/**
	 * Generate a random RSA key pair
	 * @return key pair of RSA keys
	 * @throws java.security.NoSuchAlgorithmException 
	 */
	public static KeyPair generateKeys() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(KEY_LENGTH);
		return keyGen.genKeyPair();
	}
	
	/**
	 * Encrypt array of bytes with public key
	 * @param message
	 * @param publicKey
	 * @return encrypted message
	 * @throws Exception 
	 */
	public static byte[] encrypt(byte[] message, byte[] publicKey) throws Exception {
		PublicKey publicKeyObject = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, publicKeyObject);
		return cipher.doFinal(message);
	}
	
	public byte[] encrypt(byte[] message) throws Exception {
		return encrypt(message, this.publicKey);
	}
	
	/**
	 * Decrypt array of byte with private key
	 * @param encryptedMessage
	 * @param privateKey
	 * @return decrypted message
	 * @throws Exception 
	 */
	public static byte[] decrypt(byte[] encryptedMessage, byte[] privateKey) throws Exception {
		PrivateKey privateKeyObject = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");  
		cipher.init(Cipher.DECRYPT_MODE, privateKeyObject);
		return cipher.doFinal(encryptedMessage);
	}
	
	public byte[] decrypt(byte[] message) throws Exception {
		return decrypt(message, this.privateKey);
	}
	
	/**
	 * Verify an array of bytes with signature and public key
	 * @param message
	 * @param signature
	 * @param publicKey
	 * @return whether is correct or not
	 * @throws Exception 
	 */
	public static boolean verify(byte[] message, byte[] signature, byte[] publicKey) throws Exception {
		PublicKey publicKeyObject = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey));
		Signature publicSignature = Signature.getInstance("SHA256withRSA");
		publicSignature.initVerify(publicKeyObject);
		publicSignature.update(message);
		return publicSignature.verify(signature);
    }
	
	public boolean verify(byte[] message, byte[] signature) throws Exception {
		return verify(message, signature, this.publicKey);
	}
	
	/**
	 * Sign an array of bytes with a private key
	 * @param message
	 * @param privateKey
	 * @return signature of the message
	 * @throws java.lang.Exception
	 */
	public static byte[] sign(byte[] message, byte[] privateKey) throws Exception {
		PrivateKey privateKeyObject = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(privateKey));
		Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKeyObject);
        privateSignature.update(message);
        return privateSignature.sign();
	}
	
	public byte[] sign(byte[] message) throws Exception {
		return sign(message, this.privateKey);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof RSAKey)) {
			return false;
		}
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
		StringBuilder stringBuffer = new StringBuilder();
		stringBuffer.append("Public Key: \n")
			.append(keyToString(this.publicKey))
			.append("\n")
			.append("Private Key: \n")
			.append(keyToString(this.privateKey));
		return stringBuffer.toString();
	}
	
	/**
	 * Convert a key into a string
	 * @param keyBytes
	 * @return string - hexadecimal representation of the key
	 */
	public static String keyToString(byte[] keyBytes) {
		StringBuilder stringBuffer = new StringBuilder();
		for (int i = 0; i < keyBytes.length; i++) {
			stringBuffer.append(Integer.toHexString(0x0100 + (keyBytes[i] & 0x00FF)).substring(1));
		}
		return stringBuffer.toString();
	} 
	
}
