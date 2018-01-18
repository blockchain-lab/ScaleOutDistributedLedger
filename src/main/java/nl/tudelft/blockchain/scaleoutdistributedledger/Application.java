package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.CancellableInfiniteRunnable;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketServer;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import lombok.Getter;

/**
 * Class to run a node.
 */
public class Application {
	public static final String TRACKER_SERVER_ADDRESS = "localhost";
	public static final int TRACKER_SERVER_PORT = 3000;
	public static final int NODE_PORT = 40000;
	
	@Getter
	private LocalStore localStore;
	private Thread executor;
	private CancellableInfiniteRunnable transactionExecutable;
	private final boolean isProduction;

	@Getter
	private Thread serverThread;
	
	@Getter
	private TransactionSender transactionSender;

	/**
	 * Creates a new application.
	 * The application must be initialized with {@link #init(int, Block, Map, Ed25519Key, OwnNode)} before it can be used.
	 * @param isProduction - if this is production or testing
	 */
	public Application(boolean isProduction) {
		this.isProduction = isProduction;
	}
	
	/**
	 * Initializes the application.
	 * Registers to the tracker and creates the local store.
	 * @param nodePort     - the port on which the node will accept connections. Note, also port+1,
	 *                       port+2 and port+3 are used (for tendermint: p2p.laddr, rpc.laddr, ABCI server).
	 * @param genesisBlock - the genesis (initial) block for the entire system
	 * @param key          - the key
	 * @param ownNode      - the own node
	 * @throws IOException - error while registering node
	 */
	public void init(int nodePort, Block genesisBlock, Ed25519Key key, OwnNode ownNode) throws IOException {
		ownNode.setGenesisBlock(genesisBlock);

		ownNode.setPrivateKey(key.getPrivateKey());

		// Setup local store
		localStore = new LocalStore(ownNode, this, genesisBlock, this.isProduction);
		localStore.updateNodes();
		localStore.initMainChain();

		serverThread = new Thread(new SocketServer(nodePort, localStore));
		serverThread.start();
		transactionSender = new TransactionSender(localStore);
	}
	
	/**
	 * Stops this application. This means that this application no longer accepts any new
	 * connections and that all existing connections are closed.
	 */
	public void stop() {
		if (serverThread.isAlive()) serverThread.interrupt();
		if (transactionSender != null) transactionSender.shutdownNow();
		
		localStore.getMainChain().stop();
	}
	
	/**
	 * @param pattern - the transaction pattern
	 * @throws IllegalStateException - If there is already a transaction pattern running.
	 */
	public synchronized void setTransactionPattern(ITransactionPattern pattern) {
		if (isTransacting()) throw new IllegalStateException("There is already a transaction pattern running!");
		this.transactionExecutable = pattern.getRunnable(localStore);
		Log.log(Level.FINE, "Node " + localStore.getOwnNode().getId() + ": Set transaction pattern " + pattern.getName());
	}
	
	/**
	 * Starts making transactions by executing the transaction pattern.
	 * @throws IllegalStateException - If there is already a transaction pattern running.
	 */
	public synchronized void startTransacting() {
		if (isTransacting()) throw new IllegalStateException("There is already a transaction pattern running!");
		this.executor = new Thread(this.transactionExecutable);
		this.executor.setUncaughtExceptionHandler((t, ex) -> 
			Log.log(Level.SEVERE, "Node " + localStore.getOwnNode().getId() + ": Uncaught exception in transaction pattern executor!", ex)
		);
		this.executor.start();
		Log.log(Level.INFO, "Node " + localStore.getOwnNode().getId() + ": Started transacting with transaction pattern.");
	}
	
	/**
	 * Stops making transactions.
	 */
	public synchronized void stopTransacting() {
		if (!isTransacting()) return;
		this.transactionExecutable.cancel();
		Log.log(Level.INFO, "Node " + localStore.getOwnNode().getId() + ": Stopped transacting with transaction pattern.");
	}
	
	/**
	 * @return if a transaction pattern is running
	 */
	public synchronized boolean isTransacting() {
		return this.executor != null && this.executor.isAlive();
	}

	/**
	 * @return - the main chain of this application
	 */
	public MainChain getMainChain() {
		return localStore.getMainChain();
	}
}
