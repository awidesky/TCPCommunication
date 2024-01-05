package io.github.awidesky.tcpCommunication;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Protocol {

	public static final int HeaderSize = 4;
	public static final int Goodbye = -1;

	public static final Charset METADATACHARSET = StandardCharsets.UTF_8;
	
	
	/**
	 * Result will be integer number only
	 * */
	public static String formatExactByteSize(long bytes) {
		
		if(bytes == 0L) return "0byte";
		
		
		String arr[] = {"B", "KiB", "MiB", "GiB"};
		
		for(String prefix : arr) {
			if(bytes % 1024 != 0) {
				return bytes + prefix;
			} else { bytes /= 1024; }
		}
		
		return bytes + "TB";
		
	}
}
