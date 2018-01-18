package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;

import static org.junit.Assert.assertTrue;

/**
 * Test class for serialization of classes.
 */
public class SerializationTest {
	
	private HashMap<Integer, Node> nodeList;
	
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
		// Initialize node list
		this.nodeList = new HashMap<>();
		this.nodeList.put(0, new OwnNode(0));
		for (int i = 1; i < 10; i++) {
			this.nodeList.put(i, new Node(i));
		}
		// Setup sender and block
		this.sender = (OwnNode) this.nodeList.get(0);
		
		// Generate genesis block (10 nodes, 1000 coins)
		this.genesisBlock = TendermintHelper.generateGenesisBlock(1000, nodeList);
		this.sender.setGenesisBlock(this.genesisBlock);
		
		//Create two following blocks
		this.block = this.sender.getChain().appendNewBlock();
		this.sender.getChain().appendNewBlock();
		
		this.receiver = this.nodeList.get(1);
		
		// Setup LocalStore
		this.localStore = new LocalStore(this.sender, null, this.genesisBlock, false);
		this.localStore.getNodes().putAll(this.nodeList);
		
		// Add Transaction
		this.transaction = new Transaction(44, this.sender, this.receiver, 100, 20, new HashSet<>());
		this.block.addTransaction(this.transaction);
		// Add Proof
		this.proof = new Proof(this.transaction);
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
		assertTrue(genesisBlockMessage.getNumber() == this.genesisBlock.getNumber());
		assertTrue(genesisBlockMessage.getPreviousBlockNumber() == -1);
		assertTrue(genesisBlockMessage.getPreviousBlock() == null);
		assertTrue(genesisBlockMessage.getOwnerId() == Transaction.GENESIS_SENDER);
		assertTrue(genesisBlockMessage.getTransactions().size() == this.genesisBlock.getTransactions().size());
		assertTrue(genesisBlockMessage.getHash().equals(this.genesisBlock.getHash()));
		// Decode genesis block
		Block decodedGenesisBlock = new Block(genesisBlockMessage, this.localStore);
		// Check
		assertTrue(decodedGenesisBlock.getNumber() == this.genesisBlock.getNumber());
		// getPreviousBlock should be null
		assertTrue(decodedGenesisBlock.getPreviousBlock() == this.genesisBlock.getPreviousBlock());
		// getOwner should be null
		assertTrue(decodedGenesisBlock.getOwner() == this.genesisBlock.getOwner());
		assertTrue(decodedGenesisBlock.getTransactions().equals(this.genesisBlock.getTransactions()));
		assertTrue(decodedGenesisBlock.getHash().equals(this.genesisBlock.getHash()));
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
		assertTrue(transactionMessage.getNumber() == this.transaction.getNumber());
		assertTrue(transactionMessage.getSenderId() == this.transaction.getSender().getId());
		assertTrue(transactionMessage.getReceiverId() == this.transaction.getReceiver().getId());
		assertTrue(transactionMessage.getAmount() == this.transaction.getAmount());
		assertTrue(transactionMessage.getRemainder() == this.transaction.getRemainder());
		assertTrue(transactionMessage.getHash().equals(this.transaction.getHash()));
		assertTrue(transactionMessage.getBlockNumber() == this.transaction.getBlockNumber().getAsInt());
		// Decode TransactionMessahe into Transaction
		Transaction decodedTransaction = new Transaction(transactionMessage, this.localStore);
		// Check
		assertTrue(decodedTransaction.getNumber() == this.transaction.getNumber());
		assertTrue(decodedTransaction.getSender().equals(this.transaction.getSender()));
		assertTrue(decodedTransaction.getReceiver().equals(this.transaction.getReceiver()));
		assertTrue(decodedTransaction.getAmount() == this.transaction.getAmount());
		assertTrue(decodedTransaction.getRemainder() == this.transaction.getRemainder());
		assertTrue(decodedTransaction.getSource().equals(this.transaction.getSource()));
		assertTrue(decodedTransaction.getHash().equals(this.transaction.getHash()));
		assertTrue(decodedTransaction.getBlockNumber().equals(this.transaction.getBlockNumber()));
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
		assertTrue(decodedProof.getTransaction().equals(this.proof.getTransaction()));
		assertTrue(decodedProof.getChainUpdates().equals(this.proof.getChainUpdates()));
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
		assertTrue(blockMessage.getNumber() == this.block.getNumber());
		assertTrue(blockMessage.getPreviousBlockNumber() == -1);
		assertTrue(blockMessage.getOwnerId() == this.block.getOwner().getId());
		assertTrue(blockMessage.getHash().equals(this.block.getHash()));
		assertTrue(blockMessage.getTransactions().size() == this.block.getTransactions().size());
		// Decode BlockMessage into Block
		Block decodedBlock = new Block(blockMessage, this.localStore);
		// Check
		assertTrue(decodedBlock.getNumber() == this.block.getNumber());
		if (decodedBlock.getPreviousBlock() != null) {
			assertTrue(decodedBlock.getPreviousBlock().equals(this.block.getPreviousBlock()));
		}
		assertTrue(decodedBlock.getOwner().equals(this.block.getOwner()));
		assertTrue(decodedBlock.getTransactions().equals(this.block.getTransactions()));
		assertTrue(decodedBlock.getHash().equals(this.block.getHash()));
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
		Transaction newTransaction = new Transaction(88, this.sender, newReceiver, 200, 40, listSources);
		this.sender.getChain().getBlocks().get(1).addTransaction(newTransaction);
		// Encode Transaction into TransactionMessage
		TransactionMessage transactionMessage = new TransactionMessage(newTransaction);
		// Check the correctness of the encoding
		assertTrue(transactionMessage.getKnownSource().contains(new SimpleEntry<>(this.sender.getId(), this.transaction.getNumber())));
		assertTrue(transactionMessage.getNewSource().isEmpty());
		// Decode TransactionMessage into Transaction
		Transaction decodedTransaction = new Transaction(transactionMessage, this.localStore);
		// Check that newReceiver gets the sources correctly
		assertTrue(decodedTransaction.getSource().size() == 1);
		Transaction sourceTransaction = decodedTransaction.getSource().iterator().next();
		assertTrue(sourceTransaction.getNumber() == this.transaction.getNumber());
		assertTrue(sourceTransaction.getAmount() == this.transaction.getAmount());
		assertTrue(sourceTransaction.getRemainder() == this.transaction.getRemainder());
		assertTrue(sourceTransaction.getHash().equals(this.transaction.getHash()));
		assertTrue(sourceTransaction.getBlockNumber().equals(this.transaction.getBlockNumber()));
	}
	
}
