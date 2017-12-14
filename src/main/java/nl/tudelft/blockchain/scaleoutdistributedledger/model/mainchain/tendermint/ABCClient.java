package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import org.apache.http.client.fluent.Request;

import java.io.IOException;

public class ABCClient {
	private final String addr;

	public ABCClient(String adrr) {
		this.addr = adrr;
	}

	public boolean commit(BlockAbstract abs) {
		byte[] result = sendTx(abs.toBytes());
		if (result == null) return false;

		//TODO: parse the result
		System.out.println(result);
		return true;
	}

	private byte[] sendTx(byte[] data) {
		try {
			return Request.Get(addr + "broadcast_tx_sync?tx=%22" + data + "%22").execute().returnContent().asBytes();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
