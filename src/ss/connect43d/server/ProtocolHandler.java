package ss.connect43d.server;

import java.io.UnsupportedEncodingException;
import java.nio.channels.SocketChannel;

public abstract class ProtocolHandler {
	private Server server;
	private String encoding; 
	
	protected void setSendEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public void setServer(Server server) {
		this.server = server;
	}
	
	public void send(SocketChannel socket, String data) {
		try {
			this.send(socket, data.getBytes(encoding));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	public void send(SocketChannel socket, byte[] data) {
		this.server.send(socket, data);
	}
	
	public abstract void onClientConnected(SocketChannel socket);
	public abstract void onClientDisconnected(SocketChannel socket);
	public abstract void onData(SocketChannel socket, byte[] data);
}
