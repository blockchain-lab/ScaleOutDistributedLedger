package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;

/**
 * Message abstract super class.
 */
public abstract class Message {

	/**
	 * Handle the message received.
	 * @param localStore - local store of the current node
	 */
	public abstract void handle(LocalStore localStore);
	
	/**
	 * @return - the message id of this message
	 */
	public abstract int getMessageId();
	
	/**
	 * Writes this message to the given stream.
	 * Message implementations should override this method if they have any information to send.
	 * @param stream       - the stream to write to
	 * @throws IOException - If writing to the stream causes an exception.
	 */
	public void writeToStream(ByteBufOutputStream stream) throws IOException {}
	
	/**
	 * @param stream       - the stream to read from
	 * @return             - the message that was read
	 * @throws IOException - If reading from the stream causes an IOException.
	 */
	public static Message readFromStream(ByteBufInputStream stream) throws IOException {
		int msgId = stream.readUnsignedByte();
		switch (msgId) {
    		case ProofMessage.MESSAGE_ID:
    			return ProofMessage.readFromStream(stream);
    		case BlockMessage.MESSAGE_ID:
    			return BlockMessage.readFromStream(stream);
    		case TransactionMessage.MESSAGE_ID:
    			return TransactionMessage.readFromStream(stream);
    		case StartTransactingMessage.MESSAGE_ID:
    			return new StartTransactingMessage();
    		case StopTransactingMessage.MESSAGE_ID:
    			return new StopTransactingMessage();
    		case UpdateNodesMessage.MESSAGE_ID:
    			return new UpdateNodesMessage();
    		case TransactionPatternMessage.MESSAGE_ID:
    			try (ObjectInputStream ois = new ObjectInputStream(stream)) {
    				return (Message) ois.readObject();
    			} catch (ClassNotFoundException ex) {
					throw new IOException("Unable to read TransactionPatternMessage from stream: ClassNotFound", ex);
				}
			default:
				Log.log(Level.SEVERE, "Received unknown message ID: " + msgId);
				throw new IllegalArgumentException("Unknown message type!");
    	}
	}
}
