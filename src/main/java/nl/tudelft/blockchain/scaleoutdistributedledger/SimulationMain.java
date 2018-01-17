package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.Simulation;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.RandomTransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import javax.sound.midi.Track;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Main class for running a simulation.
 */
public final class SimulationMain {
	private SimulationMain() {}
	
	/**
	 * @param args - the program arguments
	 * @throws Exception - If an exception occurs.
	 */
	public static void main(String[] args) throws Exception {
		//NOTE: The tracker should be started first, manually

		//SETTINGS
		//number of local nodes to generate
		int localNodesNumber = 4;
		//number of total nodes in the system
		int totalNodesNumber = 4;
		//number from which our nodes are (e.g if we run nodes (2, 3, 4), then this should be 2
		int nodesFromNumber = 0;

		// --- PHASE 0: Cleanup ---

		// Clean Tendermint folder
		TendermintHelper.cleanTendermintFiles(Simulation.TENDERMINT_NODES_FOLDER);

		// reset the tracker server
		TrackerHelper.resetTrackerServer();


		// --- PHASE 1: generating key pairs and registering with tracker ---

		List<Integer> nodeNumbersToRunLocally = IntStream.rangeClosed(nodesFromNumber, nodesFromNumber + localNodesNumber - 1)
				.boxed().collect(Collectors.toList());
		//generate priv_validator.json for each local node
		Map<Integer, Ed25519Key> nodeToKeyPair = TendermintHelper.generatePrivValidatorFiles(
				Simulation.TENDERMINT_BINARY, Simulation.TENDERMINT_NODES_FOLDER, nodeNumbersToRunLocally);

		//register with tracker, keep track of local own nodes
		Map<Integer, byte[]> localPublicKeys = convertToPublicKeys(nodeToKeyPair);
		Map<Integer, OwnNode> ownNodes = new HashMap<>(localNodesNumber);
		//TODO: this would make each application ports start at different places, maybe it should always start at 40000?
		for (Integer i : localPublicKeys.keySet()) {
			int basePort = Application.NODE_PORT + i * 4;
			ownNodes.put(i, TrackerHelper.registerNode(basePort, localPublicKeys.get(i)));
		}

		//wait for all the nodes to register in tracker
		while (totalNodesNumber != TrackerHelper.getRegistered()) {
			Thread.sleep(1000);
		}

		// --- PHASE 2: all nodes registered, so create genesis block and genesis.json files ---
		//update nodes from the tracker
		Map<Integer, Node> nodes = new HashMap<>(totalNodesNumber);
		TrackerHelper.updateNodes(nodes);
		final Block genesisBlock = TendermintHelper.generateGenesisBlock(totalNodesNumber, 1000, nodes);

		//generate genesis.json for all local nodes
		TendermintHelper.generateGenesisFiles(
				Simulation.TENDERMINT_NODES_FOLDER, new Date(),
				generatePublicKeysMap(nodes),
				genesisBlock.getHash().getBytes(), nodeNumbersToRunLocally);

		// --- PHASE 3: start the actual simulation ---
		Simulation simulation = new Simulation();
		ITransactionPattern itp = new RandomTransactionPattern(10, 20, 1000, 2000, 1);
		simulation.setTransactionPattern(itp);
		
		simulation.runNodesLocally(nodeNumbersToRunLocally, nodes, ownNodes, genesisBlock, Simulation.TENDERMINT_NODES_FOLDER, nodeToKeyPair);
		
		Thread.sleep(5000);
		simulation.initialize();
		
		Thread.sleep(5000);
		simulation.start();
		
		Thread.sleep(60 * 1000);
		simulation.stop();

		Thread.sleep(10 * 1000);
		
		simulation.stopLocalNodes();
		simulation.cleanup();
	}

	private static Map<Integer,String> generatePublicKeysMap(Map<Integer, Node> nodes) {
		Map<Integer, String> ret = new HashMap<>(nodes.size());
		for (Integer i: nodes.keySet()) {
			ret.put(i, Utils.bytesToHexString(nodes.get(i).getPublicKey()));
		}
		return ret;
	}

	private static Map<Integer, byte[]> convertToPublicKeys(Map<Integer, Ed25519Key> nodeToKeyPairMap){
		Map<Integer, byte[]> publicKeys = new HashMap<>(nodeToKeyPairMap.size());
		for (Map.Entry<Integer, Ed25519Key> e : nodeToKeyPairMap.entrySet()) {
			publicKeys.put(e.getKey(), e.getValue().getPublicKey());
		}
		return publicKeys;
	}
}
