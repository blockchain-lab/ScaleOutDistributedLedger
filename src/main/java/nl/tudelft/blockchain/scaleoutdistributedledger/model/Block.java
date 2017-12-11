package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.Getter;

import java.util.List;
import java.util.logging.Level;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Block class.
 */
public class Block {

    @Getter
    private final int number;

    @Getter
    private Block previousBlock;

    @Getter
    private final Node owner;

    @Getter
    private final List<Transaction> transactions;

	// Custom getter
	private Sha256Hash hash;

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
    }

	public synchronized Sha256Hash getHash() {
		if (this.hash == null) {
			this.hash = this.calculateHash();
		}
		return this.hash;
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
	 * Calculates the block hash
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
			outputStream.write(Utils.intToByteArray(this.owner.getId()));
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
	 * Get the abstract of the block
	 * @param privateKey - private RSA key
	 * @return abstract - abstract of the block
	 * @throws java.lang.Exception - something went wrong while signing the block
	 */
	public BlockAbstract getAbstract(byte[] privateKey) throws Exception {
		// Convert attributes of abstract into an array of bytes, for the signature
		// Important to keep the order of writings
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		outputStream.write(Utils.intToByteArray(this.owner.getId()));
		outputStream.write(Utils.intToByteArray(this.number));
		outputStream.write(this.getHash().getBytes());
		byte[] attrInBytes = outputStream.toByteArray();
		
		// Sign the attributes
		byte[] signature = RSAKey.sign(attrInBytes, privateKey);
		return new BlockAbstract(this.owner, this.number, this.getHash(), signature);
	}
	
	/**
	 * Get the abstract of the block
	 * @param rsaKey - RSA key pair
	 * @return abstract - abstract of the block
	 * @throws java.lang.Exception - something went wrong while signing the block
	 */
	public BlockAbstract getAbstract(RSAKey rsaKey) throws Exception {
		return this.getAbstract(rsaKey.getPrivateKey());
	}

}
