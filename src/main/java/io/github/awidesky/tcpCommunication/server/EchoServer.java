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
		System.out.println("read");
		try {
			byte[] ret = client.read();
			if(ret.length == 0) {
				client.queue.add(ByteBuffer.allocate(0));
			} else if (ret != null) {
				broadCast(ret);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void write(Connection client) {
		System.out.println("write");
		try {
			client.send();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void broadCast(byte[] packet) {
		clients.values().parallelStream().forEach(c -> {
			try {
				c.queue.put(ByteBuffer.allocate(Protocol.HeaderSize).putInt(packet.length));
				c.queue.put(ByteBuffer.wrap(packet));
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
	}
}

