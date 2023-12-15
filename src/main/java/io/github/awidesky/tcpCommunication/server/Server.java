package io.github.awidesky.tcpCommunication.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.LoggerThread;
import io.github.awidesky.guiUtil.SwingDialogs;

public class Server {

	private final ExecutorService pool;
	private final Selector selector;
	private final ServerSocketChannel serverSocket;
	private final int port;
	
	private final LoggerThread loggerThread = new LoggerThread();
	private final Logger logger;
	
	public Server(int port) throws IOException {
		this(port, Runtime.getRuntime().availableProcessors());
	}

	public Server(int port, int threads) throws IOException {
		serverSocket = ServerSocketChannel.open();
		loggerThread.start();
		try {
			this.port = port;
			pool = Executors.newFixedThreadPool(threads);
			selector = Selector.open();
			serverSocket.configureBlocking(false);
			serverSocket.bind(new InetSocketAddress(this.port));
			serverSocket.register(selector, SelectionKey.OP_ACCEPT);
			logger = loggerThread.getLogger("[Server|" + getIP() + ":" + port + "] ");
			logger.log("Server is opended to port " + port);
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
						accept();
					} else if (key.isReadable()) {
						read(key.channel());
					} else if (key.isWritable()) {
						write(key.channel());
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
		
	}

	protected void read(SelectableChannel channel) {
		// TODO Auto-generated method stub
		
	}
	
	protected void write(SelectableChannel channel) {
		// TODO Auto-generated method stub
		
	}

	public void stop() {
		
	}
	
	public static String getIP() {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com/").openStream()))) {
			return br.readLine();
		} catch (IOException e) {
			SwingDialogs.error("Unable to find this PC's IP", "%e%", e, true);
			return null;
		}
	}
}
