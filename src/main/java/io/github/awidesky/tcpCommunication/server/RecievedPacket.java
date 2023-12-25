package io.github.awidesky.tcpCommunication.server;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.awidesky.tcpCommunication.MessageType;
import io.github.awidesky.tcpCommunication.Protocol;

public class RecievedPacket {

	public final Set<String> include;
	public final Set<String> exclude;
	
	public final String from;
	public final MessageType type;

	private byte[] data;
	private byte[] headerOutput;
	
	public RecievedPacket(String from, MessageType type, Set<String> include, Set<String> exclude) {
		this.from = from;
		this.type = type;
		this.include = include;
		this.exclude = exclude;
	}
	
	public void putData(byte[] data) {
		this.data = data;
		StringBuilder metaStr = new StringBuilder().append(type.getStr()).append(":").append(from);
		if(!include.isEmpty()) {
			metaStr.append(":").append("INCLUDE=").append(include.stream().collect(Collectors.joining(":")));
		}
		if(!exclude.isEmpty()) {
			metaStr.append(":").append("EXCLUDE=").append(exclude.stream().collect(Collectors.joining(":")));
		}
		byte[] headerStr = Protocol.METADATACHARSET.encode(metaStr.toString()).array();
		byte[] sizes = ByteBuffer.allocate(Protocol.HEADERSIZEBUFFERSIZE).putInt(headerStr.length).putInt(data.length).array();
		headerOutput = new byte[Protocol.HEADERSIZEBUFFERSIZE + headerStr.length];
		System.arraycopy(sizes, 0, headerOutput, 0, sizes.length);
		System.arraycopy(headerStr, 0, headerOutput, sizes.length, headerStr.length);
	}
	
	public byte[] getData() {
		return data;
	}
	
	public byte[] getHeaderOutput() {
		return headerOutput;
	}
}
