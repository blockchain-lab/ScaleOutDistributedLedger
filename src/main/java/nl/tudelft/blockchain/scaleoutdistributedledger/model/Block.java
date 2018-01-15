package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import lombok.Getter;
import lombok.Setter;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.BlockMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.TransactionMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Block class.
 */
public class Block implements Cloneable {

	public static final int GENESIS_BLOCK_NUMBER = 1;
	
	@Getter
	private final int number;

	@Getter
	private Block previousBlock;

	@Getter @Setter
	private Node owner;

	@Getter
	private final List<Transaction> transactions;

	// Custom getter
	private Sha256Hash hash;
	
	private transient Optional<Boolean> onMainChain;

	/**
	 * Constructor.
	 * @param number - the number of this block.
	 * @param owner - the owner of this block.
	 * @param transactions - a list of transactions of this block.
	 */
	public Block(int number, Node owner, List<Transaction> transactions) {
		this.number = number;
		this.owner = owner;
		this.transactions = transactions;
		this.previousBlock = null;
		this.onMainChain = Optional.empty();
	}

	/**
	 * Constructor.
	 * @param number - the number of this block.
	 * @param previousBlock - reference to the previous block in the chain of this block.
	 * @param owner - the owner of this block.
	 * @param transactions - a list of transactions of this block.
	 */
	public Block(int number, Block previousBlock, Node owner, List<Transaction> transactions) {
		this.number = number;
		this.previousBlock = previousBlock;
		this.owner = owner;
		this.transactions = transactions;
		this.onMainChain = Optional.empty();
	}

	/**
	 * Constructor to decode a block message.
	 * @param blockMessage - block message from network.
	 * @param localStore - local store.
	 * @throws IOException - error while getting node from tracker.
	 */
	public Block(BlockMessage blockMessage, LocalStore localStore) throws IOException {
		this.number = blockMessage.getNumber();
		this.owner = localStore.getNode(blockMessage.getOwnerId());
		
		if (blockMessage.getPreviousBlock() != null) {
			// Convert BlockMessage to Block
			this.previousBlock = new Block(blockMessage.getPreviousBlock(), localStore);
		} else if (blockMessage.getPreviousBlockNumber() != -1) {
			// Get block by number from owner
			this.previousBlock = this.owner.getChain().getBlocks().get(blockMessage.getPreviousBlockNumber());
		} else {
			// It's a genesis block
			this.previousBlock = null;
		}
		
		// Convert TransactionMessage to Transaction
		this.transactions = new ArrayList<>();
		for (TransactionMessage transactionMessage : blockMessage.getTransactions()) {
			this.transactions.add(new Transaction(transactionMessage, localStore));
		}
		this.hash = blockMessage.getHash();
		this.onMainChain = Optional.empty();
	}
	
	/**
	 * Get hash of the block.
	 * @return Hash SHA256
	 */
	public synchronized Sha256Hash getHash() {
		if (this.hash == null) {
			this.hash = this.calculateHash();
		}
		return this.hash;
	}

	/**
	 * Calculate the abstract of the block.
	 * @return abstract of the block
	 * @throws IllegalStateException - something went wrong while signing the block
	 */
	public BlockAbstract calculateBlockAbstract() {
		if (!(this.owner instanceof OwnNode)) {
			throw new UnsupportedOperationException("You cannot calculate the block abstract of a block you do not own!");
		}
		
		// Convert attributes of abstract into an array of bytes, for the signature
		// Important to keep the order of writings
		byte[] attrInBytes;
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			outputStream.write(Utils.intToByteArray(this.owner.getId()));
			outputStream.write(Utils.intToByteArray(this.number));
			outputStream.write(this.getHash().getBytes());
			attrInBytes = outputStream.toByteArray();
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to write to outputstream", ex);
		}

		// Sign the attributes
		try {
			byte[] signature = ((OwnNode) this.owner).sign(attrInBytes);
			return new BlockAbstract(this.owner.getId(), this.number, this.getHash(), signature);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to sign block abstract", ex);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + number;
		result = prime * result + owner.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof Block)) return false;

		Block other = (Block) obj;
		if (this.number != other.number) return false;
		if (this.owner != other.owner) return false;

		if (this.previousBlock == null) {
			if (other.previousBlock != null) return false;
		} else if (!this.previousBlock.equals(other.previousBlock)) return false;

		if (!this.getHash().equals(other.getHash())) return false;

		return this.transactions.equals(other.transactions);
	}

	/**
	 * Calculates the block hash.
	 * @return Hash SHA256
	 */
	private Sha256Hash calculateHash() {
		// Convert attributes of block into an array of bytes
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			// Important to keep the order of writings
			outputStream.write(Utils.intToByteArray(this.number));
			byte[] prevBlockHash = (this.previousBlock != null) ? this.previousBlock.getHash().getBytes() : new byte[0];
			outputStream.write(prevBlockHash);
			if (this.owner != null) {
				outputStream.write(Utils.intToByteArray(this.owner.getId()));
			}
			for (Transaction tx : this.transactions) {
				outputStream.write(tx.getHash().getBytes());
			}
		} catch (IOException ex) {
			Log.log(Level.SEVERE, null, ex);
		}
		byte[] blockInBytes = outputStream.toByteArray();

		return new Sha256Hash(blockInBytes);
	}

	/**
	 * Clones the block.
	 * @return - the clone
	 */
	@Override
	public Block clone() {
		try {
			return (Block) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new IllegalStateException("Clone not supported");
		}
	}
	
	/**
	 * Returns the boolean onMainChain, and gets it if it is not present.
	 *
	 * @return - boolean identifying if this abstract is on the main chain.
	 */
	public boolean isOnMainChain(LocalStore localStore) {
		if (!this.onMainChain.isPresent()) {
			this.onMainChain = Optional.of(localStore.getMainChain().isPresent(this.getHash()));
		}
		return this.onMainChain.get();
	}
}
