package nl.tudelft.blockchain.scaleoutdistributedledger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.test.utils.TestHelper;

/**
 * Class to test {@link LocalStore}.
 */
public class LocalStoreTest {
	
	private OwnNode ownNode;
	
	private LocalStore localStore;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		this.ownNode = new OwnNode(0);
		Application application = mock(Application.class);
		Block genesisBlock = TestHelper.generateGenesis(ownNode, 10, 1000);
		this.localStore = new LocalStore(ownNode, application, genesisBlock, false);
	}
	
	/**
	 * Test for {@link LocalStore#getTransactionFromNode(int, int)}.
	 */
	@Test
	public void testGetTransactionFromNode_Valid_GenesisSender() {
		Transaction transaction = this.localStore.getTransactionFromNode(Transaction.GENESIS_SENDER, 0, 0);
		assertEquals(this.ownNode, transaction.getReceiver());
	}
	
	/**
	 * Test for {@link LocalStore#getTransactionFromNode(int, int)}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testGetTransactionFromNode_Invalid_GenesisSender() {
		this.localStore.getTransactionFromNode(Transaction.GENESIS_SENDER, 0, 99);
	}
	
	/**
	 * Test for {@link LocalStore#getTransactionFromNode(int, int)}.
	 */
	@Test
	public void testGetTransactionFromNode_Valid_NormalSender() {
		Transaction transaction = this.localStore.getTransactionFromNode(0, 0, 0);
		assertEquals(this.ownNode, transaction.getReceiver());
	}
	
	/**
	 * Test for {@link LocalStore#getTransactionFromNode(int, int)}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testGetTransactionFromNode_Invalid_Block() {
		this.localStore.getTransactionFromNode(0, 99, 1);
	}
	
	/**
	 * Test for {@link LocalStore#getTransactionFromNode(int, int)}.
	 */
	@Test(expected = IllegalStateException.class)
	public void testGetTransactionFromNode_Invalid_Transaction() {
		this.localStore.getTransactionFromNode(0, 0, 99);
	}
	
}
