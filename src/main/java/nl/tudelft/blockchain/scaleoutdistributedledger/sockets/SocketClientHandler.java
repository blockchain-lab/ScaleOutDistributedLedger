package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.logging.Level;

/**
 * Socket handler for the client.
 */
public class SocketClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.log(Level.SEVERE, "Client socket error", cause);
        ctx.close();
    }
}
