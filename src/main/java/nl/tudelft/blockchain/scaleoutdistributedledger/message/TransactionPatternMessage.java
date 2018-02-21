package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.lang3.SerializationUtils;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.simulation.transactionpattern.ITransactionPattern;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

/**
 * Message containing the transaction pattern that the receiver should use.
 */
public class TransactionPatternMessage extends Message {
	public static final int MESSAGE_ID = 6;
	
	private ITransactionPattern pattern;
	
	/**
	 * @param pattern - the pattern
	 */
	public TransactionPatternMessage(ITransactionPattern pattern) {
		this.pattern = pattern;
	}
	
	@Override
	public void handle(LocalStore localStore) {
		try {
			localStore.getApplication().setTransactionPattern(pattern);
		} catch (Exception ex) {
			Log.log(Level.SEVERE, "Unable to set transaction pattern on node " + localStore.getOwnNode().getId(), ex);
		}
	}
	
	@Override
	public int getMessageId() {
		return MESSAGE_ID;
	}
	
	@Override
	public void writeToStream(ByteBufOutputStream stream) throws IOException {
		byte[] bytes = SerializationUtils.serialize(pattern);
		stream.writeInt(bytes.length);
		stream.write(bytes);
	}
	
	/**
	 * @param stream       - the stream to read from
	 * @return             - the read TransactionPatternMessage
	 * @throws IOException - If reading from the stream causes an IOException.
	 */
	public static TransactionPatternMessage readFromStream(ByteBufInputStream stream) throws IOException {
		int length = stream.readInt();
		byte[] bytes = new byte[length];
		stream.read(bytes);
		ITransactionPattern pattern = SerializationUtils.deserialize(bytes);
		
		return new TransactionPatternMessage(pattern);
	}

	@Override
	public String toString() {
		return "TransactionPatternMessage<" + pattern.getName() + ">";
	}
}
