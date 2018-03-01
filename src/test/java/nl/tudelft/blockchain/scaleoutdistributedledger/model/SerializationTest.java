package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.ProofMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Test class for serialization of classes.
 */
public class SerializationTest {
	
	private int transactionNumber = 11;
	
	private Map<Integer, Node> nodeList;
	
	private Node aliceNode;
	
	private Node bobNode;
	
	private Node charlieNode;
	
	private LocalStore aliceLocalStore;
	
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
		setupLocalStore(this.bobNode);
		this.charlieLocalStore = setupLocalStore(this.charlieNode);
		// Send 100 coins from Alice to Bob
		this.transactionSource = generateTransaction(this.aliceNode, this.bobNode, 100, this.genesisBlock.getTransactions().get(0));
		// Send 50 coins from Bob to Alice
		this.transaction = generateTransaction(this.bobNode, this.aliceNode, 50, this.transactionSource);

		// Add Proof
		this.proof = new Proof(this.transaction);
		// Manually change the chainUpdates
		List<Block> listBlockUpdate = new ArrayList<>(this.bobNode.getChain().getBlocks());
		listBlockUpdate.remove(0);
		this.proof.getChainUpdates().put(this.bobNode, listBlockUpdate);
	}
	
	/**
	 * Setup a node with a genesis and two empty blocks.
	 * @param nodeId - id of the node
	 * @return node
	 */
	public Node setupNode(int nodeId) {
		Node node = new OwnNode(nodeId);
		node.getChain().setGenesisBlock(this.genesisBlock);
		node.getChain().appendNewBlock();
		node.getChain().appendNewBlock();
		return node;
	}
	
	/**
	 * Setup a Local Store for a node.
	 * @param node - node for the localStore
	 * @return localStore
	 */
	public LocalStore setupLocalStore(Node node) {
		LocalStore localStore = new LocalStore((OwnNode) node, null, this.genesisBlock, false);
		// Make sure each node has its own list of nodes
		HashMap<Integer, Node> newNodeList = new HashMap<>();
		for (int i = 0; i < 10; i++) {
			if (node.getId() == i) continue;
			Node localNode = new Node(i);
			localNode.getChain().setGenesisBlock(this.genesisBlock);
			newNodeList.put(i, localNode);
		}
		localStore.getNodes().putAll(newNodeList);
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
		// Create transaction
		TreeSet<Transaction> sources = new TreeSet<>();
		sources.add(transactionSource);
		long remainder = transactionSource.getAmount() - amount;
		Transaction newtTransaction = new Transaction(this.transactionNumber++, sender, receiver, amount, remainder, sources);
		// Add to chains
		sender.getChain().getBlocks().get(1).addTransaction(newtTransaction);
		// Set lastCommitedBlock
		sender.getChain().setLastCommittedBlock(sender.getChain().getBlocks().get(1));
		
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
		assertEquals(this.genesisBlock.getNumber(), genesisBlockMessage.getNumber());
		assertEquals(GenesisNode.GENESIS_NODE_ID, genesisBlockMessage.getOwnerId());
		assertEquals(this.genesisBlock.getTransactions().size(), genesisBlockMessage.getTransactions().length);
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
		assertEquals(this.transaction.getNumber(), transactionMessage.getNumber());
		assertEquals(this.transaction.getSender().getId(), transactionMessage.getSenderId());
		assertEquals(this.transaction.getReceiver().getId(), transactionMessage.getReceiverId());
		assertEquals(this.transaction.getAmount(), transactionMessage.getAmount());
		assertEquals(this.transaction.getRemainder(), transactionMessage.getRemainder());
		assertEquals(this.transaction.getHash(), transactionMessage.getHash());
		assertEquals(this.transaction.getBlockNumber(), transactionMessage.getBlockNumber());
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
		assertEquals(this.proof.getTransaction(), decodedProof.getTransaction());
		assertEquals(this.proof.getChainUpdates(), decodedProof.getChainUpdates());
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
		assertEquals(aliceBlock.getNumber(), blockMessage.getNumber());
		assertEquals(aliceBlock.getOwner().getId(), blockMessage.getOwnerId());
		assertEquals(aliceBlock.getTransactions().size(), blockMessage.getTransactions().length);
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
		// Manually change the chainUpdates
		originalProof.getChainUpdates().put(this.aliceNode, new ArrayList<>(this.aliceNode.getChain().getBlocks()));
		originalProof.getChainUpdates().put(this.bobNode, new ArrayList<>(this.bobNode.getChain().getBlocks()));
		// Encode Proof into ProofMessage
		ProofMessage encodedProof = new ProofMessage(originalProof);
		// Decode ProofMessage into Proof
		Proof decodedProof = new Proof(encodedProof, this.charlieLocalStore);
		
		// Check Block and Transaction
		Block originalBlock = this.aliceNode.getChain().getBlocks().get(1);
		// Note: The decoding process got rid of the genesis block
		Block decodedBlock = decodedProof.getChainUpdates().get(this.charlieLocalStore.getNodes().get(0)).get(1);
		assertEquals(originalBlock, decodedBlock);
	}
	
}
