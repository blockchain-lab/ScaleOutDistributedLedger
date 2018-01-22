package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;

/**
 * Block message for netty.
 */
public class BlockMessage extends Message {
	
	@Getter
	private final int number;

	@Getter
	private final int previousBlockNumber;
	
	//@Getter
	//private final BlockMessage previousBlock;
	
	@Getter
	private final int ownerId;

	@Getter
	private final List<TransactionMessage> transactions;

	@Getter
	private final Sha256Hash hash;

	//private transient BlockAbstract blockAbstract;
	//private transient Boolean hasAbstract;
	
	/**
	 * Constructor.
	 * @param block - original block
	 * @param proofReceiver - receiver of the proof
	 */
	public BlockMessage(Block block, Node proofReceiver) {
		this.number = block.getNumber();
		Block prevBlock = block.getPreviousBlock();
		if (prevBlock != null) {
			this.previousBlockNumber = prevBlock.getNumber();
		} else {
			// It's a genesis block
			this.previousBlockNumber = -1;
		}
		// It's a genesis block
		if (block.getOwner() == null) {
			this.ownerId = Transaction.GENESIS_SENDER;
		} else {
			this.ownerId = block.getOwner().getId();
		}
		this.transactions = new ArrayList<>();
		for (Transaction transaction : block.getTransactions()) {
			this.transactions.add(new TransactionMessage(transaction, proofReceiver));
		}
		this.hash = block.getHash();
	}

	@Override
	public void handle(LocalStore localStore) {
		// Do nothing.
	}

	public Block toBlockWithoutSources(LocalStore localStore) {
		List<Transaction> transactions = new ArrayList<>();
		for(TransactionMessage tm : this.transactions) {
			transactions.add(tm.toTransactionWithoutSources(localStore));
		}
		return new Block(this.number, localStore.getNode(this.ownerId), transactions);
	}
}
