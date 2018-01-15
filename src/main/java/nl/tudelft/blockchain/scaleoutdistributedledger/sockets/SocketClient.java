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
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * Socket client.
 */
public class SocketClient {

    private HashMap<Node, Channel> connections;

    private Bootstrap bootstrap;

    private EventLoopGroup group;

    /**
     * Constructor.
     */
    public SocketClient() {
        this.connections = new HashMap<>();
        this.initSocketClient();
    }

    /**
     * Init the client.
     * Note: one client can be used to send to multiple servers, this just sets the settings and pipeline.
     */
    private void initSocketClient() {
        group = new NioEventLoopGroup();
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
     * Shuts down the client server.
     */
    public void shutdown() {
        Log.log(Level.INFO, "Shutting down socket client...");
        group.shutdownGracefully();
    }

    /**
     * Send object to specified host.
     * Is blocking until message is actually sent, or failed (up to 60 seconds)
     * @param node - the node to send the message to.
     * @param msg - the message object to send
     * @return - whether the message was sent successfully
     */
    public boolean sendMessage(Node node, Object msg) throws InterruptedException {
        Channel channel = connections.get(node);
        if (channel == null || !channel.isOpen()) {
            Log.log(Level.FINE, "No open connection found, connecting...");
            ChannelFuture future = bootstrap.connect(node.getAddress(), node.getPort());
            if (!future.await().isSuccess()) {
                // Could not connect
            	Log.log(Level.SEVERE, "Unable to connect to " + node.getAddress() + ":" + node.getPort(), future.cause());
                return false;
            }
            assert future.isDone();
            channel = future.channel();
            future.channel().closeFuture().addListener((ChannelFutureListener) channelFuture -> Log.log(Level.FINE, "Client detected channel close"));
            Log.log(Level.FINE, "Client connected to server!");
        }

        ChannelFuture future = channel.writeAndFlush(msg);
        Log.log(Level.FINE, "Message sent by client");

        this.connections.put(node, future.channel());

        future.await();
        
        if (!future.isSuccess()) {
        	Log.log(Level.SEVERE, "Failed to send message", future.cause());
        	return false;
        }
        
        return true;
    }
}
