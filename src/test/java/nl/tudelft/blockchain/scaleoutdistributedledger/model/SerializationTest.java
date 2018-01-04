package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for serialization of classes.
 */
public class SerializationTest {
	
	private LocalStore localStore;
	
	private Transaction transaction;
	
	private TransactionMessage transactionMessage;
	
	private Proof proof;
	
	private Block block;
	
	/**
	 * Setup method.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Before
	public void setUp() throws IOException {
		// Encode: transactions, proofs and blocks
		Node sender = new Node(1);
		Node receiver = new Node(2);
		this.localStore = new LocalStore(sender);
		// Transaction
		this.transaction = new Transaction(44, sender, receiver, 100, 20, new HashSet<>());
		this.transactionMessage = new TransactionMessage(transaction);
		// Proofs
		this.proof = new Proof(this.transaction);
		// Blocks
		this.block = new Block(1234, sender, new ArrayList<>());
	}
	
	/**
	 * Test the serialization of {@link Transaction}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testTransaction_Valid() throws IOException {
		// Decode transaction object
		Transaction newTransaction = new Transaction(this.transactionMessage, this.localStore);
		// Check primitives
		assertTrue(newTransaction.getNumber() == this.transaction.getNumber());
		assertTrue(newTransaction.getSender() == this.transaction.getSender());
		assertTrue(newTransaction.getReceiver() == this.transaction.getReceiver());
		// Check lists
		assertTrue(newTransaction.getReceiver() == this.transaction.getReceiver());
	}
	
}
