package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.apache.http.client.fluent.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Level;

/**
 * An ABCI client for sending to the Tendermint chain.
 */
class ABCIClient {
	private final String addr;

	/**
	 * @param address - the address of the Tendermint node
	 */
	ABCIClient(String address) {
		this.addr = address;
	}

	/**
	 * Commit a block to Tendermint.
	 *
	 * @param abs - the abstract to commit
	 * @return - the hash of the block on the chain if successful, null otherwise
	 */
	byte[] commit(BlockAbstract abs) {
		JSONObject result = sendTx(abs.toBytes());
		if (result == null) return null;
		try {
			JSONObject error = result.getJSONObject("error");
			Log.log(Level.INFO, "Could not commit the abstract because: " + error.getString("data") + ".\n" + result.toString());
			return null;
		} catch (Exception e) { //could not find the 'error' in JSON, result was OK.
			Log.log(Level.FINER, "No error found: ", e);
		}
		try {
			JSONObject resultField = result.getJSONObject("result");
			if (resultField.getInt("code") == 0) {
				return Utils.hexStringToBytes(resultField.getString("hash"));
			} else {
				return null;
			}
		} catch (Exception e) {		// Malformed result
			Log.log(Level.WARNING, "Result parsing failed, result of sending was: \n" + result.toString(), e);
			return null;
		}
	}

	/**
	 * Query Tendermint for the presence of a transaction.
	 *
	 * @param hash - the hash of the transaction
	 * @return - true when the block is present, false otherwise
	 */
	boolean query(Sha256Hash hash) {
		//TODO: Verify that the abstractHash of the abstract is the correct hash
		JSONObject result = sendQuery(hash.getBytes());
		return result != null && result.has("result");
	}

	/**
	 * Send a transaction to Tendermint.
	 *
	 * @param data - the byte array containing the tx data
	 * @return - the JSON response
	 */
	private JSONObject sendTx(byte[] data) {
		return sendRequest("broadcast_tx_sync", new String[]{"tx=0x" + Utils.bytesToHexString(data)});
	}

	/**
	 * Send a query to Tendermint.
	 *
	 * @param hash - the hash to query for
	 * @return - the JSON response
	 */
	private JSONObject sendQuery(byte[] hash) {
		return sendRequest("tx", new String[]{"hash=0x" + Utils.bytesToHexString(hash)});
	}


	/**
	 * Send a request to an endpoint and return the JSON response.
	 *
	 * @param endpoint - the endpoint to connect to
	 * @param args - the args passed along with the request
	 * @return - the JSON response, or null when the response was invalid JSON
	 */
	private JSONObject sendRequest(String endpoint, String[] args) {
		try {
			StringBuilder str = new StringBuilder("http://" + addr + "/" + endpoint);
			for (String arg : args) {
				str.append('?').append(arg);
			}
			return new JSONObject(Request.Get(str.toString()).execute().returnContent().toString());
		} catch (IOException | JSONException e) {
			e.printStackTrace();
			return null;
		}
	}
}
