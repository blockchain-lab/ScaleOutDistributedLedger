package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import static org.junit.Assert.*;
import java.util.ArrayList;
import org.junit.Test;

/**
 * Test class for {@link Block}.
 */
public class BlockTest {
	
	/**
	 * Test for {@link Block#isValid()}.
	 */
	@Test
	public void testGetHash_Valid() {
		Node owner = new Node(24);
		Block block = new Block(1234, owner, new ArrayList<>());
		String hash = "334777d018eb8d1acd2d04a3f26b973169920d1c81937241a2b24c0cf0b9b448";
		
		assertTrue(block.getHash().toString().equals(hash));
	}
	
	/**
	 * Test for {@link Block#isValid()}.
	 */
	@Test
	public void testGetHash_Invalid() {
		Node owner = new Node(24);
		Block block = new Block(1234, owner, new ArrayList<>());
		String hash = "004777d018eb8d1acd2d04a3f26b973169920d1c81937241a2b24c0cf0b9b448";
		
		assertFalse(block.getHash().toString().equals(hash));
	}
	
}
