package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import static org.junit.Assert.*;

import org.junit.Test;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.junit.Before;

/**
 * Test class for {@link Ed25519Key}.
 */
public class Ed25519KeyTest {
	
	private Ed25519Key validKey;
	
	private Ed25519Key invalidKey;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		byte[] validPub = Utils.hexStringToBytes("BE8933DFF1600C026E34718F1785A4CDEAB90C35698B394E38B6947AE91DE116");
		byte[] validPriv = Utils.hexStringToBytes("547AA07C7A8CE16C5CB2A40C6C26D15B0A32960410A9F1EA6E50B636F1AB389"
				+ "ABE8933DFF1600C026E34718F1785A4CDEAB90C35698B394E38B6947AE91DE116");
		this.validKey = new Ed25519Key(validPriv, validPub);
		
		byte[] invalidPriv = Utils.hexStringToBytes("0000A07C7A8CE16C5CB2A40C6C26D15B0A32960410A9F1EA6E50B636F1AB389"
				+ "ABE8933DFF1600C026E34718F1785A4CDEAB90C35698B394E38B6947AE91DE116");
		this.invalidKey = new Ed25519Key(invalidPriv, validPub);
	}
	
	/**
	 * Test for {@link Ed25519Key#sign(byte[], byte[])}.
	 */
	@Test
	public void testSign_Valid() {
		byte[] message = "testing message".getBytes();
		
		try {
			byte[] signature = Ed25519Key.sign(message, this.validKey.getPrivateKey());
			boolean valid = Ed25519Key.verify(message, signature, this.validKey.getPublicKey());
			
			assertTrue(valid);
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
			fail();
		}
	}
	
	/**
	 * Test for {@link Ed25519Key#sign(byte[], byte[])}.
	 */
	@Test
	public void testSign_Invalid() {
		byte[] message = "testing message".getBytes();
		
		try {
			byte[] signature = Ed25519Key.sign(message, this.invalidKey.getPrivateKey());
			boolean valid = Ed25519Key.verify(message, signature, this.invalidKey.getPublicKey());
			
			assertFalse(valid);
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
			fail();
		}
	}

	/**
	 * Test for {@link Ed25519Key#generateKeys()}.
	 */
	@Test
	public void testGenerateKeys_Valid() {
		Ed25519Key keyPair = new Ed25519Key();
		byte[] pub = keyPair.getPublicKey();
		byte[] priv = keyPair.getPrivateKey();
		byte[] message = "testing message".getBytes();

		try {
			byte[] signature = Ed25519Key.sign(message, priv);
			boolean valid = Ed25519Key.verify(message, signature, pub);

			assertTrue(valid);
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
			fail();
		}
	}
	
	/**
	 * Test for {@link Ed25519Key#sign()} and {@link Ed25519Key#verify()}.
	 */
	@Test
	public void testSignVerify_Valid() {
		Ed25519Key keyPair = new Ed25519Key();
		byte[] message = "testing message".getBytes();

		try {
			byte[] signature = keyPair.sign(message);
			boolean valid = keyPair.verify(message, signature);

			assertTrue(valid);
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException ex) {
			fail();
		}
	}
	
	/**
	 * Test for {@link Ed25519Key#verify(byte[], byte[], byte[]) ()}.
	 */
	@Test
	public void testVerify_Invalid() {
		byte[] message = "testing message".getBytes();
		byte[] signature = new byte[10];

		try {
			Ed25519Key.verify(message, signature, this.validKey.getPublicKey());

			fail();
		} catch (SignatureException ex) {
			// Good
		}
	}
	
	/**
	 * Test for {@link Ed25519Key#equals()}.
	 */
	@Test
	public void testEquals_Valid() {
		Ed25519Key keyPair = new Ed25519Key();
		Ed25519Key newKeyPair = new Ed25519Key(keyPair.getPrivateKey(), keyPair.getPublicKey());
		
		assertEquals(keyPair, newKeyPair);
	}
	
	/**
	 * Test for {@link Ed25519Key#hashCode() ()}.
	 */
	@Test
	public void testHashCode() {
		assertNotEquals(this.validKey.hashCode(), this.invalidKey.hashCode());
	}
	
}
