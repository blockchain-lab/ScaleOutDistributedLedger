package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.api.ABCIAPI;
import com.github.jtendermint.jabci.types.Types;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;

/**
 *
 * @see <a href="https://github.com/tendermint/abci#message-types">Tendermint ABCI message type documentation</a>
 */
public class ABCIServer implements ABCIAPI {
    @Override
    public Types.ResponseBeginBlock requestBeginBlock(Types.RequestBeginBlock requestBeginBlock) {
        return null;
        //TODO: Implement this
    }

    /**
     * Validate a transaction.
     *
     * @param requestCheckTx
     * @return
     */
    @Override
    public Types.ResponseCheckTx requestCheckTx(Types.RequestCheckTx requestCheckTx) {
        BlockAbstract abs = BlockAbstract.fromBytes(requestCheckTx.getTx().toByteArray());

        //TODO: validate the abstract
        boolean valid = true;
        if(valid) {
            return Types.ResponseCheckTx.newBuilder().setCode(Types.CodeType.OK).build();
        } else {
            String log = "Discription of what went wrong while validating";
            return Types.ResponseCheckTx.newBuilder().setCode(Types.CodeType.BadNonce).setLog(log).build();
        }
    }

    @Override
    public Types.ResponseCommit requestCommit(Types.RequestCommit requestCommit) {
        return null;
    }

    @Override
    public Types.ResponseDeliverTx receivedDeliverTx(Types.RequestDeliverTx requestDeliverTx) {
        return null;
    }

    @Override
    public Types.ResponseEcho requestEcho(Types.RequestEcho requestEcho) {
        return null;
    }

    @Override
    public Types.ResponseEndBlock requestEndBlock(Types.RequestEndBlock requestEndBlock) {
        return null;
    }

    @Override
    public Types.ResponseFlush requestFlush(Types.RequestFlush requestFlush) {
        return null;
    }

    @Override
    public Types.ResponseInfo requestInfo(Types.RequestInfo requestInfo) {
        return null;
    }

    @Override
    public Types.ResponseInitChain requestInitChain(Types.RequestInitChain requestInitChain) {
        return null;
    }

    @Override
    public Types.ResponseQuery requestQuery(Types.RequestQuery requestQuery) {
        return null;
    }

    @Override
    public Types.ResponseSetOption requestSetOption(Types.RequestSetOption requestSetOption) {
        return null;
    }
}
