package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.security.SignatureException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

/**
 * Test class for {@link Block}.
 */
public class BlockTest {
	
	private OwnNode owner;
	
	private Block block;
	
	private Application application;
	
	private LocalStore localStore;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		Ed25519Key keyPair = new Ed25519Key();
		this.owner = new OwnNode(24);
		this.owner.setPrivateKey(keyPair.getPrivateKey());
		this.owner.setPublicKey(keyPair.getPublicKey());
		this.block = new Block(1234, this.owner, new ArrayList<>());
		this.application = mock(Application.class);
		this.localStore = new LocalStore(this.owner, this.application, null, false);
	}
	
	/**
	 * Test for {@link Block#getHash()}.
	 */
	@Test
	public void testGetHash_Valid() {
		String hash = "ef958fe3c00f72cccaaa1347b044885c0eb9ab757ecdb2e9d709f07af0cd9253";
		
		assertEquals(hash, this.block.getHash().toString());
	}
	
	/**
	 * Test for {@link Block#getHash()}.
	 */
	@Test
	public void testGetHash_Invalid() {
		String hash = "004777d018eb8d1acd2d04a3f26b973169920d1c81937241a2b24c0cf0b9b448";
		
		assertNotEquals(hash, this.block.getHash().toString());
	}

	/**
	 * Test for {@link Block#calculateBlockAbstract()}.
	 * @throws java.security.SignatureException - fail to sign
	 */
	@Test
	public void testCalculateBlockAbstract_Valid() throws SignatureException {
		byte[] attrInBytes = BlockAbstract.calculateBytesForSignature(this.owner.getId(), this.block.getNumber(), this.block.getHash());
			
		assertTrue(this.owner.verify(attrInBytes, this.block.calculateBlockAbstract().getSignature()));
	}
	
	/**
	 * Test for {@link Block#calculateBlockAbstract()}.
	 * @throws java.security.SignatureException - fail to sign
	 */
	@Test
	public void testCalculateBlockAbstract_Invalid() throws SignatureException {
		byte[] attrInBytes = BlockAbstract.calculateBytesForSignature(this.owner.getId() + 1, this.block.getNumber(), this.block.getHash());
			
		assertFalse(this.owner.verify(attrInBytes, this.block.calculateBlockAbstract().getSignature()));
	}
	
	/**
	 * Test for {@link Block#calculateBlockAbstract()}.
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void testCalculateBlockAbstract_Invalid_NotOwnNode() {
		Block newBlock = new Block(1234, new Node(1), new ArrayList<>());
		newBlock.calculateBlockAbstract();
	}
	
	/**
	 * Test for {@link Block#commit(LocalStore)}.
	 */
	@Test
	public void testCommit_Valid() {
		this.block.commit(this.localStore);
		
		assertEquals(this.block, this.owner.getChain().getLastCommittedBlock());
	}
	
	/**
	 * Test for {@link Block#commit(LocalStore)}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testCommitTwice_Invalid() {
		this.block.commit(this.localStore);
		this.block.commit(this.localStore);
	}
	
	/**
	 * Test for {@link Block#genesisCopy()}.
	 */
	@Test
	public void testGenesisCopy_Valid() {
		Block geneisBlock = new Block(Block.GENESIS_BLOCK_NUMBER, this.owner, new ArrayList<>());
		Block newGenesisBlock = geneisBlock.genesisCopy();
		
		assertEquals(geneisBlock, newGenesisBlock);
	}
	
	/**
	 * Test for {@link Block#isOnMainChain(LocalStore) ()}.
	 */
	@Test
	public void testIsOnMainChain() {
		// Is true because TendermintMock returns always true
		assertTrue(this.block.isOnMainChain(this.localStore));
	}
}
