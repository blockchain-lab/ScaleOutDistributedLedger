package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.api.ABCIAPI;
import com.github.jtendermint.jabci.types.Types.*;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.logging.Level;

/**
 * An implementation of a Tendermint ABCI server.
 * The server implements a number of callbacks for events received from Tendermint.
 * @see <a href="https://github.com/tendermint/abci#message-types">Tendermint ABCI message type documentation</a>
 */
public class ABCIServer implements ABCIAPI {
	private final TendermintChain chain;

	/**
	 * @param chain - the main chain this server is part of
	 */
	public ABCIServer(TendermintChain chain) {
		this.chain = chain;
	}

	@Override
	public ResponseBeginBlock requestBeginBlock(RequestBeginBlock requestBeginBlock) {
		Log.log(Level.FINE, "[TENDERMINT] New block started");
		return ResponseBeginBlock.newBuilder().build();
	}

	@Override
	public ResponseCheckTx requestCheckTx(RequestCheckTx requestCheckTx) {
		Log.log(Level.FINE, "[TENDERMINT] New transaction proposed");

		// Comment the next line when using a mock chain
		BlockAbstract abs = BlockAbstract.fromBytes(requestCheckTx.getTx().toByteArray());

		//TODO: validate the abstract
		boolean valid = true;
		if (valid) {
			return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build();
		} else {
			String log = "Description of what went wrong while validating";
			Log.log(Level.INFO, "[TENDERMINT] Proposed block rejected because: " + log);
			return ResponseCheckTx.newBuilder().setCode(CodeType.BadNonce).setLog(log).build();
		}
	}

	@Override
	public ResponseCommit requestCommit(RequestCommit requestCommit) {
		Log.log(Level.FINE, "[TENDERMINT] Finalize commit request");
		return ResponseCommit.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseDeliverTx receivedDeliverTx(RequestDeliverTx requestDeliverTx) {
		return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseEcho requestEcho(RequestEcho requestEcho) {
		Log.log(Level.FINE, "[TENDERMINT] Echo " + requestEcho.getMessage());
		return ResponseEcho.newBuilder().setMessage(requestEcho.getMessage()).build();
	}

	@Override
	public ResponseEndBlock requestEndBlock(RequestEndBlock requestEndBlock) {
		long height = requestEndBlock.getHeight();
		if (height > 0) {
			Log.log(Level.INFO, "[TENDERMINT] Block #" + height + " ended, going to update the cache");
			chain.updateCache(height);
		}

		return ResponseEndBlock.newBuilder().build();
	}

	@Override
	public ResponseFlush requestFlush(RequestFlush requestFlush) {
		return ResponseFlush.newBuilder().build();
	}

	@Override
	public ResponseInfo requestInfo(RequestInfo requestInfo) {
		return ResponseInfo.newBuilder().setData("OK").build();
	}

	@Override
	public ResponseInitChain requestInitChain(RequestInitChain requestInitChain) {
		return ResponseInitChain.newBuilder().build();
	}

	@Override
	public ResponseQuery requestQuery(RequestQuery requestQuery) {
		Log.log(Level.FINE, "[TENDERMINT] Chain queried");
		return ResponseQuery.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseSetOption requestSetOption(RequestSetOption requestSetOption) {
		return ResponseSetOption.newBuilder().build();
	}
}
