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
import java.util.OptionalInt;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test class for serialization of classes.
 */
public class SerializationTest {
	
	private Map<Integer, Node> nodeList;
	
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
		for (int i = 0; i < 10; i++) {
			this.nodeList.put(i, new OwnNode(i));
		}
		// Generate genesis block (10 nodes, 1000 coins)
		this.genesisBlock = TendermintHelper.generateGenesisBlock(1000, this.nodeList);
		// Setup nodes
		this.aliceNode = setupNode(0);
		this.bobNode = setupNode(1);
		this.charlieNode = setupNode(2);
		// Setup LocalStore for all thhree nodes
		this.aliceLocalStore = setupLocalStore(this.aliceNode);
		this.bobLocalStore = setupLocalStore(this.bobNode);
		this.charlieLocalStore = setupLocalStore(this.charlieNode);
		// Send 100 coins from Alice to Bob
		this.transactionSource = generateTransaction(this.aliceNode, this.bobNode, 100, this.genesisBlock.getTransactions().get(0));
		// Send 50 coins from Bob to Alice
		this.transaction = generateTransaction(this.bobNode, this.aliceNode, 50, this.transactionSource);
		// Add Proof
		this.proof = new Proof(this.transaction);
	}
	
	/**
	 * Setup a node with a genesis and two empty blocks.
	 * @param nodeId - id of the node
	 * @return node
	 */
	public Node setupNode(int nodeId) {
		Node node = this.nodeList.get(nodeId);
		node.setGenesisBlock(this.genesisBlock);
		node.getChain().appendNewBlock(new ArrayList<>());
		node.getChain().appendNewBlock(new ArrayList<>());
		return node;
	}
	
	/**
	 * Setup a Local Store for a node.
	 * @param node - node for the localStore
	 * @return localStore
	 */
	public LocalStore setupLocalStore(Node node) {
		LocalStore localStore = new LocalStore((OwnNode) node, null, this.genesisBlock, false);
		localStore.getNodes().putAll(this.nodeList);
		return localStore;
	}
	
	/**
	 * Generates a transaction that uses the genesis transaction as source.
	 * @param sender - the sender of the transaction
	 * @param receiver - the receiver of the transaction
	 * @param amount - the amount of money
	 * @param transactionSource - source for the new transaction
	 * @return - the transaction
	 */
	public Transaction generateTransaction(Node sender, Node receiver, long amount, Transaction transactionSource) {
		Set<Transaction> sources = new HashSet<>();
		sources.add(transactionSource);
		long remainder = transactionSource.getAmount() - amount;
		Transaction newtTransaction = new Transaction(11, sender, receiver, amount, remainder, sources);
		newtTransaction.setBlockNumber(OptionalInt.of(1));
		sender.getChain().getBlocks().get(1).getTransactions().add(newtTransaction);
		
		return newtTransaction;
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
		assertEquals(genesisBlockMessage.getNumber(), this.genesisBlock.getNumber());
		assertEquals(genesisBlockMessage.getPreviousBlockNumber(), -1);
		assertEquals(genesisBlockMessage.getOwnerId(), Transaction.GENESIS_SENDER);
		assertEquals(genesisBlockMessage.getTransactions().size(), this.genesisBlock.getTransactions().size());
		assertEquals(genesisBlockMessage.getHash(), this.genesisBlock.getHash());
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
		assertEquals(transactionMessage.getNumber(), this.transaction.getNumber());
		assertEquals(transactionMessage.getSenderId(), this.transaction.getSender().getId());
		assertEquals(transactionMessage.getReceiverId(), this.transaction.getReceiver().getId());
		assertEquals(transactionMessage.getAmount(), this.transaction.getAmount());
		assertEquals(transactionMessage.getRemainder(), this.transaction.getRemainder());
		assertEquals(transactionMessage.getHash(), this.transaction.getHash());
		assertEquals(transactionMessage.getBlockNumber(), this.transaction.getBlockNumber().getAsInt());
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
		assertEquals(decodedProof.getTransaction(), this.proof.getTransaction());
		assertEquals(decodedProof.getChainUpdates(), this.proof.getChainUpdates());
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
		assertEquals(blockMessage.getNumber(), aliceBlock.getNumber());
		assertEquals(blockMessage.getPreviousBlockNumber(), aliceBlock.getPreviousBlock().getNumber());
		assertEquals(blockMessage.getOwnerId(), aliceBlock.getOwner().getId());
		assertEquals(blockMessage.getTransactions().size(), aliceBlock.getTransactions().size());
		assertEquals(blockMessage.getHash(), aliceBlock.getHash());
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
		Transaction newTransaction = generateTransaction(this.bobNode, this.charlieNode, 50, this.transactionSource);
		
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
		assertEquals(decodedBlock, originalBlock);
	}
	
}
