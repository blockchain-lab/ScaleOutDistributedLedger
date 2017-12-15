package nl.tudelft.blockchain.scaleoutdistributedledger;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for interacting with the tracker.
 */
public final class TrackerHelper {
	private TrackerHelper() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Registers this node with the given public key.
	 * @param publicKey - the public key to register with
	 * @return            the assigned id
	 */
	public static int registerNode(byte[] publicKey) throws IOException {
		JSONObject json = new JSONObject();
		json.put("address", Application.TRACKER_SERVER_ADDRESS);
		json.put("port", Application.TRACKER_SERVER_PORT);
		json.put("publicKey", publicKey);
		System.out.println(json.toString());
		HttpClient client = HttpClientBuilder.create().build();
		StringEntity requestEntity = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
		HttpPost request = new HttpPost(String.format("http://%s:%d/register-node", Application.TRACKER_SERVER_ADDRESS, Application.TRACKER_SERVER_PORT));
		request.setEntity(requestEntity);
		JSONObject response = new JSONObject(IOUtils.toString(client.execute(request).getEntity().getContent()));
		System.out.println(response.toString());

		//TODO Register with server, return node id
		return 0;
	}
	
	/**
	 * Updates the given map of nodes with new information from the tracker.
	 * @param nodes - the map of nodes
	 */
	public static void updateNodes(Map<Integer, Node> nodes) throws IOException {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(String.format("http://%s:%d", Application.TRACKER_SERVER_ADDRESS, Application.TRACKER_SERVER_PORT));
		JSONArray nodesArray = (JSONArray) new JSONObject(IOUtils.toString(client.execute(request).getEntity().getContent())).get("nodes");
		System.out.println(nodesArray.toString());

//		List<String> lines = downloadNodesList();
//		for (String line : lines) {
//			String[] parts = line.split(",");
//			if (parts.length != 3) continue;
//
//			int id = Integer.parseInt(parts[0]);
//
//			//We already know about this node
//			if (nodes.containsKey(id)) continue;
//
//			//Create a new node
//			byte[] publicKey = Utils.hexStringToBytes(parts[1]);
//			String address = parts[2];
//			Node node = new Node(id, publicKey, address);
//			nodes.put(id, node);
//		}
	}
	
	/**
	 * Downloads the list of nodes from the tracker.
	 * The format is {@code id,public key (hex string),address}.
	 * @return a list of strings where each string represents a single node
	 */
	public static List<String> downloadNodesList() {
		//TODO Actually get this info
		return new ArrayList<>();
	}
}
