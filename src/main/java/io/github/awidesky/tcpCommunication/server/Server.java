package io.github.awidesky.tcpCommunication.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.LoggerThread;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.jCipherUtil.hash.Hashes;
import io.github.awidesky.jCipherUtil.messageInterface.InPut;
import io.github.awidesky.tcpCommunication.MessageType;
import io.github.awidesky.tcpCommunication.Protocol;

public class Server {

	private ExecutorService pool;
	private Selector selector;
	private ServerSocketChannel serverSocket;
	private int port;
	
	private final LoggerThread loggerThread = new LoggerThread();
	private final Logger logger;
	
	private final Map<String, ConnectedClient> clients = new HashMap<>();
	
	public Server() throws IOException {
		this(System.out);
	}

	public Server(OutputStream os) throws IOException {
		loggerThread.setLogDestination(os, true);
		loggerThread.setDatePrefixAllChildren(new SimpleDateFormat("[kk:mm:ss.SSS]"));
		logger = loggerThread.getLogger("[Server|" + getIP() + ":" + port + "] ");
		loggerThread.start();
	}
	
	public void connect(int port) throws IOException {
		connect(port, Runtime.getRuntime().availableProcessors());
	}
	
	public void connect(int port, int threads) throws IOException {
		if(serverSocket != null) {
			logger.log("Reconnecting to port " + port + " - close current server");
			stop();
			logger.log("Current server");
		}
		
		serverSocket = ServerSocketChannel.open();
		try {
			this.port = port;
			pool = Executors.newFixedThreadPool(threads);
			selector = Selector.open();
			serverSocket.configureBlocking(false);
			serverSocket.bind(new InetSocketAddress(this.port));
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
			logger.log("Server is opended with " + threads + "threads to port " + port);
		} catch (IOException e) {
			if(serverSocket.isOpen()) stop();
			throw e;
		}
		pool.submit(this::listen);
	}
	
	private void listen() {
		while (true) {
			try {
				int keys = selector.select();
				if (keys == 0)
					continue;

				Iterator<SelectionKey> it = selector.selectedKeys().iterator();

				while (it.hasNext()) {
					SelectionKey key = it.next();

					if (key.isAcceptable()) {
						pool.submit(this::accept);
					} else if (key.isReadable()) {
						pool.submit(() -> read((ConnectedClient) key.attachment()));
					} else if (key.isWritable()) {
						pool.submit(() -> write((ConnectedClient) key.attachment()));
					} else {
						logger.log("Unsupported ready-operation : \"" + key.interestOps() + "\" from selected key : "
									+ ((SocketChannel) key.channel()).getRemoteAddress());
					}

					it.remove();
				}
			} catch (IOException e) {
				logger.log("Exception while selecting chennels!");
				logger.log(e);
			}
		}
	}
	

	protected void accept() {
		try {
			SocketChannel sc = serverSocket.accept();
			ConnectedClient client = new ConnectedClient(sc, loggerThread.getLogger());
			clients.put(client.hash, client);
			sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, client);
			logger.log("Connected : " + sc.getRemoteAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void read(ConnectedClient client) {
		try {
			RecievedPacket ret = client.read();
			if (ret != null) {
				if (ret.type == MessageType.DISCONNECT) {
					clients.remove(client.hash);
				}
				broadCast(ret);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	protected void write(ConnectedClient client) {
		try {
			client.send();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void broadCast(RecievedPacket packet) {
		Stream<ConnectedClient> str = clients.values().parallelStream();
		if(!packet.include.isEmpty()) {
			str = str.filter(c -> packet.include.contains(c.hash));
		}
		
		byte[] header = packet.getHeaderOutput();
		str.filter(c -> !packet.exclude.contains(c.hash)).forEach(c -> {
			try {
				c.queue.put(ByteBuffer.wrap(header));
				c.queue.put(ByteBuffer.wrap(packet.getData()));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
	}
	
	public void stop() {
		//TODO
	}
	
	public static String getIP() {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com/").openStream()))) {
			return br.readLine();
		} catch (IOException e) {
			SwingDialogs.error("Unable to find the server's IP", "%e%", e, true);
			return null;
		}
	}
	
	protected class ConnectedClient {
		private final SocketChannel ch;
		private final String hash;
		private final Logger logger;
		
		final LinkedBlockingQueue<ByteBuffer> queue;
		
		public ConnectedClient(SocketChannel ch, Logger logger) throws IOException {
			this.ch = ch;
			String address = ch.getRemoteAddress().toString();
			hash = Hashes.SHA_512.toHex(InPut.from(address + new Random().nextLong()));
			this.logger = logger;
			this.logger.setPrefix(address);
			this.logger.log("Client hash : " + hash);
			queue = new LinkedBlockingQueue<ByteBuffer>();
			
		}
		
		public void send() throws IOException {
			int read = -1;
			ByteBuffer buf;
			while((buf = queue.peek()) != null || read != 0) {
				logger.logVerbose("Write data : " + (read = ch.write(buf)) + "byte(s)");
				if(!buf.hasRemaining()) queue.poll();
			}
		}

		private ByteBuffer sizes = ByteBuffer.allocate(Protocol.HEADERSIZEBUFFERSIZE);
		private int headerLen = -1;
		private int dataLen = -1;
		
		private ByteBuffer header = null;
		private ByteBuffer data = null;
		private RecievedPacket packet;
		
		public RecievedPacket read() throws IOException {
			if(sizes.hasRemaining()) { //sizes must be read first
				logger.logVerbose("Read packet sizes : " + Protocol.formatExactByteSize(ch.read(sizes)));
				if(!sizes.hasRemaining()) {
					sizes.flip();
					headerLen = sizes.getInt();
					dataLen = sizes.getInt();
					header = ByteBuffer.allocate(headerLen);
					data = ByteBuffer.allocate(dataLen);
					logger.logVerbose("Packet sizes recieved - header length : " + headerLen + "bytes, data length : " + Protocol.formatExactByteSize(dataLen));
				}
			} else if(header != null) { //header must be read first
				logger.logVerbose("Read header : " + ch.read(header) + "byte(s)");
				if(!header.hasRemaining()) { // header is fully read
					header.flip();
					String[] meta = Protocol.METADATACHARSET.decode(header).toString().split(":");
					header = null;
					if (Stream.of(MessageType.values()).map(MessageType::getStr).anyMatch(meta[0]::equals)) {
						Set<String> includeSet = Set.of(), excludeSet = Set.of();
						for(int i = 1; i < meta.length; i++) {
							if(meta[i].startsWith("EXCLUDE=")) {
								excludeSet = Set.of(meta[i].substring("EXCLUDE=".length()).split(","));
							} else if(meta[i].startsWith("INCLUDE=")) {
								includeSet = Set.of(meta[i].substring("INCLUDE=".length()).split(","));
							} else {
								//TODO : error
							}
						}
						packet = new RecievedPacket(hash, MessageType.valueOf(meta[0]), includeSet, excludeSet);
					} else {
						logger.log("Unknown message type : \"" + meta[0] + "\n");
						//TODO : error, have to disconnect? just clear?
					}
				}
			} else { //header is read. now read data
				logger.logVerbose("Read packet data : " + Protocol.formatExactByteSize(ch.read(data)));
				if(!data.hasRemaining()) { //data is fully read
					data.flip();
					packet.putData(data.array());
					clear();
					return packet;
				}
			}			
			return null;
		}
		

		public void clear() {
			sizes = ByteBuffer.allocate(8);
			headerLen = -1;
			dataLen = -1;
			header = null;
			data = null;
		}
	}
}

