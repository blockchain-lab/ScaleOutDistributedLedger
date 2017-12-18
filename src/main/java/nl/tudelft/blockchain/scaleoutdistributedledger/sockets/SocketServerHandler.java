package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;

/**
 * Created by bartd on 17-12-2017.
 */
public class SocketServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("SERVER1");
        System.out.println(msg.getClass());
        if(msg instanceof ArrayList) {
            ((ArrayList) msg).forEach(e -> {
                System.out.println(e);
            });
        }
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("SERVER2");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("SERVER3");
        cause.printStackTrace();
        ctx.close();
    }
}
