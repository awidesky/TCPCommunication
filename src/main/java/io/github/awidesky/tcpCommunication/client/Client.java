package io.github.awidesky.tcpCommunication.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;

import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SimpleLogger;
import io.github.awidesky.tcpCommunication.Protocol;

public class Client {

	private SocketChannel clientSocket;
	private int port;
	
	private final Logger logger;
	public Client() throws IOException {
		this(System.out);
	}

	public Client(OutputStream os) throws IOException {
		logger = new SimpleLogger(os);
		logger.setDatePrefix(new SimpleDateFormat("[kk:mm:ss.SSS]"));
	}
	
	public void connect(String ip, int port) throws IOException {
		if(clientSocket != null) {
			logger.log("Reconnecting to port " + port + " - close current server");
			stop();
			logger.log("Current server");
		}
		
		try {
			clientSocket = SocketChannel.open();
			this.port = port;
			clientSocket.configureBlocking(true);
			
			InetSocketAddress addr = new InetSocketAddress(ip, this.port);
			logger.log("Try connecting... : " + addr.toString());
			clientSocket.connect(addr);
			logger.log("Client connected to : " + clientSocket.getRemoteAddress().toString());
		} catch (IOException e) {
			if(clientSocket.isOpen()) stop();
			throw e;
		}
	}

	
	
	public void send(byte[] arr) throws IOException {
		send(arr, 0, arr.length);
	}
	public void send(byte[] arr, int offset, int len) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(arr, offset, len);
		logger.log("Sending " + Protocol.formatExactByteSize(len - offset) + " to server(total)");
		while(buf.hasRemaining()) {
			logger.log("Sent " + Protocol.formatExactByteSize(clientSocket.write(buf)));
		}
	}
	
	public byte[] read(int len) throws IOException {
		ByteBuffer buf = ByteBuffer.allocate(len);
		logger.log("Reading " + Protocol.formatExactByteSize(len) + " from server(total)");
		while(buf.hasRemaining()) {
			logger.log("Read " + Protocol.formatExactByteSize(clientSocket.read(buf)));
		}
		return buf.array();
	}
	
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
