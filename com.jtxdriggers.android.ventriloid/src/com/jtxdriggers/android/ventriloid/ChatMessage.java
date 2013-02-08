/*
 * Copyright 2013 Justin Driggers <jtxdriggers@gmail.com>
 *
 * This file is part of Ventriloid.
 *
 * Ventriloid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ventriloid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ventriloid.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jtxdriggers.android.ventriloid;

import java.util.Date;

public class ChatMessage {
	
	public static final short JOIN = (short) -1, LEAVE = (short) -2;
	
	public static final int TYPE_MESSAGE = 0, TYPE_ENTER_CHAT = 1, TYPE_LEAVE_CHAT = 2, TYPE_CLOSE_CHAT = 3, TYPE_REOPEN_CHAT = 4, TYPE_ERROR = -1, TYPE_DISCONNECT = -2;
	
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
	
	public ChatMessage(String username, int type) {
		this.username = username;
		this.type = type;
		switch (type) {
		case TYPE_ENTER_CHAT:
			message = "has joined the chat.";
			break;
		case TYPE_LEAVE_CHAT:
			message = "has left the chat.";
			break;
		case TYPE_CLOSE_CHAT:
			message = "Private chat session has been closed.";
			break;
		case TYPE_REOPEN_CHAT:
			message = "Private session has been reopened.";
			break;
		case TYPE_ERROR:
			message = "Error sending message.";
			break;
		case TYPE_DISCONNECT:
			message = "has disconnected.";
		}
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
	
	public int getType() {
		return type;
	}

}