package nl.tudelft.blockchain.scaleoutdistributedledger;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.settings.Settings;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.Simulation;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

/**
 * Main class for running a simulation.
 */
public final class SimulationMain {
	private static Simulation simulation;
	
	private SimulationMain() {}
	
	/**
	 * @param args - the program arguments
	 * @throws Exception - If an exception occurs.
	 */
	public static void main(String[] args) throws Exception {
		List<Integer> nodeNumbersToRunLocally = IntStream.rangeClosed(
				Settings.INSTANCE.nodesFromNumber,
				Settings.INSTANCE.nodesFromNumber + Settings.INSTANCE.localNodesNumber - 1)
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
		Map<Integer, Node> nodes = TrackerHelper.getNodes();
		Block genesisBlock = TendermintHelper.generateGenesisBlock(Settings.INSTANCE.initialMoney, nodes);

		//generate genesis.json for all local nodes
		TendermintHelper.generateGenesisFiles(new Date(),
				generatePublicKeysMap(nodes),
				genesisBlock.getHash().getBytes(), nodeNumbersToRunLocally);


		// --- PHASE 3: start the actual simulation ---
		simulation = new Simulation(Settings.INSTANCE.isMaster);
		simulation.setTransactionPattern(Settings.INSTANCE.getTransactionPattern());
		simulation.runNodesLocally(nodes, ownNodes, genesisBlock, nodeToKeyPair);

		// Wait for all nodes to have initialized
		waitForInitialize();

		simulation.initialize();
		
		Thread.sleep(5000);
		simulation.start();

		//Stop tendermint if it is not enabled
		if (!Settings.INSTANCE.enableTendermint) simulation.stopTendermint();
		
		// --- PHASE 4: stop the simulation ---
		Thread.sleep(Settings.INSTANCE.simulationDuration * 1000);

		//Stop the simulation and wait for nodes to stop.
		simulation.stop();
		waitForStop();

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
		while (Settings.INSTANCE.totalNodesNumber != TrackerHelper.getRunning()) {
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
		while (Settings.INSTANCE.totalNodesNumber != TrackerHelper.getRegistered()) {
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
		Map<Integer, OwnNode> ownNodes = new HashMap<>(Settings.INSTANCE.localNodesNumber);

		for (int i = 0; i < Settings.INSTANCE.localNodesNumber; i++) {
			int basePort = Application.NODE_PORT + i * 4;
			int nodeID = Settings.INSTANCE.nodesFromNumber + i;
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
			if (Settings.INSTANCE.isMaster) {
				TrackerHelper.resetTrackerServer();
			} else {
				// Check if the tracker is running
				TrackerHelper.getStatus();
			}
		} catch (IOException e) {
			Log.log(Level.SEVERE, "Tracker not running, please start it on '" + Settings.INSTANCE.trackerUrl + "'");
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
	
	/**
	 * WARNING: This method has the potential to leak state!
	 * @param nodeId - the id of the node
	 * @return - the application of the node with the given id, or null if unavailable
	 */
	public static Application getApplicationOfNode(int nodeId) {
		return simulation.getApplicationOfNode(nodeId);
	}
}
