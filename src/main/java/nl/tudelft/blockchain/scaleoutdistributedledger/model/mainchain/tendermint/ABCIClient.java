package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import org.apache.http.client.fluent.Request;
import org.json.JSONObject;

import java.io.IOException;

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

		try {
			JSONObject resultField = result.getJSONObject("result");
			if (resultField.getInt("code") == 0) {
				return hexStringToByteArray(resultField.getString("hash"));
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
	public boolean query(Sha256Hash hash) {
		JSONObject result = sendQuery(hash.getBytes());
		return result != null && result.has("result");
	}

	/**
	 * Connect with another peer.
	 *
	 * @param address - the address of the peer
	 * @return - true when the connection was successfully made, false otherwise
	 */
	public boolean connect(String address) {
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
			return new JSONObject(Request.Get(addr + "/broadcast_tx_sync?tx=%22" + data + "%22").execute().returnContent());

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
		String data = byteArrayToHexString(hash);
		try {
			return new JSONObject(Request.Get(addr + "tx?hash=" + data).execute().returnContent());

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
	public JSONObject sendConnect(String address) {
		//TODO: this endpoint seems to be removed
//		try {
//			return new JSONObject(Request.Get(addr + "/dial_seeds?seeds=%22" + address + %22").execute().returnContent());
//
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
		return null;
	}

	/**
	 * Convert a hex string ('0x123EFF' or '123EFF') to a byte array.
	 *
	 * @param string - the string to parse
	 * @return - the same value, but as a byte array
	 */
	public byte[] hexStringToByteArray(String string) {
		//TODO: @Karol Do you know a better way to do this?
		String s = string.replaceFirst("0x", "");	// Remove 0x prefix
		byte[] b = new byte[s.length() / 2];
		for (int i = 0; i < b.length; i++) {
			int index = i * 2;
			int v = Integer.parseInt(s.substring(index, index + 2), 16);
			b[i] = (byte) v;
		}
		return b;
	}

	/**
	 * Convert a byte array to a hex string prefixed by 0x.
	 *
	 * @param bytes - the array to convert
	 * @return - the converted string
	 */
	public String byteArrayToHexString(byte[] bytes) {
		//TODO: @Karol Do you know a better way to do this?
		final char[] hexArray = "0123456789ABCDEF".toCharArray();
		char[] hexChars = new char[bytes.length * 2];
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return "0x" + new String(hexChars);
	}

}
