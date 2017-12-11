package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.socket.TSocket;
import lombok.SneakyThrows;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;

public final class TendermintChain implements MainChain {
    private ABCIServer handler;
    private ABCClient client;
    private TSocket socket;

    public TendermintChain() {
        System.out.println("starting Tendermint cahin");
        socket = new TSocket();
        handler = new ABCIServer();

        socket.registerListener(handler);

        Thread t = new Thread(socket::start);
        t.setName("Main Chain Socket");
        t.start();
    }
    @Override
    public void commitAbstract(BlockAbstract abs) {
        client.commit(abs);
    }

    @Override
    public boolean isPresent(BlockAbstract abs) {
        return false;
    }

    @SneakyThrows
    public static void main(String[] args) {
        TendermintChain tmchain = new TendermintChain();
        while(true) {
            Thread.sleep(1000);
        }
    }
}
