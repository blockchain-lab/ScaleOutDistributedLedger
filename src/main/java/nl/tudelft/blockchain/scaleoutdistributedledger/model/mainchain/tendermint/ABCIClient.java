package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.apache.http.client.fluent.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * An ABCI client for sending to the Tendermint chain.
 */
public class ABCIClient {
	private final String addr;

	/**
	 * @param address - the address of the Tendermint node
	 */
	public ABCIClient(String address) {
		this.addr = address;
	}

	/**
	 * Commit a block to Tendermint.
	 *
	 * @param abs - the abstract to commit
	 * @return - the hash of the block on the chain if successful, null otherwise
	 */
	public byte[] commit(BlockAbstract abs) {
		JSONObject result = sendTx(abs.toBytes());
		if (result == null) return null;
		JSONObject error;
		if ((error = getError(result)) != null) {
			Log.log(Level.INFO, "Could not commit the abstract because: " + error.getString("data"));
			Log.log(Level.FINE, result.toString());
			return null;
		} else { //No error coming from Tendermint found
			byte[] ret = null;
			try {
				JSONObject resultField = result.getJSONObject("result");
				if (resultField.getInt("code") == 0) { //double check we succeeded
					ret = Utils.hexStringToBytes(resultField.getString("hash"));
				}
			} catch (Exception e) {		// Malformed result
				Log.log(Level.WARNING, "Result parsing failed, result of sending was: \n" + result.toString(), e);
			}
			return ret;
		}
	}

	/**
	 * Query Tendermint for the presence of a transaction (main chain block).
	 *
	 * @param hash - the hash of the transaction (main chain block)
	 * @return - true when the block is present, false otherwise
	 */
	public boolean query(Sha256Hash hash) {
		//TODO: Verify that the abstractHash of the abstract is the correct hash
		JSONObject result = sendQuery(hash.getBytes());
		return result != null && result.has("result");
	}

	/**
	 * Query the main chain for blocks on a given height.
	 *
	 * @param height - The height to look at
	 * @return - A list of blocks at the given height, or an empty list if the height was invalid
	 */
	public List<BlockAbstract> query(long height) {
		List<BlockAbstract> abstracts = new ArrayList<>();

		JSONObject result = getBlockAt(height);

		// Height is not valid, so return
		if (getError(result) != null) {
			Log.log(Level.INFO, "Error while querying for height");
			return abstracts;
		}

		try {
			JSONArray bytes = result.getJSONObject("result").getJSONObject("block").getJSONObject("data").getJSONArray("txs");
			for (Object obj : bytes) {
				abstracts.add(BlockAbstract.fromBytes(Utils.base64StringToBytes((String) obj)));

				// Use the following lines when testing on a mock chain that does not contain actual abstracts
// 				byte[] b = Utils.base64StringToBytes((String) obj);
//				abss.add(new BlockAbstract(0, 0, Sha256Hash.withHash(b), b));
			}
		} catch (Exception e) {
			Log.log(Level.WARNING, "Malformed result " + result.toString(1) + "\nCausing exception:", e);
		}
		return abstracts;
	}

	/**
	 * Get the status JSON object of the main chain.
	 *
	 * @return - A JSON object representing the status of the chain
	 */
	public JSONObject status() {
		JSONObject result = sendRequest("status", new HashMap<>());
		try {
			return result.getJSONObject("result");
		} catch (NullPointerException e) {
			Log.log(Level.SEVERE, "The main chain appears broken, this should not happen");
			return new JSONObject();
		}
	}

	/**
	 * Check whether HTTP response has an error coming from Tendermint (in JSON).
	 * @param obj http response
	 * @return error JSON object if has, null otherwise
	 */
	private JSONObject getError(JSONObject obj) {
		try {
			return obj.getJSONObject("error");
		} catch (Exception e) { //could not find the 'error' in JSON, result was OK.
			Log.log(Level.FINER, "No error found: ", e);
			return null;
		}
	}

	/**
	 * Send a transaction to Tendermint.
	 *
	 * @param data - the byte array containing the tx data
	 * @return - the JSON response
	 */
	private JSONObject sendTx(byte[] data) {
		Map<String, String> params = new HashMap<>();
		params.put("tx", "0x" + Utils.bytesToHexString(data));
		return sendRequest("broadcast_tx_commit", params);
	}

	/**
	 * Send a query to Tendermint.
	 *
	 * @param hash - the hash to query for
	 * @return - the JSON response
	 */
	private JSONObject sendQuery(byte[] hash) {
		Map<String, String> params = new HashMap<>();
		params.put("hash", "0x" + Utils.bytesToHexString(hash));
		return sendRequest("tx", params);
	}

	/**
	 * Send a query to Tendermint.
	 *
	 * @param height - the height to query at
	 * @return - the JSON response
	 */
	private JSONObject getBlockAt(long height) {
		Map<String, String> params = new HashMap<>();
		params.put("height", Long.toString(height));
		return sendRequest("block", params);
	}

	/**
	 * Send a request to an endpoint and return the JSON response.
	 *
	 * @param endpoint - the endpoint to connect to
	 * @param params - the params passed along with the request
	 * @return - the JSON response, or null when the response was invalid JSON
	 */
	private JSONObject sendRequest(String endpoint, Map<String, String> params) {
		try {
			StringBuilder str = new StringBuilder("http://" + addr + "/" + endpoint);

			Iterator<Map.Entry<String, String>> it = params.entrySet().iterator();
			if (it.hasNext()) {
				Map.Entry<String, String> param = it.next();
				str.append('?').append(param.getKey()).append('=').append(param.getValue());
			}
			while (it.hasNext()) {
				Map.Entry<String, String> param = it.next();
				str.append('&').append(param.getKey()).append('=').append(param.getValue());
			}

			return new JSONObject(Request.Get(str.toString()).execute().returnContent().toString());
		} catch (IOException | JSONException e) {
			Log.log(Level.INFO, "Failed executing request", e);
			return null;
		}
	}
}
