package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Socket handler for the client.
 */
public class SocketClientHandler extends ChannelInboundHandlerAdapter {



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
