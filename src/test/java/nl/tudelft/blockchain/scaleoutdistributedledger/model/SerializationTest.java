package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.tudelft.blockchain.scaleoutdistributedledger.test.utils.TestHelper;

import static org.junit.Assert.*;

/**
 * Test class for serialization of classes.
 */
public class SerializationTest {
	
	private Map<Integer, Node> nodeList;
	
	private OwnNode sender;
	
	private Node receiver;
	
	private LocalStore localStore;
		
	private Block genesisBlock;
	
	private Transaction transaction;
	
	private Proof proof;
	
	private Block block;
	
	/**
	 * Setup method.
	 */
	@Before
	public void setUp() {
		//Generate a genesis block for 10 nodes, 1000 money each
		this.sender = new OwnNode(0);
		this.genesisBlock = TestHelper.generateGenesis(this.sender, 10, 1000);
		this.nodeList = TestHelper.getNodeList(this.genesisBlock);
		
		// Setup LocalStore
		this.localStore = new LocalStore(this.sender, null, this.genesisBlock, false);
		this.localStore.getNodes().putAll(this.nodeList);
		
		this.receiver = this.nodeList.get(1);
		
		//Create a following block with one transaction
		this.transaction = generateTransaction(this.receiver, 100);
		this.block = this.sender.getChain().appendNewBlock();
		
		this.block.addTransaction(this.transaction);
		
		//Create an empty following block
		this.sender.getChain().appendNewBlock();
		
		// Add Proof
		this.proof = new Proof(this.transaction);
	}
	
	/**
	 * Generates a transaction that uses the genesis transaction as source.
	 * @param receiver - the receiver of the transaction
	 * @param amount - the amount of money
	 * @return - the transaction
	 */
	public Transaction generateTransaction(Node receiver, long amount) {
		Transaction genesisTransaction = this.sender.getChain().getGenesisBlock().getTransactions().get(this.sender.getId());
		Set<Transaction> sources = new HashSet<>();
		sources.add(genesisTransaction);
		
		long remainder = genesisTransaction.getAmount() - amount;
		return new Transaction(20, this.sender, receiver, amount, remainder, sources);
	}
	
	/**
	 * Test the serialization of a genesis {@link Block}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testBlockGenesis_Valid() throws IOException {
		// Encode genesis block
		BlockMessage genesisBlockMessage = new BlockMessage(this.genesisBlock);
		// Check
		assertEquals(this.genesisBlock.getNumber(), genesisBlockMessage.getNumber());
		assertEquals(-1, genesisBlockMessage.getPreviousBlockNumber());
		assertEquals(null, genesisBlockMessage.getPreviousBlock());
		assertEquals(Transaction.GENESIS_SENDER, genesisBlockMessage.getOwnerId());
		assertEquals(this.genesisBlock.getTransactions().size(), genesisBlockMessage.getTransactions().size());
		assertEquals(this.genesisBlock.getHash(), genesisBlockMessage.getHash());
		// Decode genesis block
		Block decodedGenesisBlock = new Block(genesisBlockMessage, this.localStore);
		// Check
		assertEquals(this.genesisBlock.getNumber(), decodedGenesisBlock.getNumber());
		// getPreviousBlock should be null
		assertEquals(this.genesisBlock.getPreviousBlock(), decodedGenesisBlock.getPreviousBlock());
		// getOwner should be null
		assertEquals(this.genesisBlock.getOwner(), decodedGenesisBlock.getOwner());
		assertEquals(this.genesisBlock.getTransactions(), decodedGenesisBlock.getTransactions());
		assertEquals(this.genesisBlock.getHash(), decodedGenesisBlock.getHash());
	}
	
	/**
	 * Test the serialization of {@link Transaction}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testTransaction_Valid() throws IOException {
		// Encode Transaction into TransactionMessage
		TransactionMessage transactionMessage = new TransactionMessage(this.transaction);
		// Check
		assertEquals(this.transaction.getNumber(), transactionMessage.getNumber());
		assertEquals(this.transaction.getSender().getId(), transactionMessage.getSenderId());
		assertEquals(this.transaction.getReceiver().getId(), transactionMessage.getReceiverId());
		assertEquals(this.transaction.getAmount(), transactionMessage.getAmount());
		assertEquals(this.transaction.getRemainder(), transactionMessage.getRemainder());
		assertEquals(this.transaction.getHash(), transactionMessage.getHash());
		assertEquals(this.transaction.getBlockNumber().getAsInt(), transactionMessage.getBlockNumber());
		// Decode TransactionMessahe into Transaction
		Transaction decodedTransaction = new Transaction(transactionMessage, this.localStore);
		// Check
		assertEquals(this.transaction.getNumber(), decodedTransaction.getNumber());
		assertEquals(this.transaction.getSender(), decodedTransaction.getSender());
		assertEquals(this.transaction.getReceiver(), decodedTransaction.getReceiver());
		assertEquals(this.transaction.getAmount(), decodedTransaction.getAmount());
		assertEquals(this.transaction.getRemainder(), decodedTransaction.getRemainder());
		assertEquals(this.transaction.getSource(), decodedTransaction.getSource());
		assertEquals(this.transaction.getHash(), decodedTransaction.getHash());
		assertEquals(this.transaction.getBlockNumber(), decodedTransaction.getBlockNumber());
	}
	
	/**
	 * Test the serialization of {@link Proof}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testProof_Valid() throws IOException {
		// Encode Proof into ProofMessage
		ProofMessage proofMessage = new ProofMessage(this.proof);
		// Decode ProofMessage into Proof
		Proof decodedProof = new Proof(proofMessage, this.localStore);
		// Check references
		assertEquals(this.proof.getTransaction(), decodedProof.getTransaction());
		assertEquals(this.proof.getChainUpdates(), decodedProof.getChainUpdates());
	}
	
	/**
	 * Test the serialization of NOT genesis {@link Block}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testBlock_Valid() throws IOException {
		// Encode Block into BlockMessage
		BlockMessage blockMessage = new BlockMessage(this.block);
		// Check
		assertEquals(this.block.getNumber(), blockMessage.getNumber());
		assertEquals(-1, blockMessage.getPreviousBlockNumber());
		assertEquals(this.block.getOwner().getId(), blockMessage.getOwnerId());
		assertEquals(this.block.getHash(), blockMessage.getHash());
		assertEquals(this.block.getTransactions().size(), blockMessage.getTransactions().size());
		// Decode BlockMessage into Block
		Block decodedBlock = new Block(blockMessage, this.localStore);
		// Check
		assertEquals(this.block.getNumber(), decodedBlock.getNumber());
		if (decodedBlock.getPreviousBlock() != null) {
			assertEquals(this.block.getPreviousBlock(), decodedBlock.getPreviousBlock());
		}
		assertEquals(this.block.getOwner(), decodedBlock.getOwner());
		assertEquals(this.block.getTransactions(), decodedBlock.getTransactions());
		assertEquals(this.block.getHash(), decodedBlock.getHash());
	}
	
	/**
	 * Test the serialization of {@link Transaction} with metaknowledge.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testTransaction_WithMetaknowledge() throws IOException {
		// Setup new receiver
		Node newReceiver = this.nodeList.get(2);
		newReceiver.getMetaKnowledge().put(this.sender, 1234);
		// Create new transaction
		HashSet<Transaction> listSources = new HashSet<>();
		listSources.add(this.transaction);
		Transaction newTransaction = new Transaction(40, this.sender, newReceiver, 200, 700, listSources);
		this.sender.getChain().getBlocks().get(1).addTransaction(newTransaction);
		// Encode Transaction into TransactionMessage
		TransactionMessage transactionMessage = new TransactionMessage(newTransaction);
		// Check the correctness of the encoding
		assertTrue(transactionMessage.getKnownSource().contains(new SimpleEntry<>(this.sender.getId(), this.transaction.getNumber())));
		assertTrue(transactionMessage.getNewSource().isEmpty());
		// Decode TransactionMessage into Transaction
		Transaction decodedTransaction = new Transaction(transactionMessage, this.localStore);
		// Check that newReceiver gets the sources correctly
		assertEquals(1, decodedTransaction.getSource().size());
		Transaction sourceTransaction = decodedTransaction.getSource().iterator().next();
		assertEquals(this.transaction.getNumber(), sourceTransaction.getNumber());
		assertEquals(this.transaction.getAmount(), sourceTransaction.getAmount());
		assertEquals(this.transaction.getRemainder(), sourceTransaction.getRemainder());
		assertEquals(this.transaction.getHash(), sourceTransaction.getHash());
		assertEquals(this.transaction.getBlockNumber(), sourceTransaction.getBlockNumber());
	}
	
}
