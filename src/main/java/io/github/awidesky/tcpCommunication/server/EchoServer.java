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
			if(ret.length == 0) {
				//a packet that tells client that the connection will be over
				client.queue.put(ByteBuffer.allocate(Protocol.HeaderSize).putInt(Protocol.Goodbye).flip());
				//empty object flag that tells the "server" to close the connection with  
				client.queue.put(ByteBuffer.allocate(0));
			} else if (ret != null) {
				broadCast(ret);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void write(Connection client) {
		try {
			client.send();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void broadCast(byte[] packet) {
		clients.parallelStream().forEach(c -> {
			try {
				ByteBuffer b = ByteBuffer.allocate(Protocol.HeaderSize + packet.length).putInt(packet.length).put(packet);
				c.queue.put(b.flip());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		
	}
}

