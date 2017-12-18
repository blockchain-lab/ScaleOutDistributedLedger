package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bartd on 17-12-2017.
 */
public class SocketClientHandler extends ChannelInboundHandlerAdapter {

    private final List<Integer> firstMessage;

    public SocketClientHandler() {
        firstMessage = new ArrayList<>(SocketClient.SIZE);
        System.out.println("CLIENT1");
        for(int i = 0; i < SocketClient.SIZE; i++) {
            firstMessage.add(i);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("CLIENT2");
        ctx.writeAndFlush(firstMessage);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("CLIENT3");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        System.out.println("CLIENT4");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println("CLIENT5");
        cause.printStackTrace();
        ctx.close();
    }
}
