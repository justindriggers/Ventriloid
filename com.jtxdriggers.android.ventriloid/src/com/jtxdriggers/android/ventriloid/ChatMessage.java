package com.jtxdriggers.android.ventriloid;

import java.util.Date;

public class ChatMessage {
	
	public static final short JOIN = (short) -1, LEAVE = (short) -2;
	
	public static final int TYPE_MESSAGE = 0, TYPE_NOTIFICATION = 1;
	
	private String username;
	private String message;
	private Date timestamp;
	private int type;
	
	public ChatMessage(String username, String message) {
		this.username = username;
		this.message = message;
		timestamp = new Date();
		type = TYPE_MESSAGE;
	}
	
	public ChatMessage(String username, boolean enter) {
		this.username = username;
		message = enter ? "has joined the chat."  : "has left the chat.";
		timestamp = new Date();
		type = TYPE_NOTIFICATION;
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
	
	public int getType() {
		return type;
	}

}