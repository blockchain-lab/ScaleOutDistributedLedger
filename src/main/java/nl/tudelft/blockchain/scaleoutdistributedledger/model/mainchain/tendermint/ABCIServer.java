package nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint;

import com.github.jtendermint.jabci.api.ABCIAPI;
import com.github.jtendermint.jabci.types.Types.*;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.BlockAbstract;

/**
 * @see <a href="https://github.com/tendermint/abci#message-types">Tendermint ABCI message type documentation</a>
 */
public class ABCIServer implements ABCIAPI {
	@Override
	public ResponseBeginBlock requestBeginBlock(RequestBeginBlock requestBeginBlock) {
		System.out.println("Begin block");
		return ResponseBeginBlock.newBuilder().build();
		//TODO: Implement this
	}

	/**
	 * Validate a transaction.
	 *
	 * @param requestCheckTx
	 * @return
	 */
	@Override
	public ResponseCheckTx requestCheckTx(RequestCheckTx requestCheckTx) {
		System.out.println("Check Tx");
		BlockAbstract abs = BlockAbstract.fromBytes(requestCheckTx.getTx().toByteArray());

		//TODO: validate the abstract
		boolean valid = true;
		if (valid) {
			return ResponseCheckTx.newBuilder().setCode(CodeType.OK).build();
		} else {
			String log = "Discription of what went wrong while validating";
			return ResponseCheckTx.newBuilder().setCode(CodeType.BadNonce).setLog(log).build();
		}
	}

	@Override
	public ResponseCommit requestCommit(RequestCommit requestCommit) {
		System.out.println("Commit");
		return ResponseCommit.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseDeliverTx receivedDeliverTx(RequestDeliverTx requestDeliverTx) {
		return ResponseDeliverTx.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseEcho requestEcho(RequestEcho requestEcho) {
		return ResponseEcho.newBuilder().setMessage(requestEcho.getMessage()).build();
	}

	@Override
	public ResponseEndBlock requestEndBlock(RequestEndBlock requestEndBlock) {
		System.out.println("End of Block");
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
		System.out.println("Query");
		return ResponseQuery.newBuilder().setCode(CodeType.OK).build();
	}

	@Override
	public ResponseSetOption requestSetOption(RequestSetOption requestSetOption) {
		return ResponseSetOption.newBuilder().build();
	}
}
