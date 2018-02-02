package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.HashSet;
import org.junit.Before;
import org.junit.Test;

/**
 * Class to test {@link Proof}.
 */
public class ProofTest {
	
	private Proof proof;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		Node nodeSender = new Node(0);
		Node nodeReceiver = new Node(1);
		Transaction transaction = new Transaction(1, nodeSender, nodeReceiver, 100, 20, new HashSet<>());
		this.proof = new Proof(transaction);
	}
	
	/**
	 * Test for {@link Proof#addBlock(Block)}.
	 */
	@Test
	public void testAddBlock_Valid() {
		// TODO
	}
	
}
