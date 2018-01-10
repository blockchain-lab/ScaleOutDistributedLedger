package nl.tudelft.blockchain.scaleoutdistributedledger.simulation;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.Application;
import nl.tudelft.blockchain.scaleoutdistributedledger.TrackerHelper;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StartTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.StopTransactingMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionPatternMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import lombok.Getter;

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
	
	/**
	 * Creates a new simulation.
	 */
	public Simulation() {
		this.socketClient = new SocketClient();
		this.state = SimulationState.STOPPED;
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
	 * @param amount - the number of nodes to run locally
	 * @throws IllegalStateException - if the state is not STOPPED.
	 */
	public void runNodesLocally(int amount) {
		if (state != SimulationState.STOPPED) throw new IllegalStateException("You can only start local nodes when the simulation is in the STOPPED state.");
		
		localApplications = new Application[amount];
		for (int i = 0; i < amount; i++) {
			Application app = new Application();
			try {
				app.init(Application.NODE_PORT + i, 46658);
			} catch (Exception ex) {
				Log.log(Level.SEVERE, "Unable to initialize local node " + i + " on port " + (Application.NODE_PORT + i) + "!", ex);
			}
			localApplications[i] = app;
		}
	}
	
	/**
	 * Stops all nodes that are running locally.
	 * @throws IllegalStateException - if the state is not STOPPED.
	 */
	public void stopLocalNodes() {
		if (state != SimulationState.STOPPED) throw new IllegalStateException("You can only stop local nodes when the simulation is in the STOPPED state.");
		if (localApplications == null) return;
		
		for (Application app : localApplications) {
			app.stop();
		}
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
		if (state != SimulationState.STOPPED) throw new IllegalStateException("You can only initialize when the simulation is in the STOPPED state.");
		if (transactionPattern == null) throw new NullPointerException("TransactionPattern must not be null!");
		
		getNodeListFromTracker();
		if (nodes.isEmpty()) {
			Log.log(Level.INFO, "[Simulation] No nodes found. Stopping simulation.");
			return;
		} else {
			Log.log(Level.INFO, "[Simulation] Tracker reported " + nodes.size() + " nodes.");
		}
		
		//Broadcast distributed transaction pattern
		if (transactionPattern.getSimulationMode() == SimulationMode.DISTRIBUTED) {
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
		if (state != SimulationState.INITIALIZED) throw new IllegalStateException("You can only start when the simulation is in the INIALIZED state.");
		
		if (transactionPattern.getSimulationMode() == SimulationMode.DISTRIBUTED) {
			broadcastMessage(new StartTransactingMessage());
		} else if (transactionPattern.getSimulationMode() == SimulationMode.DIRECTED) {
			Log.log(Level.INFO, "[Simulation] Starting directed simulation...");
			//TODO Directed simulation
		}
		
		Log.log(Level.INFO, "[Simulation] Running");
		state = SimulationState.RUNNING;
	}
	
	/**
	 * Stops the simulation.
	 * @throws IllegalStateException - if the state is not RUNNING.
	 */
	public void stop() {
		if (state != SimulationState.RUNNING) throw new IllegalStateException("You can only stop when the simulation is in the RUNNING state.");
		
		broadcastMessage(new StopTransactingMessage());
		Log.log(Level.INFO, "[Simulation] Stopped");
		state = SimulationState.STOPPED;
	}
	
	/**
	 * Cleans up the simulation.
	 * @throws IllegalStateException - if the state is RUNNING.
	 */
	public void cleanup() {
		if (state == SimulationState.RUNNING) throw new IllegalStateException("Cannot cleanup while still running!");
		
		this.nodes = null;
		//TODO Close socketClient?
		state = SimulationState.STOPPED;
	}
	
	/**
	 * Gets the node list from the tracker.
	 */
	protected void getNodeListFromTracker() {
		nodes = new HashMap<>();
		try {
			TrackerHelper.updateNodes(nodes);
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "[Simulation] Unable to get list of nodes from tracker!", ex);
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
				socketClient.sendMessage(node, msg);
			} catch (Exception ex) {
				Log.log(Level.SEVERE,
						"[Simulation] Failed to send message " + msg + " to node " + node.getId() +
						" at " + node.getAddress() + ":" + node.getPort(), ex);
			}
		}
	}
}
