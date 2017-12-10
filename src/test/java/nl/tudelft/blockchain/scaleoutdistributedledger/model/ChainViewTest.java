package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link ChainView}.
 */
public class ChainViewTest {
	private ChainView chainview;
	private Chain chainMock;
	private Node nodeMock;
	private List<Block> blocks;
	private List<Block> updatedBlocks;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		blocks = new ArrayList<Block>();
		updatedBlocks = new ArrayList<Block>();
		
		nodeMock = mock(Node.class);
		chainMock = mock(Chain.class);
		when(chainMock.getBlocks()).thenReturn(blocks);
		
		chainview = new ChainView(chainMock, updatedBlocks);
	}
	
	/**
	 * @param nr
	 * 		the block number
	 * @param toChain
	 * 		if true, the block is added to the chain.
	 * 		if false, the block is added to the updatedBlocks
	 * 
	 * @return
	 * 		the block that was added
	 */
	protected Block addBlock(int nr, boolean toChain) {
		Block block = new Block(nr, nodeMock, new ArrayList<Transaction>());
		
		if (toChain) {
			blocks.add(block);
		} else {
			updatedBlocks.add(block);
		}
		
		return block;
	}
	
	/**
	 * Test for {@link ChainView#isValid()}.
	 */
	@Test
	public void testIsValid_Valid() {
		addBlock(0, true);
		addBlock(1, true);
		addBlock(2, false);
		
		assertTrue(chainview.isValid());
	}
	
	/**
	 * Test for {@link ChainView#isValid()}.
	 */
	@Test
	public void testIsValid_EmptyUpdate() {
		addBlock(0, true);
		addBlock(1, true);
		
		assertTrue(chainview.isValid());
	}
	
	/**
	 * Test for {@link ChainView#isValid()}.
	 */
	@Test
	public void testIsValid_Missing() {
		addBlock(0, true);
		addBlock(1, true);
		addBlock(3, false);
		
		assertFalse(chainview.isValid());
	}
	
	/**
	 * Test for {@link ChainView#isValid()}.
	 */
	@Test
	public void testIsValid_Gap() {
		addBlock(0, true);
		addBlock(1, true);
		addBlock(2, false);
		addBlock(4, false);
		
		assertFalse(chainview.isValid());
	}
	
	/**
	 * Test for {@link ChainView#isValid()}.
	 */
	@Test
	public void testIsValid_OverlapCorrect() {
		addBlock(0, true);
		addBlock(1, true);
		addBlock(2, true);
		addBlock(1, false);
		addBlock(2, false);
		addBlock(3, false);
		
		assertTrue(chainview.isValid());
	}
	
	/**
	 * Test for {@link ChainView#isValid()}.
	 */
	@Test
	public void testIsValid_OverlapIncorrect() {
		addBlock(0, true);
		addBlock(1, true);
		addBlock(2, true);
		addBlock(1, false);
		addBlock(3, false);
		
		assertFalse(chainview.isValid());
	}
	
	/**
	 * Test for {@link ChainView#getBlock(int)}.
	 */
	@Test
	public void testGetBlock() {
		addBlock(0, true);
		addBlock(1, true);
		
		assertEquals(0, chainview.getBlock(0).getNumber());
		assertEquals(1, chainview.getBlock(1).getNumber());
	}
	
	/**
	 * Test for {@link ChainView#getBlock(int)}. Check if the blocks we know take precedence over
	 * the blocks in the update part.
	 */
	@Test
	public void testGetBlock_Overlap() {
		addBlock(0, true);
		Block good1 = addBlock(1, true);
		addBlock(1, false);
		
		assertSame(good1, chainview.getBlock(1));
	}
	
	/**
	 * Test for {@link ChainView#getBlock(int)}. Check if getting a block from the updated part
	 * also works.
	 */
	@Test
	public void testGetBlock_UpdatePart() {
		addBlock(0, true);
		addBlock(1, true);
		addBlock(2, false);
		
		assertEquals(2, chainview.getBlock(2).getNumber());
	}
	
	/**
	 * Test for {@link ChainView#iterator()}, for next and nextIndex.
	 */
	@Test
	public void testIteratorNextIndex() {
		addBlock(0, true);
		addBlock(1, false);
		
		ListIterator<Block> it = chainview.iterator();
		assertEquals(0, it.nextIndex());
		assertEquals(0, it.next().getNumber());
		assertEquals(1, it.nextIndex());
		assertEquals(1, it.next().getNumber());
		assertEquals(2, it.nextIndex());
	}
	
	/**
	 * Test for {@link ChainView#listIterator(int)}, for previous and nextIndex.
	 */
	@Test
	public void testListIteratorNextIndex() {
		addBlock(0, true);
		addBlock(1, false);
		addBlock(2, false);
		
		ListIterator<Block> it = chainview.listIterator(2);
		assertEquals(2, it.nextIndex());
		assertEquals(1, it.previous().getNumber());
		assertEquals(1, it.nextIndex());
		assertEquals(0, it.previous().getNumber());
		assertEquals(0, it.nextIndex());
	}
	
	/**
	 * Test for {@link ChainView#iterator()}, for next and previousIndex.
	 */
	@Test
	public void testIteratorPreviousIndex() {
		addBlock(0, true);
		addBlock(1, false);
		
		ListIterator<Block> it = chainview.iterator();
		assertEquals(-1, it.previousIndex());
		assertEquals(0, it.next().getNumber());
		assertEquals(0, it.previousIndex());
		assertEquals(1, it.next().getNumber());
		assertEquals(1, it.previousIndex());
	}
	
	/**
	 * Test for {@link ChainView#listIterator()}, for alternating next and previous.
	 */
	@Test
	public void testListIteratorNextPrevious() {
		addBlock(0, true);
		addBlock(1, false);
		
		ListIterator<Block> it = chainview.listIterator();
		assertEquals(0, it.next().getNumber());
		assertEquals(0, it.previous().getNumber());
		assertEquals(0, it.next().getNumber());
	}
	
	/**
	 * Test for {@link ChainView#listIterator(int)}, for previous and previousIndex.
	 */
	@Test
	public void testListIteratorPreviousIndex() {
		addBlock(0, true);
		addBlock(1, false);
		addBlock(2, false);
		
		ListIterator<Block> it = chainview.listIterator(2);
		assertEquals(1, it.previousIndex());
		assertEquals(1, it.previous().getNumber());
		assertEquals(0, it.previousIndex());
		assertEquals(0, it.previous().getNumber());
		assertEquals(-1, it.previousIndex());
	}
}
