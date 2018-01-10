package nl.tudelft.blockchain.scaleoutdistributedledger;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.RSAKey;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketServer;

import java.io.IOException;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;

/**
 * Class to run a node.
 */
public class Application {
	public static final int TRACKER_SERVER_PORT = 3000;
	public static final String TRACKER_SERVER_ADDRESS = "localhost";
	public static final int NODE_PORT = 8007;
	private static MainChain mainChain;
	
	private LocalStore localStore;

	@Getter
	private Thread serverThread;
	@Getter
	private SocketClient socketClient;

	/**
	 * Creates a new application.
	 * @param tendermintPort - the port on which the tendermint server will run.
	 * @throws IOException - error while registering nodes
	 */
	public Application(int tendermintPort) throws IOException {
		this.setupNode(tendermintPort);
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
	 * @param tmPort - the port on which to run the Tendermint server
	 * @throws java.io.IOException - error while registering node
	 */
	private void setupNode(int tmPort) throws IOException {
		// Create and register node
		RSAKey key = new RSAKey();
//		Node ownNode = TrackerHelper.registerNode(key.getPublicKey());
		Node ownNode = new Node(0, new byte[0], "localhost", 80808, tmPort);
		ownNode.setPrivateKey(key.getPrivateKey());

		this.serverThread = new Thread(new SocketServer(NODE_PORT));
		serverThread.start();
		this.socketClient = new SocketClient();
		
		// Setup local store
		localStore = new LocalStore(ownNode);

	}


}
