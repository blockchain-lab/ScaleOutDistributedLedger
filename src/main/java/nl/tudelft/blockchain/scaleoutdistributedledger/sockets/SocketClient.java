package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;

import java.util.HashMap;

/**
 * Socket client.
 */
public class SocketClient {

    private HashMap<Integer, Channel> connections;

    private Bootstrap bootstrap;

    /**
     * Constructor.
     */
    public SocketClient() {
        this.connections = new HashMap<>();
    }

    /**
     * Init the client.
     * Note: one client can be used to send to multiple servers, this just sets the settings and pipeline.
     */
    public void initSocketClient() {
        EventLoopGroup group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline p = socketChannel.pipeline();
                        p.addLast(new ObjectEncoder(),
                                new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
                                new SocketClientHandler());
                    }
                });
    }

    /**
     * Send object to specified host.
     * @param node - the node to send the message to.
     * @param msg - the message object to send
     * @return - whether the message was sent successfully (does not mean it was received)
     */
    public boolean sendObject(Node node, Object msg) {
        ChannelFuture future = bootstrap.connect(node.getAddress(), node.get);
        if (!future.awaitUninterruptibly().isSuccess()) {
            // Could not connect
            return false;
        }
        assert future.isDone();
        future = future.channel().writeAndFlush(msg);
        this.connections.put()
        future.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                System.out.println("CHANNEL CLOSED BITCHES");
            }
        });
        return future.awaitUninterruptibly().isSuccess();
    }
}
