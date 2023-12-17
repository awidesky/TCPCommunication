package io.github.awidesky.tcpCommunication;

public class Protocol {

	public final static int HEADERSIZEBUFFERSIZE = 8;
	
	
	/**
	 * Result will be integer number only
	 * */
	public static String formatExactByteSize(long bytes) {
		
		if(bytes == 0L) return "0byte";
		
		
		String arr[] = {"B", "KB", "MB", "GB"};
		
		for(String prefix : arr) {
			if(bytes % 1024 != 0) {
				return bytes + prefix;
			} else { bytes /= 1024; }
		}
		
		return bytes + "TB";
		
	}
}
