package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.util.*;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.test.utils.TestHelper;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;

/**
 * Class to test {@link CommunicationHelper}.
 */
public class CommunicationHelperTest {
	
	private static int transactionId = 11;
	
	private Block genesisBlock;
	
	private OwnNode ownNode;
	
	private LocalStore localStore;
	
	private Node bobNode;
	
	private Node charlieNode;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		// Setup ownNode
		this.ownNode = new OwnNode(0);
		this.genesisBlock = TestHelper.generateGenesis(this.ownNode, 10, 1000);
		Application application = mock(Application.class);
		this.localStore = new LocalStore(this.ownNode, application, this.genesisBlock, false);
		// Setup bobNode
		this.bobNode = new Node(1);
		bobNode.getChain().setGenesisBlock(this.genesisBlock);
		// Setup charlie
		this.charlieNode = new Node(2);
		this.charlieNode.getChain().setGenesisBlock(this.genesisBlock);
	}
	
	/**
	 * Create a transaction from a genesis block.
	 * @param sender - node sender
	 * @param receiver - node receiver
	 * @param amount - amount of the transaction
	 * @param remainder - remainder of the transaction
	 * @return 
	 */
	private Transaction createTransactionFromGenesis(Node sender, Node receiver, long amount, long remainder) {
		// Create new block for transaction
		List<Block> blockList = new ArrayList<>();
		blockList.add(new Block(sender.getChain().getLastBlock().getNumber() + 1, sender, new ArrayList<>()));
		sender.getChain().update(blockList, localStore);
		// Setup sources
		TreeSet<Transaction> sources = new TreeSet<>();
		sources.add(this.genesisBlock.getTransactions().get(sender.getId()));
		// Create transaction
		Transaction transaction = new Transaction(transactionId++, sender, receiver, amount, remainder, sources);
		Block blockOfTransaction = sender.getChain().getLastBlock();
		transaction.setBlockNumber(blockOfTransaction.getNumber());
		blockOfTransaction.addTransaction(transaction);
		return transaction;
	}
	
	/**
	 * Test for {@link CommunicationHelper#receiveTransaction}.
	 */
	@Test
	public void testReceiveTransaction_Valid() {
		// Create Transaction and Proof
		Transaction transaction = this.createTransactionFromGenesis(this.bobNode, this.ownNode, 100, 900);
		Proof proof = new Proof(transaction);
		
		assertTrue(CommunicationHelper.receiveTransaction(proof, this.localStore));
		assertTrue(this.localStore.getUnspent().contains(transaction));
	}
	
	/**
	 * Test for {@link CommunicationHelper#receiveTransaction}.
	 */
	@Test
	public void testReceiveTransaction_InvalidReceiver() {
		// Create Transaction and Proof
		Transaction transaction = this.createTransactionFromGenesis(this.bobNode, this.charlieNode, 100, 900);
		Proof proof = new Proof(transaction);
		
		assertFalse(CommunicationHelper.receiveTransaction(proof, this.localStore));
	}
	
	/**
	 * Test for {@link CommunicationHelper#receiveTransaction}.
	 */
	@Test
	public void testReceiveTransaction_InvalidAmount() {
		// Create Transaction and Proof
		Transaction transaction = this.createTransactionFromGenesis(this.bobNode, this.ownNode, 9999, 900);
		Proof proof = new Proof(transaction);
		
		assertFalse(CommunicationHelper.receiveTransaction(proof, this.localStore));
	}
	
}
