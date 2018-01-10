package nl.tudelft.blockchain.scaleoutdistributedledger;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketServer;

import java.io.IOException;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;

/**
 * Class to run a node.
 */
public class Application {
	public static final int TRACKER_SERVER_PORT = 3000;
	public static final String TRACKER_SERVER_ADDRESS = "localhost";
	public static final int NODE_PORT = 8007;
	private static MainChain mainChain;
	
	private LocalStore localStore;
	private Thread executor;
	private CancellableInfiniteRunnable transactionExecutable;

	@Getter
	private Thread serverThread;
	@Getter
	private SocketClient socketClient;

	/**
	 * Creates a new application.
	 * The application must be initialized with {@link #init(int, int)} before it can be used.
	 */
	public Application() {}
	
	/**
	 * Initializes the application.
	 * Registers to the tracker and creates the local store.
	 * @param nodePort       - the port on which the node will accept connections.
	 * @param tendermintPort - the port on which the tendermint server will run.
	 * @throws IOException   - error while registering node
	 */
	public void init(int nodePort, int tendermintPort) throws IOException {
		Ed25519Key key = new Ed25519Key();
		OwnNode ownNode = TrackerHelper.registerNode(key.getPublicKey());
		ownNode.setPrivateKey(key.getPrivateKey());

		this.serverThread = new Thread(new SocketServer(NODE_PORT, localStore));
		serverThread.start();
		this.socketClient = new SocketClient();
		
		// Setup local store
		localStore = new LocalStore(ownNode, this);
		localStore.updateNodes();
		if (mainChain == null) {
			mainChain = new TendermintChain(tendermintPort);
		}
	}
	
	/**
	 * Stops this application. This means that this application no longer accepts any new
	 * connections and that all existing connections are closed.
	 */
	public void stop() {
		if (serverThread.isAlive()) {
			serverThread.interrupt();
		}
		//TODO Stop socket client?
	}
	
	/**
	 * @param pattern - the transaction pattern
	 * @throws IllegalStateException - If there is already a transaction pattern running.
	 */
	public synchronized void setTransactionPattern(ITransactionPattern pattern) {
		if (isTransacting()) throw new IllegalStateException("There is already a transaction pattern running!");
		this.transactionExecutable = pattern.getRunnable(localStore);
	}
	
	/**
	 * Starts making transactions by executing the transaction pattern.
	 * @throws IllegalStateException - If there is already a transaction pattern running.
	 */
	public synchronized void startTransacting() {
		if (isTransacting()) throw new IllegalStateException("There is already a transaction pattern running!");
		this.executor = new Thread(this.transactionExecutable);
		this.executor.start();
	}
	
	/**
	 * Stops making transactions.
	 */
	public synchronized void stopTransacting() {
		if (!isTransacting()) return;
		this.transactionExecutable.cancel();
	}
	
	/**
	 * @return if a transaction pattern is running
	 */
	public synchronized boolean isTransacting() {
		return this.executor != null && this.executor.isAlive();
	}
	
	/**
	 * Send a transaction to the receiver of the transaction.
	 * An abstract of the block containing the transaction (or a block after it) must already be
	 * committed to the main chain.
	 * @param transaction - the transaction to send
	 * @throws InterruptedException - when sending is interrupted
	 */
	public void sendTransaction(Transaction transaction) throws InterruptedException {
		CommunicationHelper.sendTransaction(transaction, socketClient);
	}

	/**
	 * Returns the singleton main chain.
	 * @return -
	 */
	public static MainChain getMainChain() {
		return mainChain;
	}
}
