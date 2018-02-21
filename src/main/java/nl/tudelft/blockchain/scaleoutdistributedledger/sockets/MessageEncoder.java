package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty encoder for messages.
 */
public class MessageEncoder extends MessageToByteEncoder<Message> {
	private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        int startIdx = out.writerIndex();

        ByteBufOutputStream bout = new ByteBufOutputStream(out);
        try {
            bout.write(LENGTH_PLACEHOLDER);
            bout.write(msg.getMessageId());
            msg.writeToStream(bout);
            bout.flush();
        } finally {
            bout.close();
        }
        int endIdx = out.writerIndex();
        
        out.setInt(startIdx, endIdx - startIdx - 4);
    }
}
