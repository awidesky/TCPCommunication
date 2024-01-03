package io.github.awidesky.tcpCommunication.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.LoggerThread;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.jCipherUtil.hash.Hashes;
import io.github.awidesky.jCipherUtil.messageInterface.InPut;
import io.github.awidesky.tcpCommunication.Protocol;

public abstract class Server {

	private ExecutorService pool;
	private Selector selector;
	private ServerSocketChannel serverSocket;
	private int port;
	
	private final LoggerThread loggerThread = new LoggerThread();
	protected final Logger logger;
	
	protected final Set<Connection> clients = new HashSet<>();
	
	public Server() throws IOException {
		this(System.out);
	}

	public Server(OutputStream os) throws IOException {
		loggerThread.setLogDestination(os, true);
		loggerThread.setDatePrefixAllChildren(new SimpleDateFormat("[kk:mm:ss.SSS]"));
		logger = loggerThread.getLogger("[Server|" + getIP() + ":" + port + "] ");
		loggerThread.start();
	}
	
	public void open(int port) throws IOException {
		open(port, Runtime.getRuntime().availableProcessors());
	}
	
	public void open(int port, int threads) throws IOException {
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
			logger.log("Server is opended with " + threads + " threads to port " + port);
		} catch (IOException e) {
			if(serverSocket.isOpen()) stop();
			throw e;
		}
		pool.submit(this::listen);
	}
	
	private void listen() {
		while (true) {
			clients.stream().filter(Connection::isWriteOnly).forEach(c -> {
				try {
					c.ch.register(selector, SelectionKey.OP_WRITE, c);
				} catch (ClosedChannelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
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
						pool.submit(() -> read((Connection) key.attachment()));
					} else if (key.isWritable()) {
						pool.submit(() -> write((Connection) key.attachment()));
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
			if(sc == null) return;
			
			sc.configureBlocking(false);
			Connection client = new Connection(sc, loggerThread.getLogger());
			clients.add(client);
			sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, client);
			logger.log("Connected : " + sc.getRemoteAddress() + "(hash : " + client.hash + ")");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected abstract void read(Connection client);
	protected abstract void write(Connection client);
	
	public void stop() {
		clients.parallelStream().forEach(Connection::close);
		clients.clear();
		try {
			serverSocket.close();
		} catch (IOException e) {
			SwingDialogs.error("Unable to close ServerSocket", "%e%", e, false);
		}
		serverSocket = null;
	}
	
	public static String getIP() {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com/").openStream()))) {
			return br.readLine();
		} catch (Exception e) {
			//SwingDialogs.error("Unable to find the server's IP", "%e%", e, true);
			return "localhost";
		}
	}
	
	
	protected class Connection {
		private final SocketChannel ch;
		private final String hash;
		private final Logger logger;
		
		final LinkedBlockingQueue<ByteBuffer> queue;

		private boolean readNoMore = false;
		
		public Connection(SocketChannel ch, Logger logger) throws IOException {
			this.ch = ch;
			String address = ch.getRemoteAddress().toString();
			hash = Hashes.SHA_512_256.toHex(InPut.from(address + new Random().nextLong())).substring(0, 8);
			this.logger = logger;
			this.logger.setPrefix("[Connection " + hash + "] ");
			this.logger.log("Connected client hash : " + hash + ", address : " + address);
			queue = new LinkedBlockingQueue<ByteBuffer>();
			
		}
		
		public void send() throws IOException {
			int read = -1;
			ByteBuffer buf;
			while((buf = queue.peek()) != null || read != 0) {
				if(buf.capacity() == 0) {
					logger.log("Closing connection now...");
					close();
				}
				logger.log("Write data : " + (read = ch.write(buf)) + "byte(s)");
				if(!buf.hasRemaining()) queue.poll();
			}
		}

		private ByteBuffer header = ByteBuffer.allocate(Protocol.HeaderSize);
		private int packageLen = 0;
		
		private ByteBuffer data = null;
		
		public byte[] read() throws IOException {
			if(data == null) { //header must be read first
				logger.logVerbose("Read package header : " + Protocol.formatExactByteSize(ch.read(header)));
				if(!header.hasRemaining()) {
					packageLen = header.flip().getInt();
					switch(packageLen) {
					case Protocol.GoodbyeNow:
						readNoMore = true;
						logger.log("Client sent goodbye. Closing connection now without sending " + queue.size() + " package(s) in queue...");
						close();
						return null;
					case Protocol.Goodbye:
						readNoMore = true;
						logger.log("Client sent goodbye. Closing connection after sending " + queue.size() + " package(s) in queue...");
						clear();
						return new byte[0];
								
					}
					data = ByteBuffer.allocate(packageLen);
					logger.log("Packet size recieved - package length : " + Protocol.formatExactByteSize(packageLen));
				}
			} else { //header is read. now read data
				logger.log("Read package data : " + Protocol.formatExactByteSize(ch.read(data)));
				if(!data.hasRemaining()) { //data is fully read
					data.flip();
					byte[] ret = data.array();
					clear();
					return ret;
				}
			}			
			return null;
		}
		
		public boolean isWriteOnly() { return readNoMore; }

		public void clear() {
			header.clear();
			packageLen = -1;
			data = null;
		}
		
		public void close() {
			try {
				this.ch.close();
			} catch (IOException e) {
				SwingDialogs.error("Unable to close connection with : " + hash, "%e%", e, false);
			}
			clients.remove(this);
			clear();
		}
	}
}

