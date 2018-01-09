package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.logging.Level;

/**
 * Handler for socket server.
 */
public class SocketServerHandler extends ChannelInboundHandlerAdapter {

    private LocalStore localStore;

    /**
     * Constructor.
     * @param localStore - the localstor of the node
     */
    public SocketServerHandler(LocalStore localStore) {
        this.localStore = localStore;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Log.log(Level.INFO, "Server received message");
        if(msg instanceof Message) {
            ((Message) msg).handle(localStore);
        } else {
            Log.log(Level.SEVERE, "Invalid message, not a message instance");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Log.log(Level.SEVERE, "Server socket error", cause);
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                Log.log(Level.INFO, "Server detected idle channel, closing connection!");
                ctx.close();
            }
        }
    }
}
