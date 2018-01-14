package nl.tudelft.blockchain.scaleoutdistributedledger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Chain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

/**
 * Test class for TransactionCreator.
 */
public class TransactionCreatorTest {
	private OwnNode ownNodeMock;
	
	private LocalStore storeMock;
	
	private Map<Integer, Node> nodes;
	
	/**
	 * Called before every test.
	 * Creates all required mocks.
	 */
	@Before
	public void setUp() {
		ownNodeMock = mock(OwnNode.class);
		when(ownNodeMock.getId()).thenReturn(0);
		when(ownNodeMock.getMetaKnowledge()).thenReturn(new HashMap<>());
		when(ownNodeMock.getChain()).thenReturn(new Chain(ownNodeMock));
		
		storeMock = spy(new LocalStore(ownNodeMock, null, null));
		nodes = storeMock.getNodes();
		when(storeMock.getNode(anyInt())).thenAnswer(i -> nodes.get(i.getArgument(0)));
		
		nodes.put(0, ownNodeMock);
	}
	
	/**
	 * Creates the nodes <tt>from</tt> up to (inclusive) <tt>to</tt>.
	 * @param from - the id of the first node to created
	 * @param to   - the id of the last node to create
	 */
	public void createNodes(int from, int to) {
		for (int i = from; i <= to; i++) {
			Node nodeMock = createNodeMock(i);
			nodes.put(i, nodeMock);
		}
	}
	
	/**
	 * Creates a mock for a node with the given id.
	 * The {@link Node#getId()}, {@link Node#getChain()} and {@link Node#getMetaKnowledge()}
	 * methods will work as normal.
	 * @param id - the id of the node
	 * @return     the mocked node
	 */
	public Node createNodeMock(int id) {
		Node nodeMock = mock(Node.class);
		when(nodeMock.getId()).thenReturn(id);
		when(nodeMock.getMetaKnowledge()).thenReturn(new HashMap<>());
		when(nodeMock.getChain()).thenReturn(new Chain(nodeMock));
		return nodeMock;
	}
	
	/**
	 * Convenience method for getting nodes by id.
	 * @param id - the id of the node to get
	 * @return     the node with the given id
	 */
	public Node getNode(int id) {
		return storeMock.getNode(id);
	}
	
	/**
	 * Adds the given chains as meta knowledge to the given node.
	 * @param node   - the node to add the meta knowledge to
	 * @param chains - the chains to add to the meta knowledge
	 */
	public void addMetaKnowledge(Node node, int... chains) {
		Map<Node, Integer> meta = node.getMetaKnowledge();
		for (int chain : chains) {
			meta.put(getNode(chain), 0);
		}
	}
	
	/**
	 * Adds an unspent transaction.
	 * A genesis transaction will be set as source.
	 * @param sender    - the sender of the transaction
	 * @param receiver  - the receiver of the transaction
	 * @param amount    - the amount
	 * @param remainder - the remainder
	 * @return          the transaction
	 */
	public Transaction addUnspent(Node sender, Node receiver, long amount, long remainder) {
		Transaction genesis = new Transaction(0, null, sender, amount + remainder, 0, new HashSet<>());
		Transaction transaction = new Transaction(storeMock.getNewTransactionId(), sender, receiver, amount, remainder, Collections.singleton(genesis));
		storeMock.addUnspentTransaction(transaction);
		return transaction;
	}
	
	/**
	 * Adds a transaction where we receive money to the unspent transactions.
	 * @param from   - the node we received money from
	 * @param amount - the amount of money received
	 * @return         the transaction
	 */
	public Transaction addReceivedMoney(Node from, long amount) {
		return addUnspent(from, ownNodeMock, amount, 0);
	}
	
	/**
	 * Adds a transaction where we spend money but have leftovers to the unspent transactions.
	 * @param to        - the node we sent money to
	 * @param remainder - the amount of money left over
	 * @return            the transaction
	 */
	public Transaction addRemainderMoney(Node to, long remainder) {
		return addUnspent(ownNodeMock, to, remainder + 1, remainder);
	}
	
	/**
	 * Checks if the given transaction has exactly the given sources.
	 * @param transaction     - the transaction to check the sources from
	 * @param expectedSources - the expected sources
	 */
	public void checkTransactionSources(Transaction transaction, Transaction... expectedSources) {
		Set<Transaction> actualSet = transaction.getSource();
		Set<Transaction> expectedSet = new HashSet<>();
		expectedSet.addAll(Arrays.asList(expectedSources));
		
		assertEquals(expectedSet, actualSet);
	}
	
	/**
	 * Test for creating a transaction when we don't have any money.
	 */
	@Test(expected = NotEnoughMoneyException.class)
	public void testNotEnoughMoney1() {
		createNodes(1, 1);
		
		TransactionCreator tc = new TransactionCreator(storeMock, getNode(1), 10);
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
		
		TransactionCreator tc = new TransactionCreator(storeMock, getNode(1), 10);
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
		TransactionCreator tc = new TransactionCreator(storeMock, getNode(1), 10);
		
		Transaction transaction = tc.createTransaction();
		
		//Check if all the fields are correct
		assertEquals(ownNodeMock, transaction.getSender());
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
		TransactionCreator tc = new TransactionCreator(storeMock, getNode(1), 10);
		
		Transaction transaction = tc.createTransaction();
		
		//Check if all the fields are correct
		assertEquals(ownNodeMock, transaction.getSender());
		assertEquals(getNode(1), transaction.getReceiver());
		assertEquals(10, transaction.getAmount());
		assertEquals(0, transaction.getRemainder());
		
		//We expect t4 to be used as the sources
		checkTransactionSources(transaction, t4);
	}
}
