package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Class to test {@link Node}.
 */
public class NodeTest {
	
	/**
	 * Test for {@link Node#updateMetaKnowledge(Proof) }.
	 */
	@Test
	public void testUpdateMetaKnowledge() {
		Ed25519Key key = new Ed25519Key();
		Node node = new Node(1, key.getPublicKey(), "127.0.0.1", 1234);
		Node otherNode = new Node(2);
		Proof proof = new Proof(null);
		proof.addBlock(new Block(1, otherNode, new ArrayList<>()));
		proof.addBlock(new Block(2, otherNode, new ArrayList<>()));
		proof.addBlock(new Block(3, otherNode, new ArrayList<>()));
		node.updateMetaKnowledge(proof);

		assertEquals(3, node.getMetaKnowledge().get(otherNode).intValue());
	}
	
}
