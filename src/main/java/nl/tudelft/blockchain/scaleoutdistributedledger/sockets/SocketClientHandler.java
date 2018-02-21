package nl.tudelft.blockchain.scaleoutdistributedledger.sockets;

import java.util.logging.Level;

import nl.tudelft.blockchain.scaleoutdistributedledger.LocalStore;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.HandshakeMessage;
import nl.tudelft.blockchain.scaleoutdistributedledger.message.Message;
import nl.tudelft.blockchain.scaleoutdistributedledger.utils.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Socket handler for the client.
 */
public class SocketClientHandler extends ChannelInboundHandlerAdapter {

	private final LocalStore localStore;

	/**
	 * Constructor.
	 * @param localStore - the localstore of the node
	 */
	public SocketClientHandler(LocalStore localStore) {
		this.localStore = localStore;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		Log.log(Level.SEVERE, "Client socket error", cause);
		ctx.close();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		//Client should send handshake
		if (localStore != null) {
			ctx.channel().writeAndFlush(new HandshakeMessage(localStore.getOwnNode().getId()));
		}

		super.channelActive(ctx);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof Message) {
			((Message) msg).handle(localStore);
		} else {
			Log.log(Level.SEVERE, "Invalid message, not a message instance");
		}
	}
}
