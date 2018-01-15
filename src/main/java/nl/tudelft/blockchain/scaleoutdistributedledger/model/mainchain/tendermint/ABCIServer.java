package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.api.ABCIAPI;
import com.github.jtendermint.jabci.types.Types;
import com.github.jtendermint.jabci.types.Types.CodeType;
import com.github.jtendermint.jabci.types.Types.RequestBeginBlock;
import com.github.jtendermint.jabci.types.Types.RequestCheckTx;
import com.github.jtendermint.jabci.types.Types.RequestCommit;
import com.github.jtendermint.jabci.types.Types.RequestDeliverTx;
import com.github.jtendermint.jabci.types.Types.RequestEcho;
import com.github.jtendermint.jabci.types.Types.RequestEndBlock;
import com.github.jtendermint.jabci.types.Types.RequestFlush;
import com.github.jtendermint.jabci.types.Types.RequestInfo;
import com.github.jtendermint.jabci.types.Types.RequestInitChain;
import com.github.jtendermint.jabci.types.Types.RequestQuery;
import com.github.jtendermint.jabci.types.Types.RequestSetOption;
import com.github.jtendermint.jabci.types.Types.ResponseBeginBlock;
import com.github.jtendermint.jabci.types.Types.ResponseCheckTx;
import com.github.jtendermint.jabci.types.Types.ResponseCommit;
import com.github.jtendermint.jabci.types.Types.ResponseDeliverTx;
import com.github.jtendermint.jabci.types.Types.ResponseEcho;
import com.github.jtendermint.jabci.types.Types.ResponseEndBlock;
import com.github.jtendermint.jabci.types.Types.ResponseFlush;
import com.github.jtendermint.jabci.types.Types.ResponseInfo;
import com.github.jtendermint.jabci.types.Types.ResponseInitChain;
import com.github.jtendermint.jabci.types.Types.ResponseQuery;
import com.github.jtendermint.jabci.types.Types.ResponseSetOption;
import com.google.protobuf.ByteString;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import java.util.logging.Level;

/**
 * An implementation of a Tendermint ABCI server.
 * The server implements a number of callbacks for events received from Tendermint.
 * @see <a href="https://github.com/tendermint/abci#message-types">Tendermint ABCI message type documentation</a>
 */
public class ABCIServer implements ABCIAPI {
	private final TendermintChain chain;
	private final Block genesisBlock;
	/**
	 * @param chain - the main chain this server is part of
	 * @param genesisBlock - the genesis (initial) block for the entire system
	 */
	public ABCIServer(TendermintChain chain, Block genesisBlock) {
		this.chain = chain;
		this.genesisBlock = genesisBlock;
		if (genesisBlock != null) {
			chain.addToCache(genesisBlock.getHash());
		}
	}

	@Override
	public ResponseBeginBlock requestBeginBlock(RequestBeginBlock requestBeginBlock) {
		Log.log(Level.FINER, "[TENDERMINT] New block started");
		return ResponseBeginBlock.newBuilder().build();
	}

	@Override
	public ResponseCheckTx requestCheckTx(RequestCheckTx requestCheckTx) {
		Log.log(Level.FINER, "[TENDERMINT] New transaction proposed");

		// Comment the next line when using a mock chain
		BlockAbstract abs = BlockAbstract.fromBytes(requestCheckTx.getTx().toByteArray());
		byte[] publicKey = chain.getApp().getLocalStore().getNode(abs.getOwnerNodeId()).getPublicKey();
		boolean valid = abs.checkSignature(publicKey);
		if (valid) {
			return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build();
		} else {
			String log = "signature on the abstract was invalid. Public key used:" + Utils.bytesToHexString(publicKey);
			Log.log(Level.INFO, "[TENDERMINT] Proposed block rejected because " + log);
			return ResponseCheckTx.newBuilder().setCode(CodeType.BadNonce).setLog(log).build();
		}
	}

	@Override
	public ResponseCommit requestCommit(RequestCommit requestCommit) {
		Log.log(Level.FINER, "[TENDERMINT] Finalize commit request");
		ResponseCommit.Builder responseCommit = ResponseCommit.newBuilder();
		return responseCommit.build();
	}

	@Override
	public ResponseDeliverTx receivedDeliverTx(RequestDeliverTx requestDeliverTx) {
		Log.log(Level.FINER, "[TENDERMINT] requestDeliverTx " + requestDeliverTx.toString());
		return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseEcho requestEcho(RequestEcho requestEcho) {
		Log.log(Level.FINER, "[TENDERMINT] Echo " + requestEcho.getMessage());
		return ResponseEcho.newBuilder().setMessage(requestEcho.getMessage()).build();
	}

	@Override
	public ResponseEndBlock requestEndBlock(RequestEndBlock requestEndBlock) {
		Log.log(Level.FINER, "[TENDERMINT] requestEndBlock " + requestEndBlock.toString());
		long height = requestEndBlock.getHeight();
		if (height > 0) {
			Log.log(Level.FINE, "[TENDERMINT] Block #" + height + " ended, going to update the cache");
			chain.updateCache(height);
		}
		return ResponseEndBlock.newBuilder().build();
	}

	@Override
	public ResponseFlush requestFlush(RequestFlush requestFlush) {
		Log.log(Level.FINER, "[TENDERMINT] requestFlush " + requestFlush.toString());
		return ResponseFlush.newBuilder().build();
	}

	@Override
	public ResponseInfo requestInfo(RequestInfo requestInfo) {
		Log.log(Level.FINER, "[TENDERMINT] requestInfo " + requestInfo.toString());
		ResponseInfo.Builder responseInfo = ResponseInfo.newBuilder();
		byte[] initialAppHash = genesisBlock.getHash().getBytes();
		//TODO: this should return the actual last block hash - now it's only used for genesis block
		responseInfo.setLastBlockAppHash(ByteString.copyFrom(initialAppHash));
		responseInfo.setLastBlockHeight(chain.getCurrentHeight());
		Log.log(Level.FINER, "[TENDERMINT] responseInfo " + responseInfo.toString());
		return responseInfo.build();
	}

	@Override
	public ResponseInitChain requestInitChain(RequestInitChain requestInitChain) {
		Log.log(Level.FINER, "[TENDERMINT] requestInitChain " + requestInitChain.toString());
		return ResponseInitChain.newBuilder().build();
	}

	@Override
	public ResponseQuery requestQuery(RequestQuery requestQuery) {
		Log.log(Level.FINER, "[TENDERMINT] Chain queried");
		return ResponseQuery.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseSetOption requestSetOption(RequestSetOption requestSetOption) {
		Log.log(Level.FINER, "[TENDERMINT] requestSetOption " + requestSetOption.toString());
		return ResponseSetOption.newBuilder().build();
	}
}
