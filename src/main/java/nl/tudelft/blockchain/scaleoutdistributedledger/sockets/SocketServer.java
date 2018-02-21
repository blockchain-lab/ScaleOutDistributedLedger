package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Socket server.
 */
public class SocketServer implements Runnable {
	// The maximum message size in bytes.
    public static final int MAX_MESSAGE_SIZE = 5 * 1024 * 1024;
    // In seconds, time connections are kept open after messages.
    private static final int CHANNEL_TIMEOUT = 0;

    private int port;
    private LocalStore localStore;

    /**
     * Constructor.
     * @param port - the port to listen on.
     * @param localStore - the localstore of the node.
     */
    public SocketServer(int port, LocalStore localStore) {
        this.port = port;
        this.localStore = localStore;
    }

    /**
     * Init the socket serer.
     */
    public void initServer() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
//                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline p = socketChannel.pipeline();
                            p.addLast(new IdleStateHandler(0, 0, CHANNEL_TIMEOUT),
                                    new MessageEncoder(),
                                    new MessageDecoder(MAX_MESSAGE_SIZE),
                                    new SocketServerHandler(localStore));
                        }
                    });

            Log.log(Level.INFO, "Starting socket server on port " + this.port);
            b.bind(port).sync().channel().closeFuture().sync();
        } catch (InterruptedException e) {
            Log.log(Level.INFO, "Socket server was interrupted, shutting down...");
        } catch (Exception ex) {
        	Log.log(Level.SEVERE, "Exception in socket server", ex);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void run() {
        this.initServer();
    }
}
