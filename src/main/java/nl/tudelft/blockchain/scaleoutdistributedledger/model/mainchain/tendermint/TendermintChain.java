package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.socket.TSocket;
import lombok.SneakyThrows;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.logging.Level;

public final class TendermintChain implements MainChain {
	private ABCIServer handler;
	private ABCIClient client;
	private TSocket socket;

	/**
	 * Create and start the connection with Tendermint on the given address.
	 *
	 * @param addr - the address to connect to
	 */
	public TendermintChain(String addr) {
		System.out.println("Starting Tendermint cahin on " + addr);
		socket = new TSocket();
		handler = new ABCIServer();
		client = new ABCIClient(addr);

		socket.registerListener(handler);

		Thread t = new Thread(socket::start);
		t.setName("Main Chain Socket");
		t.start();
	}

	/**
	 * Stop the connection to Tendermint.
	 * Used for testing.
	 */
	protected void stop() {
		socket.stop();
	}

	@SneakyThrows //TODO: remove this method
	public static void main(String[] args) {
		TendermintChain tmchain = new TendermintChain("http://localhost:46657");
		tmchain.isPresent(null);
		while (true) {
			Thread.sleep(1000);
		}
	}

	@Override
	public Sha256Hash commitAbstract(BlockAbstract abs) {
		byte[] hash = client.commit(abs);
		if(hash == null) {
			Log.log(Level.WARNING, "Tendermint [COMMIT] failed");
			return null;
		} else {
			return Sha256Hash.withHash(hash);
		}
	}

	@Override
	public boolean isPresent(BlockAbstract abs) {
		return client.query(null);
	}

	@Override
	public void connectTo(Node node) {
		if (!client.connect(node.getAddress())) {
			Log.log(Level.WARNING, "Tendermint failed to connect to peer " + node.getAddress());
		}
	}
}
