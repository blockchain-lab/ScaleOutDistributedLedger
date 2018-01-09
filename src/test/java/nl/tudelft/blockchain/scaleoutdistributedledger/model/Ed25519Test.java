package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Test class for {@link ED25119}.
 */
public class Ed25519Test {
	
	/**
	 * Test for {@link ED25519#sign()}.
	 */
	@Test
	public void testSign_Valid() {
		byte[] pub = Utils.hexStringToBytes("BE8933DFF1600C026E34718F1785A4CDEAB90C35698B394E38B6947AE91DE116");
		byte[] priv = Utils.hexStringToBytes("547AA07C7A8CE16C5CB2A40C6C26D15B0A32960410A9F1EA6E50B636F1AB389"
				+ "ABE8933DFF1600C026E34718F1785A4CDEAB90C35698B394E38B6947AE91DE116");
		
		byte[] message = "testing message".getBytes();
		
		try {
			byte[] signature = Ed25519Key.sign(message, priv);
			boolean valid = Ed25519Key.verify(message, signature, pub);
			
			assertTrue(valid);
		} catch (Exception ex) {
			fail();
		}
	}
	
	/**
	 * Test for {@link ED25519#sign()}.
	 */
	@Test
	public void testSign_Invalid() {
		byte[] pub = Utils.hexStringToBytes("BE8933DFF1600C026E34718F1785A4CDEAB90C35698B394E38B6947AE91DE116");
		byte[] priv = Utils.hexStringToBytes("0000A07C7A8CE16C5CB2A40C6C26D15B0A32960410A9F1EA6E50B636F1AB389"
				+ "ABE8933DFF1600C026E34718F1785A4CDEAB90C35698B394E38B6947AE91DE116");
		
		byte[] message = "testing message".getBytes();
		
		try {
			byte[] signature = Ed25519Key.sign(message, priv);
			boolean valid = Ed25519Key.verify(message, signature, pub);
			
			assertFalse(valid);
		} catch (Exception ex) {
			fail();
		}
	}
	
}
