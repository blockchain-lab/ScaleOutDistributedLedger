package nl.tudelft.blockchain.scaleoutdistributedledger;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.exceptions.NotEnoughMoneyException;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.MetaKnowledge;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;

/**
 * Test class for TransactionCreator.
 */
public class TransactionCreatorTest {
	private OwnNode ownNode;
	
	private LocalStore localStore;
	
	private Map<Integer, Node> nodes;
	
	/**
	 * Called before every test.
	 */
	@Before
	public void setUp() {
		Settings.INSTANCE.cheatyMetaKnowledge = false;
		
		ownNode = new OwnNode(0);
		//TestHelper.generateGenesis(ownNode, 5, 100);
		localStore = new LocalStore(ownNode, null, null, false);
		nodes = localStore.getNodes();
	}
	
	/**
	 * Creates the nodes <tt>from</tt> up to (inclusive) <tt>to</tt>.
	 * @param from - the id of the first node to created
	 * @param to   - the id of the last node to create
	 */
	public void createNodes(int from, int to) {
		for (int i = from; i <= to; i++) {
			nodes.put(i, new Node(i));
			localStore.getNewTransactionId();
		}
	}
	
	/**
	 * Convenience method for getting nodes by id.
	 * @param id - the id of the node to get
	 * @return     the node with the given id
	 */
	public Node getNode(int id) {
		return localStore.getNode(id);
	}
	
	/**
	 * Adds the given chains as meta knowledge to the given node.
	 * @param node   - the node to add the meta knowledge to
	 * @param chains - the chains to add to the meta knowledge
	 */
	public void addMetaKnowledge(Node node, int... chains) {
		MetaKnowledge meta = node.getMetaKnowledge();
		for (int chain : chains) {
			meta.updateLastKnownBlockNumber(chain, 2);
		}
	}
	
	/**
	 * Adds an unspent transaction.
	 * A genesis transaction will be set as source.
	 * @param sender    - the sender of the transaction
	 * @param receiver  - the receiver of the transaction
	 * @param amount    - the amount
	 * @param remainder - the remainder
	 * @return            the transaction
	 */
	public Transaction addUnspent(Node sender, Node receiver, long amount, long remainder) {
		//Create genesis block
		Transaction genesis = new Transaction(0, null, sender, amount + remainder, 0, new TreeSet<>());
		Block genesisBlock = new Block(0, null, Arrays.asList(genesis));
		sender.getChain().getBlocks().add(genesisBlock);
		
		//Create transaction and block
		Transaction transaction = new Transaction(localStore.getNewTransactionId(), sender, receiver, amount, remainder, genesis);
		Block block1 = new Block(1, sender, Arrays.asList(transaction));
		sender.getChain().getBlocks().add(block1);
		
		localStore.addUnspentTransaction(transaction);
		return transaction;
	}
	
	/**
	 * Adds a transaction where we receive money to the unspent transactions.
	 * @param from   - the node we received money from
	 * @param amount - the amount of money received
	 * @return         the transaction
	 */
	public Transaction addReceivedMoney(Node from, long amount) {
		return addUnspent(from, ownNode, amount, 0);
	}
	
	/**
	 * Adds a transaction where we spend money but have leftovers to the unspent transactions.
	 * @param to        - the node we sent money to
	 * @param remainder - the amount of money left over
	 * @return            the transaction
	 */
	public Transaction addRemainderMoney(Node to, long remainder) {
		return addUnspent(ownNode, to, remainder + 1, remainder);
	}
	
	/**
	 * Checks if the given transaction has exactly the given sources.
	 * @param transaction     - the transaction to check the sources from
	 * @param expectedSources - the expected sources
	 */
	public void checkTransactionSources(Transaction transaction, Transaction... expectedSources) {
		Set<Transaction> actualSet = transaction.getSource();
		Set<Transaction> expectedSet = new TreeSet<>();
		expectedSet.addAll(Arrays.asList(expectedSources));
		
		assertEquals(expectedSet, actualSet);
	}
	
	/**
	 * Test for creating a transaction when we don't have any money.
	 */
	@Test(expected = NotEnoughMoneyException.class)
	public void testNotEnoughMoney1() {
		createNodes(1, 1);
		
		TransactionCreator tc = new TransactionCreator(localStore, getNode(1), 10);
		tc.createTransaction();
	}
	
	/**
	 * Test for creating a transaction when we don't have enough money.
	 */
	@Test(expected = NotEnoughMoneyException.class)
	public void testNotEnoughMoney2() {
		createNodes(1, 2);
		
		//We have 5 money, received from node 2.
		addReceivedMoney(getNode(2), 5);
		
		TransactionCreator tc = new TransactionCreator(localStore, getNode(1), 10);
		tc.createTransaction();
	}
	
	/**
	 * Test case for the scenario explained below.
	 * <pre>
	 * Transactions:
	 * $5  from 2 -> 0
	 * $5  from 3 -> 0
	 * $10 from 4 -> 0
	 * 
	 * Knowledge:
	 * 1 knows about 2 and 3
	 * 
	 * Transaction to create:
	 * $10 from 0 -> 1
	 * </pre>
	 * 
	 * The source choices are:
	 * <pre>
	 * Selected  -> To Send, Amount, Sufficient, Filtered
	 * {2}       -> {},      5,      NO,         NO
	 * {3}       -> {},      5,      NO,         NO
	 * {4}       -> {4},     10,     YES,        YES, BEST ROUND 1
	 * {2, 3}    -> {},      10,     YES,        YES, BEST ROUND 2
	 * </pre>
	 * 
	 * We expect the outcome to be:
	 * <pre>
	 * Selected  -> To Send, Amount, Remainder
	 * {2, 3}    -> {},      10,     0
	 * </pre>
	 */
	@SuppressWarnings("unused")
	@Test
	public void testScenario1() {
		//Create node 1 through 4
		createNodes(1, 4);
		
		//Node 1 knows about chain 2 and 3
		addMetaKnowledge(getNode(1), 2, 3);
		
		//Add unspent transactions
		Transaction t2 = addReceivedMoney(getNode(2), 5);
		Transaction t3 = addReceivedMoney(getNode(3), 5);
		Transaction t4 = addReceivedMoney(getNode(4), 10);
		
		//We want to send 10 money to node 1
		TransactionCreator tc = new TransactionCreator(localStore, getNode(1), 10);
		
		Transaction transaction = tc.createTransaction();
		
		//Check if all the fields are correct
		assertEquals(ownNode, transaction.getSender());
		assertEquals(getNode(1), transaction.getReceiver());
		assertEquals(10, transaction.getAmount());
		assertEquals(0, transaction.getRemainder());
		
		//We expect t2 and t3 to be used as the sources
		checkTransactionSources(transaction, t2, t3);
	}
	
	/**
	 * Test case for the scenario explained below.
	 * <pre>
	 * Transactions:
	 * $5  from 2 -> 0
	 * $5  from 3 -> 0
	 * $10 from 4 -> 0
	 * $1  from 5 -> 0
	 * 
	 * Knowledge:
	 * 1 knows about 2 and 5
	 * 
	 * Transaction to create:
	 * $10 from 0 -> 1
	 * </pre>
	 * 
	 * The source choices are:
	 * <pre>
	 * Selected  -> To Send, Amount, Sufficient, Filtered
	 * {2}       -> {},      5,      NO,         NO
	 * {3}       -> {3},     5,      NO,         NO
	 * {4}       -> {4},     10,     YES,        YES, BEST ROUND 1
	 * {5}       -> {},      1,      NO,         NO
	 * {2, 3}    -> {3},     10,     YES,        ROUND 2, EQUAL TO BEST
	 * {2, 5}    -> {},      6,      NO,         NO
	 * {3, 5}    -> {3},     6,      NO,         ROUND 2, EQUAL TO BEST
	 * </pre>
	 * 
	 * We expect the outcome to be:
	 * <pre>
	 * Selected  -> To Send, Amount, Remainder
	 * {4}       -> {4},     $10,    $0
	 * </pre>
	 */
	@SuppressWarnings("unused")
	@Test
	public void testScenario2() {
		//Create node 1 through 5
		createNodes(1, 5);
		
		//Node 1 knows about chain 2 and 5
		addMetaKnowledge(getNode(1), 2, 5);
		
		//Add unspent transactions
		Transaction t2 = addReceivedMoney(getNode(2), 5);
		Transaction t3 = addReceivedMoney(getNode(3), 5);
		Transaction t4 = addReceivedMoney(getNode(4), 10);
		Transaction t5 = addReceivedMoney(getNode(5), 1);
		
		//We want to send 10 money to node 1
		TransactionCreator tc = new TransactionCreator(localStore, getNode(1), 10);
		
		Transaction transaction = tc.createTransaction();
		
		//Check if all the fields are correct
		assertEquals(ownNode, transaction.getSender());
		assertEquals(getNode(1), transaction.getReceiver());
		assertEquals(10, transaction.getAmount());
		assertEquals(0, transaction.getRemainder());
		
		//We expect t4 to be used as the sources
		checkTransactionSources(transaction, t4);
	}
}
