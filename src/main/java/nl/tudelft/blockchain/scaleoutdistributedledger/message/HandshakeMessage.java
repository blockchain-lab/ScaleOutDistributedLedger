package nl.tudelft.blockchain.scaleoutdistributedledger.message;

import java.io.IOException;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import lombok.Getter;

/**
 * Message for representing handshakes.
 */
public class HandshakeMessage extends Message {
	public static final int MESSAGE_ID = 7;
	
	@Getter
	private final int senderId;
	
	/**
	 * @param senderId - the sender
	 */
	public HandshakeMessage(int senderId) {
		this.senderId = senderId;
	}
	
	@Override
	public void handle(LocalStore localStore) {}

	@Override
	public int getMessageId() {
		return MESSAGE_ID;
	}
	
	@Override
	public void writeToStream(ByteBufOutputStream stream) throws IOException {
		stream.writeShort(senderId);
	}
	
	/**
	 * @param stream       - the stream to read from
	 * @return             - the HandShakeMessage that was read
	 * @throws IOException - If reading from the stream causes an IOException.
	 */
	public static HandshakeMessage readFromStream(ByteBufInputStream stream) throws IOException {
		return new HandshakeMessage(stream.readUnsignedShort());
	}
	
	@Override
	public String toString() {
		return "HandShakeMessage";
	}
}
