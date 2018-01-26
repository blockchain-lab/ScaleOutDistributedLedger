package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.StopTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.Simulation;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.OnlyNodeZeroTransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.UniformRandomTransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Main class for running a simulation.
 */
public final class SimulationMain {
	private SimulationMain() {}

	//SETTINGS
	//number of local nodes to generate
	public static final int LOCAL_NODES_NUMBER = 3;
	//number of total nodes in the system
	public static final int TOTAL_NODES_NUMBER = 3;
	//number from which our nodes are (e.g if we run nodes (2, 3, 4), then this should be 2
	public static final int NODES_FROM_NUMBER = 0;
	//Whether this main is the master coordinator of the simulation
	//Note that the master should always be started first
	public static final boolean IS_MASTER = true;
	//The duration of the simulation in seconds. Only has an effect when IS_MASTER == true.
	public static final int SIMULATION_DURATION = 6000;

	/**
	 * @param args - the program arguments
	 * @throws Exception - If an exception occurs.
	 */
	public static void main(String[] args) throws Exception {
		List<Integer> nodeNumbersToRunLocally = IntStream.rangeClosed(NODES_FROM_NUMBER, NODES_FROM_NUMBER + LOCAL_NODES_NUMBER - 1)
				.boxed().collect(Collectors.toList());

		// --- PHASE 0: cleanup ---
		if (!cleanup()) return;


		// --- PHASE 1: generating key pairs and registering with tracker ---

		// generate priv_validator.json for each local node
		Map<Integer, Ed25519Key> nodeToKeyPair = TendermintHelper.generatePrivValidatorFiles(nodeNumbersToRunLocally);

		// register with tracker, keep track of local own nodes
		Map<Integer, OwnNode> ownNodes = registerOwnNodes(nodeToKeyPair);

		// wait for all the nodes to register in tracker
		waitForRegister();


		// --- PHASE 2: all nodes registered, so create genesis block and genesis.json files ---

		//update nodes from the tracker
		Map<Integer, Node> nodes = new HashMap<>(TOTAL_NODES_NUMBER);
		TrackerHelper.updateNodes(nodes, null);
		final Block genesisBlock = TendermintHelper.generateGenesisBlock(1000, nodes);

		//generate genesis.json for all local nodes
		TendermintHelper.generateGenesisFiles(new Date(),
				generatePublicKeysMap(nodes),
				genesisBlock.getHash().getBytes(), nodeNumbersToRunLocally);


		// --- PHASE 3: start the actual simulation ---
		Simulation simulation = new Simulation(IS_MASTER);
//		ITransactionPattern itp = new OnlyNodeZeroTransactionPattern(10, 20, 1000, 2000, 1);
		UniformRandomTransactionPattern itp = new UniformRandomTransactionPattern(10, 10, 2000, 2000, 2);
//		itp.setSeed(1);
		simulation.setTransactionPattern(itp);
		simulation.runNodesLocally(nodes, ownNodes, genesisBlock, nodeToKeyPair);

		// Wait for all nodes to have initialized
		waitForInitialize();

		simulation.initialize();
		
		Thread.sleep(5000);
		simulation.start();


		// --- PHASE 4: stop the simulation ---

		if (IS_MASTER) {
			Thread.sleep(SIMULATION_DURATION * 1000);
			simulation.broadcastMessage(new StopTransactingMessage());
		}

		// Wait for all the other nodes to stop
		waitForStop();
		simulation.stop();

		simulation.stopLocalNodes();
		simulation.cleanup();
	}

	/**
	 * Wait until all nodes have stopped.
	 * @throws InterruptedException - when the sleep is interrupted
	 * @throws IOException - when the connection with the tracker fails
	 */
	private static void waitForStop() throws InterruptedException, IOException {
		Log.log(Level.INFO, "Waiting on nodes to stop");
		do {
			Thread.sleep(2000);
		} while (0 != TrackerHelper.getRunning());
		Log.log(Level.INFO, "All nodes have stopped");
	}

	/**
	 * Wait until all nodes have initialized.
	 * @throws InterruptedException - when the sleep is interrupted
	 * @throws IOException - when the connection with the tracker fails
	 */
	private static void waitForInitialize() throws IOException, InterruptedException {
		Log.log(Level.INFO, "Waiting on nodes to initialize");
		while (TOTAL_NODES_NUMBER != TrackerHelper.getRunning()) {
			Thread.sleep(1000);
		}
		Log.log(Level.INFO, "All nodes have initialized");
	}

	/**
	 * Wait until all nodes have registered.
	 * @throws InterruptedException - when the sleep is interrupted
	 * @throws IOException - when the connection with the tracker fails
	 */
	private static void waitForRegister() throws IOException, InterruptedException {
		Log.log(Level.INFO, "Waiting on nodes to register");
		while (TOTAL_NODES_NUMBER != TrackerHelper.getRegistered()) {
			Thread.sleep(1000);
		}
		Log.log(Level.INFO, "All nodes have registered");
	}

	/**
	 * Register all own nodes to the tracker using the given keypairs.
	 * @param nodeToKeyPair - a mapping between node IDs and keypairs
	 * @return - a map containing the registered nodes as objects by their IDs.
	 * @throws IOException - when the connection with the tracker fails
	 */
	private static Map<Integer, OwnNode> registerOwnNodes(Map<Integer, Ed25519Key> nodeToKeyPair) throws IOException {
		Map<Integer, byte[]> localPublicKeys = convertToPublicKeys(nodeToKeyPair);
		Map<Integer, OwnNode> ownNodes = new HashMap<>(LOCAL_NODES_NUMBER);

		for (int i = 0; i < LOCAL_NODES_NUMBER; i++) {
			int basePort = Application.NODE_PORT + i * 4;
			int nodeID = NODES_FROM_NUMBER + i;
			ownNodes.put(nodeID, TrackerHelper.registerNode(basePort, localPublicKeys.get(nodeID), nodeID));
		}
		return ownNodes;
	}

	/**
	 * Cleanup any residues of previous runs and verify that the tracker is running.
	 * @return - true when the tracker is running, false otherwise
	 */
	private static boolean cleanup() throws IOException {
		// Clean Tendermint folder
		TendermintHelper.cleanTendermintFiles();

		// Reset the tracker server when you are running the tracker server
		try {
			if (IS_MASTER) {
				TrackerHelper.resetTrackerServer();
			} else {
				// Check if the tracker is running
				TrackerHelper.getStatus();
			}
		} catch (IOException e) {
			Log.log(Level.SEVERE, "Tracker not running, please start it on '" + Application.TRACKER_SERVER_ADDRESS + "'");
			Log.log(Level.INFO, "The tracker can be started using `npm start` in the tracker-server folder");
			return false;
		}
		return true;
	}

	private static Map<Integer, String> generatePublicKeysMap(Map<Integer, Node> nodes) {
		Map<Integer, String> ret = new HashMap<>(nodes.size());
		for (Integer i: nodes.keySet()) {
			ret.put(i, Utils.bytesToHexString(nodes.get(i).getPublicKey()));
		}
		return ret;
	}

	private static Map<Integer, byte[]> convertToPublicKeys(Map<Integer, Ed25519Key> nodeToKeyPairMap) {
		Map<Integer, byte[]> publicKeys = new HashMap<>(nodeToKeyPairMap.size());
		for (Map.Entry<Integer, Ed25519Key> e : nodeToKeyPairMap.entrySet()) {
			publicKeys.put(e.getKey(), e.getValue().getPublicKey());
		}
		return publicKeys;
	}
}
