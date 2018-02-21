package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import java.io.StreamCorruptedException;

import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Netty decoder for Messages.
 */
public class MessageDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * Creates a new decoder whose maximum object size is {@code 1048576}
     * bytes.  If the size of the received object is greater than
     * {@code 1048576} bytes, a {@link StreamCorruptedException} will be
     * raised.
     */
    public MessageDecoder() {
        this(1048576);
    }

    /**
     * Creates a new decoder with the specified maximum object size.
     *
     * @param maxObjectSize  the maximum byte length of the serialized object.
     *                       if the length of the received object is greater
     *                       than this value, {@link StreamCorruptedException}
     *                       will be raised.
     */
    public MessageDecoder(int maxObjectSize) {
        super(maxObjectSize, 0, 4, 0, 4);
    }

    @Override
    protected Message decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }

        ByteBufInputStream bbis = new ByteBufInputStream(frame, true);
        try {
        	return Message.readFromStream(bbis);
        } finally {
    		bbis.close();
        }
    }
}
