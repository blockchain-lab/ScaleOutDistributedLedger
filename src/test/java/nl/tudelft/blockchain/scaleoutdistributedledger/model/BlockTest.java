package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import java.io.ByteArrayOutputStream;
import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link Block}.
 */
public class BlockTest {
	
	private Node owner;
	
	private Block block;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		this.owner = new Node(24);
		Ed25519Key keyPair = new Ed25519Key();
		this.owner.setPrivateKey(keyPair.getPrivateKey());
		this.owner.setPublicKey(keyPair.getPublicKey());
		this.block = new Block(1234, owner, new ArrayList<>());
	}
	
	/**
	 * Test for {@link Block#getHash()}.
	 */
	@Test
	public void testGetHash_Valid() {
		String hash = "334777d018eb8d1acd2d04a3f26b973169920d1c81937241a2b24c0cf0b9b448";
		
		assertTrue(this.block.getHash().toString().equals(hash));
	}
	
	/**
	 * Test for {@link Block#getHash()}.
	 */
	@Test
	public void testGetHash_Invalid() {
		String hash = "004777d018eb8d1acd2d04a3f26b973169920d1c81937241a2b24c0cf0b9b448";
		
		assertFalse(this.block.getHash().toString().equals(hash));
	}

	/**
	 * Test for {@link Block#getBlockAbstract()}.
	 */
	@Test
	public void testGetAbstract_Valid() {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(Utils.intToByteArray(this.block.getOwner().getId()));
			outputStream.write(Utils.intToByteArray(this.block.getNumber()));
			outputStream.write(this.block.getHash().getBytes());
			byte[] attrInBytes = outputStream.toByteArray();
			
			assertTrue(this.block.getOwner().verify(attrInBytes, this.block.getBlockAbstract().getSignature()));
		} catch (Exception ex) {
			fail();
		}
	}
	
	/**
	 * Test for {@link Block#getBlockAbstract()}.
	 */
	@Test
	public void testGetAbstract_Invalid() {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write(Utils.intToByteArray(this.block.getOwner().getId() + 1));
			outputStream.write(Utils.intToByteArray(this.block.getNumber()));
			outputStream.write(this.block.getHash().getBytes());
			byte[] attrInBytes = outputStream.toByteArray();

			assertFalse(this.block.getOwner().verify(attrInBytes, this.block.getBlockAbstract().getSignature()));
		} catch (Exception ex) {
			fail();
		}
	}
	
}
