package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;

import static org.junit.Assert.assertTrue;

/**
 * Test class for serialization of classes.
 */
public class SerializationTest {
	
	private HashMap<Integer, Node> nodeList;
	
	private Node aliceNode;
	
	private Node bobNode;
	
	private Node charlieNode;
	
	private LocalStore aliceLocalStore;
	
	private LocalStore bobLocalStore;
	
	private LocalStore charlieLocalStore;
		
	private Block genesisBlock;
	
	private Transaction transactionSource;
	
	private Transaction transaction;
	
	private Proof proof;
	
	/**
	 * Setup method.
	 * 
	 * Transactions:
	 * 
	 * 900		100		1000
	 * Alice	---->	Bob
	 * 
	 * 900		50		1050
	 * Alice	<----	Bob
	 */
	@Before
	public void setUp() {
		// Initialize node list
		this.nodeList = new HashMap<>();
		this.nodeList.put(0, new OwnNode(0));
		for (int i = 1; i < 10; i++) {
			this.nodeList.put(i, new OwnNode(i));
		}
		// Generate genesis block (10 nodes, 1000 coins)
		this.genesisBlock = TendermintHelper.generateGenesisBlock(this.nodeList.size(), 1000, nodeList);
		// Setup Alice and block
		this.aliceNode = this.nodeList.get(0);
		List<Block> blockListAlice = new ArrayList<>();
		blockListAlice.add(this.genesisBlock);
		blockListAlice.add(new Block(1, this.aliceNode, new ArrayList<>()));
		blockListAlice.add(new Block(2, this.aliceNode, new ArrayList<>()));
		this.aliceNode.getChain().update(blockListAlice);
		// Setup Bob and block
		this.bobNode = this.nodeList.get(1);
		List<Block> blockListBob = new ArrayList<>();
		blockListBob.add(this.genesisBlock);
		blockListBob.add(new Block(1, this.bobNode, new ArrayList<>()));
		this.bobNode.getChain().update(blockListBob);
		// Setup Charlie
		this.charlieNode = this.nodeList.get(2);
		List<Block> blockListCharlie = new ArrayList<>();
		blockListCharlie.add(this.genesisBlock);
		this.charlieNode.getChain().update(blockListCharlie);
		// Setup LocalStore for Alice
		this.aliceLocalStore = new LocalStore((OwnNode) this.aliceNode, null, this.genesisBlock, false, this.nodeList);
		// Setup LocalStore for Bob 
		this.bobLocalStore = new LocalStore((OwnNode) this.bobNode, null, this.genesisBlock, false, this.nodeList);
		// Setup LocalStore for Charlie
		this.charlieLocalStore = new LocalStore((OwnNode) this.charlieNode, null, this.genesisBlock, false, this.nodeList);
		// Send from Alice to Bob
		HashSet<Transaction> sources = new HashSet<>();
		sources.add(this.genesisBlock.getTransactions().get(0));
		this.transactionSource = new Transaction(22, this.aliceNode, this.bobNode, 100, 900, sources);
		this.transactionSource.setBlockNumber(OptionalInt.of(1));
		blockListAlice.get(0).getTransactions().add(this.transactionSource);
		// Send from Bob to Alice
		HashSet<Transaction> newSources = new HashSet<>();
		newSources.add(this.transactionSource);
		this.transaction = new Transaction(44, this.bobNode, this.aliceNode, 50, 50, newSources);
		this.transaction.setBlockNumber(OptionalInt.of(1));
		blockListBob.get(0).getTransactions().add(this.transaction);
		// Add Proof
		this.proof = new Proof(this.transaction);
	}
	
	/**
	 * Test the encoding of a genesis {@link Block}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testGensisBlockMessage_Valid() throws IOException {
		// Encode genesis block
		BlockMessage genesisBlockMessage = new BlockMessage(this.genesisBlock);
		// Check
		assertTrue(genesisBlockMessage.getNumber() == this.genesisBlock.getNumber());
		assertTrue(genesisBlockMessage.getPreviousBlockNumber() == -1);
		assertTrue(genesisBlockMessage.getOwnerId() == Transaction.GENESIS_SENDER);
		assertTrue(genesisBlockMessage.getTransactions().size() == this.genesisBlock.getTransactions().size());
		assertTrue(genesisBlockMessage.getHash().equals(this.genesisBlock.getHash()));
	}
	
	/**
	 * Test the encoding of {@link Transaction}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testTransactionMessage_Valid() throws IOException {
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
	}
	
	/**
	 * Test the encoding and decoding of {@link Proof}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testProofMessage_Valid() throws IOException {
		// Encode Proof into ProofMessage (from Bob to Alice)
		ProofMessage proofMessage = new ProofMessage(this.proof);
		// Decode ProofMessage into Proof (on Alice node)
		Proof decodedProof = new Proof(proofMessage, this.aliceLocalStore);
		// Check references
		assertTrue(decodedProof.getTransaction().equals(this.proof.getTransaction()));
		assertTrue(decodedProof.getChainUpdates().equals(this.proof.getChainUpdates()));
	}
	
	/**
	 * Test the encoding of NOT genesis {@link Block}.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testBlockMessage_Valid() throws IOException {
		// Encode Block into BlockMessage
		Block aliceBlock = this.aliceNode.getChain().getBlocks().get(1);
		BlockMessage blockMessage = new BlockMessage(aliceBlock);
		// Check
		assertTrue(blockMessage.getNumber() == aliceBlock.getNumber());
		assertTrue(blockMessage.getPreviousBlockNumber() == -1);
		assertTrue(blockMessage.getOwnerId() == aliceBlock.getOwner().getId());
		assertTrue(blockMessage.getHash().equals(aliceBlock.getHash()));
		assertTrue(blockMessage.getTransactions().size() == aliceBlock.getTransactions().size());
	}
	
	/**
	 * Test the serialization of {@link Proof} with metaknowledge.
	 * @throws IOException - error while getting nodes from tracker
	 */
	@Test
	public void testDecoding_Valid() throws IOException {		
		// New Transaction:
		// 1000		50		1000
		// Bob		---->	Charlie
		
		// Create new transaction
		HashSet<Transaction> sourcesList = new HashSet<>();
		sourcesList.add(this.transactionSource);
		Transaction newTransaction = new Transaction(88, this.bobNode, this.charlieNode, 50, 50, sourcesList);
		this.bobNode.getChain().getBlocks().get(1).getTransactions().add(newTransaction);
		
		// Create Proof
		Proof originalProof = new Proof(newTransaction);
		originalProof.getChainUpdates().put(this.aliceNode, this.aliceNode.getChain().getBlocks());
		originalProof.getChainUpdates().put(this.bobNode, this.bobNode.getChain().getBlocks());
		// Encode Proof into ProofMessage
		ProofMessage encodedProof = new ProofMessage(originalProof);
		// Decode ProofMessage into Proof
		Proof decodedProof = new Proof(encodedProof, this.charlieLocalStore);
		
		// Check Block and Transaction
		Block originalBlock = this.bobNode.getChain().getBlocks().get(1);
		Block decodedBlock = decodedProof.getChainUpdates().get(this.bobNode).get(1);
		assertTrue(decodedBlock.equals(originalBlock));
	}
	
}
