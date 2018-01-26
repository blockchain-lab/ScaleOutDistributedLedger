
package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * Test for {@link BlockAbstract}.
 */
public class BlockAbstractTest {
	
	/**
	 * Test for {@link BlockAbstract#fromBytes}.
	 */
	@Test
	public void testFromBytes_Invalid() {
		BlockAbstract blockAbstract = BlockAbstract.fromBytes(new byte[0]);
		assertEquals(null, blockAbstract);
	}
	
	/**
	 * Test for {@link BlockAbstract#checkBlockHash}.
	 */
	@Test
	public void testCheckBlockHash_Valid() {
		Ed25519Key key = new Ed25519Key();
		Block block = new Block(2, new Node(1), new ArrayList<>());
		try {
			byte[] signature = key.sign(block.getHash().getBytes());
			BlockAbstract blockAbstract = new BlockAbstract(1, block.getNumber(), block.getHash(), signature);
			assertTrue(blockAbstract.checkBlockHash(block));
		} catch (SignatureException | InvalidKeyException | NoSuchAlgorithmException ex) {
			fail();
		}
	}
	
}
