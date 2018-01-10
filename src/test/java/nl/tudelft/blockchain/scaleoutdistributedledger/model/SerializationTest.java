package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
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
	
	private ProofMessage proofMessage;
	
	private Block block;
	
	private BlockMessage blockMessage;
	
	/**
	 * Setup method.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Before
	public void setUp() throws IOException {
		// Encode: transactions, proofs and blocks
		// Setup sender and block
		OwnNode sender = new OwnNode(1);
		this.block = new Block(1234, sender, new ArrayList<>());
		List<Block> blockList = new ArrayList<>();
		blockList.add(this.block);
		sender.getChain().update(blockList);
		Node receiver = new Node(2);
		// Setup LocalStore
		this.localStore = new LocalStore(sender, null);
		this.localStore.getNodes().put(receiver.getId(), receiver);
		// Add Transaction
		this.transaction = new Transaction(44, sender, receiver, 100, 20, new HashSet<>());
		this.block.getTransactions().add(this.transaction);
		// Encode Block into BlockMessage
		this.blockMessage = new BlockMessage(this.block);
		// Encode Transaction into TransactionMessage
		this.transactionMessage = new TransactionMessage(transaction);
		// Add Proof
		this.proof = new Proof(this.transaction);
		// Encode Proof into ProofMessage
		this.proofMessage = new ProofMessage(this.proof);
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
		assertTrue(newTransaction.getAmount() == this.transaction.getAmount());
		assertTrue(newTransaction.getReceiver() == this.transaction.getReceiver());
		assertTrue(newTransaction.getBlockNumber().equals(this.transaction.getBlockNumber()));
		// Check references
		assertTrue(newTransaction.getSender().equals(this.transaction.getSender()));
		assertTrue(newTransaction.getReceiver().equals(this.transaction.getReceiver()));
		assertTrue(newTransaction.getHash().equals(this.transaction.getHash()));
		// Check sets
		assertTrue(newTransaction.getSource().equals(this.transaction.getSource()));
	}
	
	/**
	 * Test the serialization of {@link Proof}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testProof_Valid() throws IOException {
		// Decode transaction object
		Proof newProof = new Proof(this.proofMessage, this.localStore);
		// Check references
		assertTrue(newProof.getTransaction().equals(this.proof.getTransaction()));
		assertTrue(newProof.getChainUpdates().equals(this.proof.getChainUpdates()));
	}
	
	/**
	 * Test the serialization of {@link Block}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testBlock_Valid() throws IOException {
		// Decode transaction object
		Block newBlock = new Block(this.blockMessage, this.localStore);
		// Check primitives
		assertTrue(newBlock.getNumber() == this.block.getNumber());
		// Check references
		if (newBlock.getPreviousBlock() != null) {
			assertTrue(newBlock.getPreviousBlock().equals(this.block.getPreviousBlock()));
		}
		assertTrue(newBlock.getOwner().equals(this.block.getOwner()));
		assertTrue(newBlock.getHash().equals(this.block.getHash()));
		// Check list
		assertTrue(newBlock.getTransactions().equals(this.block.getTransactions()));
	}
	
}
