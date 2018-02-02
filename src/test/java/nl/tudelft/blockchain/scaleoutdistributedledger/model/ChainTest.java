package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 * Test class for {@link Chain}.
 */
public class ChainTest {
	
	private OwnNode ownNode;
	
	private Node node;
	
	private Chain chain;
	
	private LocalStore localStore;

	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		this.ownNode = new OwnNode(0);
		this.localStore = new LocalStore(this.ownNode, null, null, false);
		this.node = mock(Node.class);
		this.chain = new Chain(this.node);
	}
	
	/**
	 * Test for {@link Chainw#update()}.
	 */
	@Test
	public void testUpdate_EmptyUpdate() {
		List<Block> updateList = new ArrayList<>();
		this.chain.update(updateList, localStore);
		
		assertTrue(this.chain.getBlocks().isEmpty());
	}
	
	/**
	 * Test for {@link Chainw#update()}.
	 */
	@Test
	public void testUpdate_EmptyChain() {
		List<Block> updateList = new ArrayList<>();
		updateList.add(new Block(0, this.node, new ArrayList<>()));
		this.chain.update(updateList, localStore);
		
		assertEquals(updateList, this.chain.getBlocks());
	}

	/**
	 * Test for {@link Chainw#update()}.
	 */
	@Test
	public void testUpdate_NotEmptyChain() {
		List<Block> updateList = new ArrayList<>();
		updateList.add(new Block(0, this.node, new ArrayList<>()));
		this.chain.update(updateList, localStore);
		updateList.add(new Block(1, this.node, new ArrayList<>()));
		updateList.add(new Block(2, this.node, new ArrayList<>()));
		this.chain.update(updateList, localStore);
		
		assertEquals(updateList, this.chain.getBlocks());
	}
	
}
