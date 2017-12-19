package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * Handler for socket server.
 */
public class SocketServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("Server received msg");
        // TODO: handle msg
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if(e.state() == IdleState.ALL_IDLE) {
                System.out.println("Server detected idle channel!");
                ctx.close();
            }
        }
    }
}
