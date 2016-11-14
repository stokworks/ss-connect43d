package ss.connect43d.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Server {
	private static final String USAGE = "usage: " + Server.class.getName() + " <port>";
	
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println(USAGE);
			System.exit(0);
		}

		int port = 0;
		try {
			port = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			System.out.println(USAGE);
			System.out.println("ERROR: port " + args[1] + " is not an integer");
			System.exit(0);
		}
		
		try {
			// Listen on all addresses
			new Server(null, port, new ProtocolSHandler());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 8);
	private Map<SocketChannel, List<ByteBuffer>> pendingData = new HashMap<SocketChannel, List<ByteBuffer>>();
	private ProtocolHandler protocolHandler;
	
	public Server(InetAddress address, int port, ProtocolHandler protocolHandler) throws IOException {
		this.selector = SelectorProvider.provider().openSelector();
		
		this.serverChannel = ServerSocketChannel.open();
		this.serverChannel.configureBlocking(false);
		
		InetSocketAddress isa = new InetSocketAddress(address, port);
		this.serverChannel.socket().bind(isa);
		
		this.serverChannel.register(this.selector,  SelectionKey.OP_ACCEPT);
		
		this.protocolHandler = protocolHandler;
		this.protocolHandler.setServer(this);
		
		this.start();
	}
	
	public void send(SocketChannel socket, byte[] data) {
		SelectionKey key = socket.keyFor(this.selector);
		
		if (key == null) {
			return;
		}
		
		key.interestOps(SelectionKey.OP_WRITE);
		
		List<ByteBuffer> queue = this.pendingData.get(socket);
		if (queue == null) {
			queue = new ArrayList<ByteBuffer>();
			this.pendingData.put(socket, queue);
		}
		
		queue.add(ByteBuffer.wrap(data));
	}
	
	private void start() {
		while (true) {
			try {
				this.selector.select();
				
				Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
				
				while (selectedKeys.hasNext()) {
					SelectionKey key = selectedKeys.next();
					selectedKeys.remove();
					
					if (!key.isValid()) {
						continue;
					}
					
					if (key.isAcceptable()) {
						this.accept(key);
					} else if (key.isReadable()) {
						this.read(key);
					} else if (key.isWritable()) {
						this.write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void accept(SelectionKey key) throws IOException {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		
		socketChannel.register(this.selector, SelectionKey.OP_READ);
		
		this.handleConnect(socketChannel);
	}
	
	private void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		
		this.readBuffer.clear();
		
		int numRead;
		try {
			numRead = socketChannel.read(this.readBuffer);
		} catch (IOException e) {
			this.handleDisconnect(socketChannel);
			key.cancel();
			socketChannel.close();
			return;
		}
		
		if (numRead == -1) {
			this.handleDisconnect(socketChannel);
			key.cancel();
			socketChannel.close();
			return;
		}
		
		byte[] bytesRead = new byte[numRead];
		this.readBuffer.rewind();
		this.readBuffer.get(bytesRead);
		
		this.handleIncomingData(socketChannel, bytesRead);
	}
	
	private void write(SelectionKey key) throws IOException {
		SocketChannel socket = (SocketChannel) key.channel();
		List<ByteBuffer> queue = this.pendingData.get(socket);
		
		while (!queue.isEmpty()) {
			ByteBuffer buf = queue.get(0);
			socket.write(buf);
			
			if (buf.remaining() > 0) {
				break;
			}
			
			queue.remove(0);
		}
		
		key.interestOps(SelectionKey.OP_READ);
	}
	
	private void handleConnect(SocketChannel socket) {
		this.protocolHandler.onClientConnected(socket);
	}
	
	private void handleDisconnect(SocketChannel socket) {
		this.protocolHandler.onClientDisconnected(socket);
	}
	
	private void handleIncomingData(SocketChannel socket, byte[] data) {
		this.protocolHandler.onData(socket, data);
	}
}
