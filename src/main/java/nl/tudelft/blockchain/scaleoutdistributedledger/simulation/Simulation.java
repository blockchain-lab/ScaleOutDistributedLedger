package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.TrackerHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StartTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StopTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionPatternMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.tendermint.TendermintHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Class for simulations.
 */
public class Simulation {


	@Getter
	private ITransactionPattern transactionPattern;
	
	@Getter
	private SimulationState state;
	
	@Getter
	private Map<Integer, Node> nodes;
	
	private Application[] localApplications;
	private final SocketClient socketClient;
	private final boolean isMaster;
	
	/**
	 * Creates a new simulation.
	 */
	public Simulation(boolean isMaster) {
		this.socketClient = new SocketClient();
		this.state = SimulationState.STOPPED;
		this.isMaster = isMaster;
	}
	
	/**
	 * @param pattern - the new transaction pattern
	 * @throws NullPointerException - if pattern is null.
	 */
	public void setTransactionPattern(ITransactionPattern pattern) {
		if (pattern == null) throw new NullPointerException("Pattern must not be null!");
		this.transactionPattern = pattern;
	}
	
	/**
	 * Runs the given amount of nodes locally, in the current JVM process.
	 *
	 * @param nodeNumbers - list of numbers of nodes to run locally
	 * @param nodes - the list of nodes retrieved from tracker server
	 * @param ownNodes - the list of own nodes registered to tracker server on this instance
	 * @param genesisBlock - the genesis block of the main chain
	 * @param nodeToKeyPair - the map of own nodes numbers to their private keys
	 * @throws IllegalStateException - if the state is not STOPPED.
	 */
	public void runNodesLocally(List<Integer> nodeNumbers, Map<Integer, Node> nodes, Map<Integer, OwnNode> ownNodes,
								Block genesisBlock, Map<Integer, Ed25519Key> nodeToKeyPair) {
		checkState(SimulationState.STOPPED, "start local nodes");

		this.nodes = nodes;
		//Init the applications
		localApplications = new Application[nodeNumbers.size()];
		Map<Integer, String> nodeAddresses = reduceToNodeAddresses(nodes);
		Map<Integer, Integer> nodePorts = reduceToNodePorts(nodes);
		int counter = 0;
		for (Integer nodeNumber : nodeNumbers) {
			Application app = new Application(true);
			List<String> addressesForThisNode = generateAddressesForNodeForTendermintP2P(nodeNumber, nodeAddresses, nodePorts);
			int port = nodePorts.get(nodeNumber);
			Map<Integer, Node> nodesForThisNode = new HashMap<>(nodes);
			nodesForThisNode.remove(nodeNumber);

			//TODO: fix this dirty hack
			//The ownNodes and nodePort maps do not have the same port numbers for each node, so just override one to fix it.
			ownNodes.get(nodeNumber).setPort(port);

			try {
				TendermintHelper.runTendermintNode(nodePorts.get(nodeNumber), addressesForThisNode, nodeNumber);
				app.init(port, genesisBlock.clone(), nodesForThisNode, nodeToKeyPair.get(nodeNumber), ownNodes.get(nodeNumber));
				TrackerHelper.setRunning(nodeNumber, true);
			} catch (Exception ex) {
				Log.log(Level.SEVERE, "Unable to initialize local node " + nodeNumber + " on port " + port + "!", ex);
			}

			localApplications[counter++] = app;
		}
	}

	private List<String> generateAddressesForNodeForTendermintP2P(Integer i, Map<Integer, String> nodeAddresses, Map<Integer, Integer> nodePorts) {
		List<String> ret = new ArrayList<>(nodeAddresses.size() - 1);

		for (Map.Entry<Integer, String> e : nodeAddresses.entrySet()) {
			String addressWithPort = "";
			int curNodeNumber = e.getKey();
			if (curNodeNumber != i) {
				addressWithPort = nodeAddresses.get(curNodeNumber) + ":" + Integer.toString(nodePorts.get(curNodeNumber) + 1);
			}
			ret.add(addressWithPort.toString());
		}
		return ret;
	}

	private Map<Integer, Integer> reduceToNodePorts(Map<Integer, Node> nodes) {
		Map<Integer, Integer> ret = new HashMap<>(nodes.size());
		for (Map.Entry<Integer, Node> e : nodes.entrySet()) {
			ret.put(e.getKey(), e.getValue().getPort());
		}
		return ret;
	}

	private Map<Integer, String> reduceToNodeAddresses(Map<Integer, Node> nodes) {
		Map<Integer, String> ret = new HashMap<>(nodes.size());
		for (Map.Entry<Integer, Node> e : nodes.entrySet()) {
			ret.put(e.getKey(), e.getValue().getAddress());
		}
		return ret;
	}

	/**
	 * Stops all nodes that are running locally.
	 * @throws IllegalStateException - if the state is not STOPPED.
	 */
	public void stopLocalNodes() {
		checkState(SimulationState.STOPPED, "stop local nodes");
		if (localApplications == null) return;

		int sum = 0;
		for (Application app : localApplications) {
			app.stop();
			Log.log(Level.INFO, String.format("Node %d has a final amount of %d moneyz.",
					app.getLocalStore().getOwnNode().getId(), app.getLocalStore().getAvailableMoney()));
			sum += app.getLocalStore().getAvailableMoney();
		}
		Log.log(Level.INFO, String.format("Total amount of moneyz left in the system is %d.", sum));
	}
	
	/**
	 * Initializes the simulation.
	 * 
	 * This method first gets the node list from the tracker and then sends the transaction pattern
	 * to all nodes.
	 * @throws IllegalStateException - if the state is not STOPPED.
	 * @throws NullPointerException  - if the transaction pattern has not been set.
	 */
	public void initialize() {
		checkState(SimulationState.STOPPED, "initialize");
		if (transactionPattern == null) throw new NullPointerException("TransactionPattern must not be null!");

		if (nodes.isEmpty()) {
			Log.log(Level.INFO, "[Simulation] No nodes found. Stopping simulation.");
			return;
		} else {
			Log.log(Level.INFO, "[Simulation] Initializing with " + nodes.size() + " nodes.");
		}
		
		//Broadcast distributed transaction pattern
		if (this.isMaster && transactionPattern.getSimulationMode() == SimulationMode.DISTRIBUTED) {
			broadcastMessage(new TransactionPatternMessage(transactionPattern));
		}
		
		Log.log(Level.INFO, "[Simulation] Initialized");
		state = SimulationState.INITIALIZED;
	}
	
	/**
	 * Starts the simulation.
	 * 
	 * This method sends a "Start transacting" message to all nodes.
	 * @throws IllegalStateException - if the state is not INITIALIZED.
	 */
	public void start() {
		checkState(SimulationState.INITIALIZED, "start");

		if (isMaster) {
			if (transactionPattern.getSimulationMode() == SimulationMode.DISTRIBUTED) {
				broadcastMessage(new StartTransactingMessage());
			} else if (transactionPattern.getSimulationMode() == SimulationMode.DIRECTED) {
				Log.log(Level.INFO, "[Simulation] Starting directed simulation...");
				//TODO Directed simulation
			}
		}
		Log.log(Level.INFO, "[Simulation] Running");
		state = SimulationState.RUNNING;
	}
	
	/**
	 * Stops the simulation.
	 * @throws IllegalStateException - if the state is not RUNNING.
	 * @param nodes
	 */
	public void stop(Map<Integer, OwnNode> nodes) {
		checkState(SimulationState.RUNNING, "stop");
		
		if (isMaster) {
			broadcastMessage(new StopTransactingMessage());
		}

		Log.log(Level.INFO, "[Simulation] Stopped");
		state = SimulationState.STOPPED;

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {	}

		for (Integer nodeNumber : nodes.keySet()) {
			try {
				TrackerHelper.setRunning(nodeNumber, false);
			} catch (IOException e) {
				Log.log(Level.SEVERE, "Cannot unregister node " + nodeNumber);
			}
		}
	}
	
	/**
	 * Cleans up the simulation.
	 * @throws IllegalStateException - if the state is RUNNING.
	 */
	public void cleanup() {
		if (state == SimulationState.RUNNING) throw new IllegalStateException("Cannot cleanup while still running!");
		
		this.nodes = null;
		state = SimulationState.STOPPED;
	}
	
	/**
	 * Checks if the current state is the expected state.
	 * @param expected - the expected state
	 * @param msg      - the message
	 * @throws IllegalStateException - If the current state is not the expected state.
	 */
	protected void checkState(SimulationState expected, String msg) {
		if (state != expected) {
			throw new IllegalStateException("You can only " + msg + " when the simulation is in the " + expected.name() + " state.");
		}
	}

	
	/**
	 * Sends the given message to all nodes.
	 * @param msg - the message to send
	 */
	protected void broadcastMessage(Message msg) {
		Log.log(Level.INFO, "[Simulation] Sending " + msg + " to all nodes...");
		
		for (Node node : nodes.values()) {
			try {
				if (!socketClient.sendMessage(node, msg)) {
					Log.log(Level.SEVERE,
							"Failed to send message " + msg + " to node " + node.getId() +
							" at " + node.getAddress() + ":" + node.getPort());
				}
			} catch (Exception ex) {
				Log.log(Level.SEVERE,
						"[Simulation] Failed to send message " + msg + " to node " + node.getId() +
						" at " + node.getAddress() + ":" + node.getPort(), ex);
			}
		}
	}
}
