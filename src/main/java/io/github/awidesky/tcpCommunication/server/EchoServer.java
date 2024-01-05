package io.github.awidesky.tcpCommunication.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import io.github.awidesky.tcpCommunication.Protocol;

public class EchoServer extends Server {

	public EchoServer() throws IOException {
		super();
	}
	public EchoServer(OutputStream os) throws IOException {
		super(os);
	}

	@Override
	protected void read(Connection client) {
		try {
			byte[] ret = client.read();
			if (ret != null) {
				broadCast(ret);
			}
		} catch (IOException e) {
			
			// TODO Auto-generated catch block
			logger.log("Exception on client : " + client.getHash());
			logger.log(e);
		}
	}

	@Override
	protected void write(Connection client) {
		try {
			client.send();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.log("Exception on client : " + client.getHash());
			logger.log(e);
		}
	}

	private void broadCast(byte[] packet) {
		clients.parallelStream().forEach(c -> {
			try {
				ByteBuffer b = ByteBuffer.allocate(Protocol.HeaderSize + packet.length).putInt(packet.length).put(packet);
				c.queue.put(b.flip());
			} catch (InterruptedException e) {
				logger.log(e);
			}
		});
		
	}
}

