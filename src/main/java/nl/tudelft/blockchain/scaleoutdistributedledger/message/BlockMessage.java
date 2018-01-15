package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Block;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Sha256Hash;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Block message for netty.
 */
public class BlockMessage extends Message {
	
	@Getter
	private final int number;

	@Getter
	private final int previousBlockNumber;
	
	@Getter
	private final BlockMessage previousBlock;
	
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
	 * @param usePreviousBlockNumber - option to use or not the previous block number instead of a reference to the object
	 */
	public BlockMessage(Block block, boolean usePreviousBlockNumber) {
		this.number = block.getNumber();
		Block prevBlock = block.getPreviousBlock();
		if (prevBlock != null) {
			if (usePreviousBlockNumber) {
				this.previousBlockNumber = prevBlock.getNumber();
				this.previousBlock = null;
			} else {
				this.previousBlockNumber = -1;
				this.previousBlock = new BlockMessage(prevBlock);
			}
		} else {
			// It's a genesis block
			this.previousBlockNumber = -1;
			this.previousBlock = null;
		}
		// It's a genesis block
		if (block.getOwner() == null) {
			this.ownerId = Transaction.GENESIS_SENDER;
		} else {
			this.ownerId = block.getOwner().getId();
		}
		this.transactions = new ArrayList<>();
		for (Transaction transaction : block.getTransactions()) {
			this.transactions.add(new TransactionMessage(transaction));
		}
		this.hash = block.getHash();
	}
	
	/**
	 * Constructor.
	 * @param block - original block
	 */
	public BlockMessage(Block block) {
		this(block, false);
	}

	@Override
	public void handle(LocalStore localStore) {
		// Do nothing.
	}
}
