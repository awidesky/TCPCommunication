package io.github.awidesky.tcpCommunication;

public enum MessageType {

	MESSAGE("MSG"),
	DISCONNECT("BYE"),
	ANNOUNCEMENT("ANN");
	
	private String str;
	
	private MessageType(String str) {
		this.str = str;
	}

	public String getStr() {
		return str;
	}
}
