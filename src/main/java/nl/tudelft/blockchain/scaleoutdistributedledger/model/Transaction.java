package nl.tudelft.blockchain.scaleoutdistributedledger.model;

import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.Getter;

import java.util.Set;
import java.util.logging.Level;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

/**
 * Transaction class.
 */
public class Transaction {

    @Getter
    private final int number;

    @Getter
    private final Node sender, receiver;

    @Getter
    private final long amount, remainder;

    @Getter
    private final Set<Transaction> source;

	// Custem getter
	private Sha256Hash hash;

    /**
     * Constructor.
     * @param number - the number of this transaction.
     * @param sender - the sender of this transaction.
     * @param receiver - the receiver of this transaction.
     * @param amount - the amount to be transferred.
     * @param remainder - the remaining amount.
     * @param source - set of transactions that are used as sourc for this transaction.
     */
    public Transaction(int number, Node sender, Node receiver, long amount, long remainder, Set<Transaction> source) {
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.remainder = remainder;
        this.source = source;
        this.number = number;
    }

	public Sha256Hash getHash() {
		if (this.hash == null) {
			this.hash = this.calculateHash();
		}
		return this.hash;
	}
	
	/**
	 * Calculate the transaction hash
	 * @return Hash SHA256
	 */
	private Sha256Hash calculateHash() {
		// Convert attributes of transaction into an array of bytes
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			// Important to keep the order of writings
			outputStream.write(Utils.intToByteArray(this.number));
			outputStream.write(Utils.intToByteArray(this.sender.getId()));
			outputStream.write(Utils.intToByteArray(this.receiver.getId()));
			outputStream.write(Utils.longToByteArray(this.amount));
			outputStream.write(Utils.longToByteArray(this.remainder));
			
			// TODO: check if we really need to do this
			for (Transaction tx : this.source) {
				outputStream.write(tx.getHash().getBytes());
			}
		} catch (IOException ex) {
			Log.log(Level.SEVERE, null, ex);
		}
		byte[] transactionInBytes = outputStream.toByteArray();
		
		return new Sha256Hash(transactionInBytes);
	}

}
