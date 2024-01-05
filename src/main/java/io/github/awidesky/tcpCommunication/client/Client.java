package io.github.awidesky.tcpCommunication.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;

import io.github.awidesky.guiUtil.Logger;
import io.github.awidesky.guiUtil.SimpleLogger;
import io.github.awidesky.guiUtil.SwingDialogs;
import io.github.awidesky.tcpCommunication.Protocol;

public class Client {

	private SocketChannel clientSocket;
	private int port;
	private String remoteAddress;
	
	private boolean closed;
	private final Logger logger;
	public Client() throws IOException {
		this(System.out);
	}

	public Client(OutputStream os) throws IOException {
		logger = new SimpleLogger(os);
		logger.setDatePrefix(new SimpleDateFormat("[kk:mm:ss.SSS]"));
		logger.setPrefix("[Client] ");
	}
	
	public void connect(String ip, int port) throws IOException {
		if(clientSocket != null) {
			SwingDialogs.error("Client is already opened!", "Client is already connected to : " + remoteAddress, null, true);
			return;
		}
		
		try {
			clientSocket = SocketChannel.open();
			this.port = port;
			clientSocket.configureBlocking(true);
			
			InetSocketAddress addr = new InetSocketAddress(ip, this.port);
			logger.log("Try connecting... : " + addr.toString());
			clientSocket.connect(addr);
			remoteAddress = clientSocket.getRemoteAddress().toString();
			logger.log("Client connected to : " + remoteAddress);
		} catch (IOException e) {
			if(clientSocket.isOpen()) disconnect();
			throw e;
		}
	}

	
	
	public void send(byte[] arr) throws IOException {
		send(arr, 0, arr.length);
	}
	public void send(byte[] arr, int offset, int len) throws IOException {
		if(closed) throw new ClosedChannelException();
		ByteBuffer header = ByteBuffer.allocate(Protocol.HeaderSize).putInt(len).flip();
		ByteBuffer buf = ByteBuffer.wrap(arr, offset, len);
		
		logger.log("Sending header (" + Protocol.formatExactByteSize(header.capacity()) + ") to server");
		while(header.hasRemaining()) clientSocket.write(header);
		logger.log("Sending package (" + Protocol.formatExactByteSize(len - offset) + ") to server...");
		while(buf.hasRemaining()) {
			logger.log("Sent " + Protocol.formatExactByteSize(clientSocket.write(buf)));
		}
	}
	
	public byte[] read() throws IOException {
		if(closed) return null;
		ByteBuffer header = ByteBuffer.allocate(Protocol.HeaderSize);
		while(header.hasRemaining()) clientSocket.read(header);
		int len = header.flip().getInt();
		if(len == Protocol.Goodbye) {
			logger.log("Server sent goodbye. Closing connection...");
			clientSocket.close();
			clientSocket = null;
			closed = true;
			return null;
		}
		
		ByteBuffer buf = ByteBuffer.allocate(len);
		logger.log("Reading " + Protocol.formatExactByteSize(len) + " from server(total)");
		while(buf.hasRemaining()) {
			logger.log("Read " + Protocol.formatExactByteSize(clientSocket.read(buf)));
		}
		return buf.array();
	}
	
	
	public void disconnect() throws IOException {
		logger.log("Sending goodbye to the server...");
		ByteBuffer buf = ByteBuffer.allocate(Protocol.HeaderSize).putInt(Protocol.Goodbye).flip();
		while(buf.hasRemaining()) clientSocket.write(buf);
		logger.log("Disconnecting...");
		clientSocket.close();
		closed = true;
		logger.log("Disconnected!");
	}
}
