package io.github.awidesky.tcpCommunication;

public enum MessageType {

	/* The packet contains the message(actual data that client has sent) */
	MESSAGE("MSG"),
	/* A client disconnected */
	DISCONNECT("BYE"),
	/* Server announcement */
	ANNOUNCEMENT("ANN"),
	/* Hashes(id) of all currently connected clients(first one is the requester's) */
	CLIENTHASHES("IDS");
	
	
	private String str;
	
	private MessageType(String str) {
		this.str = str;
	}

	public String getStr() {
		return str;
	}
}
