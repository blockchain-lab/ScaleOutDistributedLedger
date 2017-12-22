package nl.tudelft.blockchain.scaleoutdistributedledger;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.RSAKey;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketServer;

import java.io.IOException;

/**
 * Class to run a node.
 */
public class Application {

	public static final int TRACKER_SERVER_PORT = 3000;
	public static final String TRACKER_SERVER_ADDRESS = "localhost";
	public static final int NODE_PORT = 8007;
	
	private LocalStore localStore;

	@Getter
	Thread serverThread;
	@Getter
	SocketClient socketClient;

	/**
	 * Creates a new application.
	 * @throws IOException - error while registering nodes
	 */
	public Application() throws IOException {
		this.setupNode();
	}

	/**
	 * Called when we receive a new transaction.
	 * @param transaction - the transaction
	 * @param proof       - the proof
	 */
	public synchronized void receiveTransaction(Transaction transaction, Proof proof) {
		if (CommunicationHelper.receiveTransaction(localStore.getVerification(), transaction, proof)) {
			if (transaction.getAmount() > 0) {
				localStore.getUnspent().add(transaction);
			}
		}
	}
	
	/**
	 * Send a transaction to the receiver of the transaction.
	 * An abstract of the block containing the transaction (or a block after it) must already be
	 * committed to the main chain.
	 * @param transaction - the transaction to send
	 */
	public void sendTransaction(Transaction transaction) {
		CommunicationHelper.sendTransaction(transaction);
	}
	
	/**
	 * Setup your own node.
	 * Register to the tracker and setup the local store.
	 * @throws java.io.IOException - error while registering node
	 */
	private void setupNode() throws IOException {
		// Create and register node
		RSAKey key = new RSAKey();
		Node ownNode = TrackerHelper.registerNode(key.getPublicKey());
		ownNode.setPrivateKey(key.getPrivateKey());

		this.serverThread = new Thread(new SocketServer(NODE_PORT));
		serverThread.start();
		this.socketClient = new SocketClient();
		
		// Setup local store
		localStore = new LocalStore(ownNode);
	}
}
