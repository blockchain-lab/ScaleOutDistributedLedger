package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import org.apache.http.client.fluent.Request;
import org.json.JSONObject;

import java.io.IOException;

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
			JSONObject resultField = result.getJSONObject("result");
			if (resultField.getInt("code") == 0) {
				return Utils.hexStringToBytes(resultField.getString("hash"));
			} else {
				return null;
			}
		} catch (Exception e) {		// Malformed result
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
		JSONObject result = sendQuery(hash.getBytes());
		return result != null && result.has("result");
	}

	/**
	 * Connect with another peer.
	 *
	 * @param address - the address of the peer
	 * @return - true when the connection was successfully made, false otherwise
	 */
	boolean connect(String address) {
		JSONObject result = sendConnect(address);
		if (result == null) return false;

		//TODO: Check response
		return false;
	}

	/**
	 * Send a transaction to Tendermint.
	 *
	 * @param data - the byte array containing the tx data
	 * @return - the JSON response
	 */
	private JSONObject sendTx(byte[] data) {
		try {
			return new JSONObject(Request.Get(addr + "/broadcast_tx_sync?tx=" + Utils.bytesToHexString(data)).execute().returnContent());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Send a query to Tendermint.
	 *
	 * @param hash - the hash to query for
	 * @return - the JSON response
	 */
	private JSONObject sendQuery(byte[] hash) {
		try {
			return new JSONObject(Request.Get(addr + "tx?hash=" + Utils.bytesToHexString(hash)).execute().returnContent());

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Send a connect message to Tendermint.
	 *
	 * @param address - the address to connect to
	 * @return - the JSON response
	 */
	JSONObject sendConnect(String address) {
		//TODO: this endpoint seems to be removed
//		try {
//			return new JSONObject(Request.Get(addr + "/dial_seeds?seeds=%22" + address + %22").execute().returnContent());
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return null;
	}
}
