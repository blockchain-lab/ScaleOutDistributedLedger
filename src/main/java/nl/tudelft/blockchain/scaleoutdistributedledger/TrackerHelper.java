package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.exceptions.NodeRegisterFailedException;
import nl.tudelft.blockchain.scaleoutdistributedledger.exceptions.TrackerException;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.OwnNode;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Proof;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.TransactionRegistration;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

/**
 * Helper class for interacting with the tracker.
 */
public final class TrackerHelper {
	private static final String TRACKER_URL = String.format("http://%s:%d", Application.TRACKER_SERVER_ADDRESS, Application.TRACKER_SERVER_PORT);

	private static volatile Queue<TransactionRegistration> transactionsToRegister = new LinkedBlockingQueue<>();
	private static final CloseableHttpClient CLIENT = HttpClientBuilder.create().build();
	private static final ExecutorService POOL = Executors.newSingleThreadExecutor();

	private TrackerHelper() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get the status of the tracker.
	 * @return - a JSON object describing the status of the tracker
	 * @throws IOException - if the connection to the tracker server cannot be made
	 */
	public static JSONObject getStatus() throws IOException {
		return getToTracker("/status");
	}

	/**
	 * Get the number of running nodes from the tracker.
	 * @return - the number of nodes already registered in tracker
	 * @throws IOException - if the connection to the tracker server cannot be made
	 */
	public static int getRunning() throws IOException {
		return getStatus().getInt("running");
	}
	
	/**
	 * Mark a node with the given id as initialized on the tracker.
	 * @param id - the id of the node to mark
	 * @param running - if the node is running or not
	 * @throws IOException - IOException while registering node
	 * @throws TrackerException - Server side exception while updating running status
	 */
	public static void setRunning(int id, boolean running) throws IOException, TrackerException {
		JSONObject json = new JSONObject();
		json.put("id", id);
		json.put("running", running);

		if (postToTracker("/set-node-status", json)) {
			Log.log(Level.INFO, "Successfully updated node " + id + " to running=" + running);
			return;
		}

		Log.log(Level.SEVERE, "Error while updating the running status of the node");
		throw new TrackerException("Unable to update to running.");
	}

	/**
	 * Get the number of registered nodes from the  tracker.
	 * @return - the number of nodes already registered in tracker
	 * @throws IOException - if the connection to the tracker server cannot be made
	 */
	public static int getRegistered() throws IOException {
		return getStatus().getInt("registered");
	}
	
	/**
	 * Registers this node with the given public key.
	 * @param nodePort  - the port of the node
	 * @param publicKey - the publicKey of the new node
	 * @param id        - the id of the node
	 * @return          - the registered node
	 * @throws IOException - if the connection to the tracker server cannot be made
	 * @throws NodeRegisterFailedException - Server side exception while registering node
	 */
	public static OwnNode registerNode(int nodePort, byte[] publicKey, int id) throws IOException, NodeRegisterFailedException {
		String address = getIP();
		JSONObject json = new JSONObject();
		json.put("address", address);
		json.put("port", nodePort);
		json.put("publicKey", publicKey);
		json.put("id", id);

		if (postToTracker("/register-node", json)) {
			Log.log(Level.INFO, "Successfully registered node to tracker server");
			return new OwnNode(id, publicKey, address, nodePort);
		}

		Log.log(Level.SEVERE, "Error while registering node");
		throw new NodeRegisterFailedException();
	}

	/**
	 * Reset the tracker server with a new empty nodelist.
	 * @return - boolean identifying if the reset was successful
	 * @throws IOException - if the connection to the tracker server cannot be made
	 */
	public static boolean resetTrackerServer() throws IOException {
		if (postToTracker("/reset", null)) {
			Log.log(Level.INFO, "Successfully reset the tracker server");
			return true;
		}

		Log.log(Level.SEVERE, "Error while resetting the tracker server");
		return false;
	}
	
	/**
	 * Gets the nodes that are registered with the tracker.
	 * @return - the nodes that are registered with the tracker.
	 * @throws IOException - exception while getting nodes
	 */
	public static Map<Integer, Node> getNodes() throws IOException {
		return updateNodes(new HashMap<>(), null);
	}

	/**
	 * Updates the given map of nodes with new information from the tracker.
	 * @param nodes   - the map of nodes
	 * @param ownNode - our own node (or null)
	 * @return        - the given map
	 * @throws IOException - exception while updating nodes
	 */
	public static Map<Integer, Node> updateNodes(Map<Integer, Node> nodes, OwnNode ownNode) throws IOException {
		JSONArray nodesArray = getToTracker("/").getJSONArray("nodes");

		for (int i = 0; i < nodesArray.length(); i++) {
			JSONObject object = nodesArray.getJSONObject(i);
			byte[] publicKey = jsonArrayToByteArray(object.getJSONArray("publicKey"));
			String address = object.getString("address");
			int port = object.getInt("port");
			if (nodes.containsKey(i)) {
				Node node = nodes.get(i);
				node.setAddress(address);
				node.setPort(port);
			} else {
				Node node = new Node(i, publicKey, address, port);
				nodes.put(i, node);
			}
		}
		
		return nodes;
	}

	/**
	 * Registers a transaction to a queue, ready to be send to the server.
	 * @param proof - the proof used to send the transaction.
	 */
	public static void registerTransaction(Proof proof) {
		transactionsToRegister.add(new TransactionRegistration(proof.getTransaction(), proof.getChainUpdates().size(), proof.getNumberOfBlocks()));

		//Check if we should send both before and in the synchronized block, to prevent blocking.
		if (transactionsToRegister.size() < SimulationMain.REGISTER_TRANSACTIONS_EVERY) return;

		synchronized (TrackerHelper.class) {
			if (transactionsToRegister.size() < SimulationMain.REGISTER_TRANSACTIONS_EVERY) return;

			//Swap the queue out with a new one
			Queue<TransactionRegistration> toSend = transactionsToRegister;
			transactionsToRegister = new LinkedBlockingQueue<>();

			//Send the transactions
			POOL.submit(() -> {
				try {
					sendTransactions(toSend);
				} catch (IOException e) {
					Log.log(Level.WARNING, "Transaction registration failed", e);
				}
			});
		}
	}

	/**
	 * Sends the given transactions to the server.
	 * @param toSend - the transactions to register
	 * @throws IOException - exception while sending.
	 */
	private static void sendTransactions(Queue<TransactionRegistration> toSend) throws IOException {
		JSONArray transactionArray = new JSONArray();

		for (TransactionRegistration next : toSend) {
			JSONObject json = new JSONObject();
			json.put("from", next.getTransaction().getSender().getId());
			json.put("to", next.getTransaction().getReceiver().getId());
			json.put("amount", next.getTransaction().getAmount());
			json.put("remainder", next.getTransaction().getRemainder());
			json.put("numberOfChains", next.getNumberOfChains());
			json.put("numberOfBlocks", next.getNumberOfBlocks());
			transactionArray.put(json);
		}
		JSONObject json = new JSONObject();
		json.put("transactions", transactionArray);

		StringEntity requestEntity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
		HttpPost request = new HttpPost(TRACKER_URL + "/register-transactions");
		request.setEntity(requestEntity);
		JSONObject response = new JSONObject(IOUtils.toString(CLIENT.execute(request).getEntity().getContent()));
		if (response.getBoolean("success")) {
			Log.log(Level.FINE, "Successfully registered " + transactionArray.length() + " transactions to tracker server");
		} else {
			Log.log(Level.WARNING, "Error while registering transactions");
		}
	}

	/**
	 * Makes a POST request to the tracker server and returns its success.
	 * @param endPoint     - the endpoint to post to
	 * @param json         - the json to send (can be null)
	 * @return             - true if the json response had success = true, false otherwise
	 * @throws IOException - If there was an error while connecting to the tracker server.
	 */
	public static boolean postToTracker(String endPoint, JSONObject json) throws IOException {
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpPost request = new HttpPost(TRACKER_URL + endPoint);

			if (json != null) {
				StringEntity requestEntity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
				request.setEntity(requestEntity);
			}

			JSONObject response = new JSONObject(IOUtils.toString(client.execute(request).getEntity().getContent()));
			return response.getBoolean("success");
		}
	}

	/**
	 * Makes a GET request to the tracker server and returns the result as a JSONObject.
	 * @param endPoint     - the endpoint to get
	 * @return             - the json returned by the tracker
	 * @throws IOException - If there was an error while connecting to the tracker server.
	 */
	public static JSONObject getToTracker(String endPoint) throws IOException {
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpGet request = new HttpGet(TRACKER_URL + endPoint);
			return new JSONObject(IOUtils.toString(client.execute(request).getEntity().getContent()));
		}
	}

	/**
	 * Tries to resolve the IP(v4) address of this machine.
	 * When it fails to do so it uses the local IP.
	 *
	 * @return the IP of this machine
	 */
	public static String getIP() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while (addresses.hasMoreElements()) {
					String address = addresses.nextElement().getHostAddress();
					//IPv6, Local or docker
					if (address.contains(":") || address.startsWith("127.") || address.startsWith("172.")) continue;
					return address;
				}
			}
		} catch (SocketException e) {
			// Intentionally empty catch block
		}

		try {
			Log.log(Level.WARNING, "Could not resolve address, using localhost instead");
			return InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			Log.log(Level.SEVERE, "Could not resolve localhost address, please check your network configuration");
			return "0.0.0.0";
		}
	}

	/**
	 * Converts JSONArray containing ints to byte array.
	 * @param json - the jsonarray to convert.
	 * @return - the generated byte array
	 */
	private static byte[] jsonArrayToByteArray(JSONArray json) {
		byte[] res = new byte[json.length()];
		for (int i = 0; i < json.length(); i++) {
			res[i] = (byte) json.getInt(i);
		}
		return res;
	}
}
