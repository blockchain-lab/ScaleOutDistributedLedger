package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.api.ABCIAPI;
import com.github.jtendermint.jabci.types.Types.CodeType;
import com.github.jtendermint.jabci.types.Types.Header;
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
	private final Object lastLock = new Object();
	private long committingHeight;
	private byte[] lastHash;
	private long lastHeight;
	
	/**
	 * @param chain - the main chain this server is part of
	 * @param genesisBlock - the genesis (initial) block for the entire system
	 */
	public ABCIServer(TendermintChain chain, Block genesisBlock) {
		this.chain = chain;
		if (genesisBlock != null) {
			chain.addToCache(genesisBlock.getHash());
			this.lastHash = genesisBlock.getHash().getBytes();
		}
	}

	@Override
	public ResponseCheckTx requestCheckTx(RequestCheckTx requestCheckTx) {
//		Log.log(Level.FINER, "[TENDERMINT] New BlockAbstract proposed", chain.getNodeId());

		// Comment the next line when using a mock chain
		BlockAbstract abs = BlockAbstract.fromBytes(requestCheckTx.getTx().toByteArray());
		byte[] publicKey = chain.getApp().getLocalStore().getNode(abs.getOwnerNodeId()).getPublicKey();
		boolean valid = abs.checkSignature(publicKey);
		if (valid) {
			return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build();
		} else {
			String log = "signature on the abstract was invalid. Public key used:" + Utils.bytesToHexString(publicKey);
			Log.log(Level.INFO, "[TENDERMINT] Proposed BlockAbstract rejected because " + log);
			return ResponseCheckTx.newBuilder().setCode(CodeType.BadNonce).setLog(log).build();
		}
	}
	
	@Override
	public ResponseBeginBlock requestBeginBlock(RequestBeginBlock requestBeginBlock) {
		Header header = requestBeginBlock.getHeader();
		committingHeight = header.getHeight();
		Log.log(Level.FINER,
				"[TENDERMINT] Begin TMBlock " + committingHeight + " (" + header.getNumTxs() + " abstracts)",
				chain.getNodeId());
		
		//TODO In TM15, we would be able to see what validators are absent
		
		return ResponseBeginBlock.newBuilder().build();
	}

	@Override
	public ResponseCommit requestCommit(RequestCommit requestCommit) {
		//TODO Save current state to add persistence
		byte[] appHash;
		synchronized (lastLock) {
			appHash = chain.getStateHash();
			lastHash = appHash;
			
			lastHeight = committingHeight;
			chain.setCurrentHeight(lastHeight);
		}
		
		//Then, respond with our new state.
		ResponseCommit.Builder responseCommit = ResponseCommit.newBuilder();
		responseCommit.setCode(CodeType.OK);
		responseCommit.setData(ByteString.copyFrom(appHash));
		return responseCommit.build();
	}

	@Override
	public ResponseDeliverTx receivedDeliverTx(RequestDeliverTx requestDeliverTx) {
		//Add to known transactions
		BlockAbstract abs = BlockAbstract.fromBytes(requestDeliverTx.getTx().toByteArray());
		if (abs != null) {
			chain.addToCache(abs.getBlockHash());
		} else {
			Log.log(Level.WARNING, "[TENDERMINT] Received invalid BlockAbstract!", chain.getNodeId());
		}
		
		return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build();
	}
	
	@Override
	public ResponseEndBlock requestEndBlock(RequestEndBlock requestEndBlock) {
		return ResponseEndBlock.newBuilder().build();
	}

	@Override
	public ResponseEcho requestEcho(RequestEcho requestEcho) {
		Log.log(Level.FINER, "[TENDERMINT] Echo " + requestEcho.getMessage(), chain.getNodeId());
		return ResponseEcho.newBuilder().setMessage(requestEcho.getMessage()).build();
	}

	@Override
	public ResponseInfo requestInfo(RequestInfo requestInfo) {
		Log.log(Level.FINER, "[TENDERMINT] Info requested " + requestInfo.toString(), chain.getNodeId());

		//Retrieve last hash and last height
		byte[] hash;
		long height;
		synchronized (lastLock) {
			hash = lastHash;
			height = lastHeight;
		}
		
		ResponseInfo.Builder responseInfo = ResponseInfo.newBuilder();
		responseInfo.setLastBlockAppHash(ByteString.copyFrom(hash));
		responseInfo.setLastBlockHeight(height);
		return responseInfo.build();
	}

	@Override
	public ResponseInitChain requestInitChain(RequestInitChain requestInitChain) {
		Log.log(Level.FINER, "[TENDERMINT] Initialized chain:\n " + requestInitChain.toString(), chain.getNodeId());
		Log.log(Level.INFO, "[TENDERMINT] Initialized with " + requestInitChain.getValidatorsCount() + " validators.", chain.getNodeId());
		return ResponseInitChain.newBuilder().build();
	}

	@Override
	public ResponseQuery requestQuery(RequestQuery requestQuery) {
		return ResponseQuery.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseSetOption requestSetOption(RequestSetOption requestSetOption) {
		return ResponseSetOption.newBuilder().build();
	}
	
	@Override
	public ResponseFlush requestFlush(RequestFlush requestFlush) {
		//Don't do anything for flush requests
		return ResponseFlush.newBuilder().build();
	}
}
