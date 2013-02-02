package com.jtxdriggers.android.ventriloid;

import java.util.Date;

public class ChatMessage {
	
	public static final short JOIN = (short) -1, LEAVE = (short) -2;
	
	private String username;
	private String message;
	private Date timestamp;
	
	public ChatMessage(String username, String message) {
		this.username = username;
		this.message = message;
		timestamp = new Date();
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getMessage() {
		return message;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}

}
