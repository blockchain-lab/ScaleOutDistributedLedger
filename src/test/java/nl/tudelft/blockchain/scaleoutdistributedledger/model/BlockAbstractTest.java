package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link BlockAbstract}.
 */
public class BlockAbstractTest {
	
	private Ed25519Key key;
	
	private Block block;
	
	private BlockAbstract blockAbstract;
	
	/**
	 * Setup method.
	 * @throws java.security.SignatureException - error while signing
	 * @throws java.security.InvalidKeyException - error while using the key
	 * @throws java.security.NoSuchAlgorithmException - error while using Ed25519
	 */
	@Before
	public void setUp() throws SignatureException, InvalidKeyException, NoSuchAlgorithmException {
		this.key = new Ed25519Key();
		OwnNode ownNode = new OwnNode(0);
		ownNode.setPublicKey(this.key.getPublicKey());
		ownNode.setPrivateKey(this.key.getPrivateKey());
		this.block = new Block(2, ownNode, new ArrayList<>());
		this.blockAbstract = this.block.calculateBlockAbstract();
	}
	
	/**
	 * Test for {@link BlockAbstract#fromBytes()}.
	 */
	@Test
	public void testFromBytes_Invalid() {
		// Invalid decoding
		BlockAbstract newBlockAbstract = BlockAbstract.fromBytes(new byte[0]);
		assertEquals(null, newBlockAbstract);
	}
	
	/**
	 * Test for {@link BlockAbstract#toBytes()}.
	 */
	@Test
	public void testToBytes_Invalid() {
		// Encode
		byte[] bytes = this.blockAbstract.toBytes();
		// Decode
		BlockAbstract newBlockAbstract = BlockAbstract.fromBytes(bytes);
		
		assertEquals(this.blockAbstract, newBlockAbstract);
	}
	
	/**
	 * Test for {@link BlockAbstract#checkBlockHash}.
	 */
	@Test
	public void testCheckBlockHash_Valid() {
		assertTrue(this.blockAbstract.checkBlockHash(this.block));
	}
	
	/**
	 * Test for {@link BlockAbstract#checkSignature(byte[])}.
	 */
	@Test
	public void testCheckSignature_Valid() {
		assertTrue(this.blockAbstract.checkSignature(this.key.getPublicKey()));
	}
	
}
