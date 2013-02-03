/*
 * Copyright 2012 Justin Driggers <jtxdriggers@gmail.com>
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

import java.util.ArrayList;
import java.util.HashMap;

public class ItemData {

	private ArrayList<Item.Channel> channels = new ArrayList<Item.Channel>();
	private ArrayList<Item.Channel> currentChannels = new ArrayList<Item.Channel>(); 
	private ArrayList<ArrayList<Item.User>> users = new ArrayList<ArrayList<Item.User>>();
	private ArrayList<ArrayList<Item.User>> currentUsers = new ArrayList<ArrayList<Item.User>>();
	
	private HashMap<Short, ArrayList<ChatMessage>> chats = new HashMap<Short, ArrayList<ChatMessage>>();
	private ArrayList<Item.User> chatUsers = new ArrayList<Item.User>();
	
	private int ping = 0;
	private String comment = "", url = "", integrationText = "";
	private boolean inChat = false;
	
	public ItemData() {
		Item i = new Item();
		channels.add(i.new Channel());
		currentChannels.add(i.new Channel());
		users.add(new ArrayList<Item.User>());
		currentUsers.add(new ArrayList<Item.User>());
		chats.put((short) 0, new ArrayList<ChatMessage>());
	}
	
	public void addChannel(Item.Channel channel) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).id == channel.parent) {
				channel.indent = channels.get(i).indent + "     ";
				i++;
				
				while (i < channels.size() && channels.get(i).parent == channel.parent)
					i++;
				
				channels.add(i, channel);
				users.add(i, new ArrayList<Item.User>());
				return;
			}
		}
	}
	
	public void addCurrentUser(Item.User user) {
		if (user.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
			currentUsers.get(0).add(user);
	}
	
	public void addUser(Item.User user) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).id == user.parent) {
				user.indent = channels.get(i).indent + "     ";
				users.get(i).add(user);
				return;
			}
		}
	}
	
	public ArrayList<Item.Channel> getChannels() {
		return channels;
	}
	
	public Item.Channel getChannelById(short id) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).id == id) {
				return channels.get(i);
			}
		}
		return null;
	}
	
	public ArrayList<Item.Channel> getCurrentChannel() {
		return currentChannels;
	}
	
	public Item.User getCurrentUserById(short id) {
		for (int i = 0; i < currentUsers.get(0).size(); i++) {
			if (currentUsers.get(0).get(i).id == id)
				return currentUsers.get(0).get(i);
		}
		return null;
	}
	
	public ArrayList<ArrayList<Item.User>> getCurrentUsers() {
		return currentUsers;
	}
	
	public ArrayList<ArrayList<Item.User>> getUsers() {
		return users;
	}
	
	public Item.User getUserById(short id) {
		for (int i = 0; i < channels.size(); i++) {
			for (int j = 0; j < users.get(i).size(); j++) {
				if (users.get(i).get(j).id == id)
					return users.get(i).get(j);
			}
		}
		return null;
	}
	
	public void removeCurrentUser(short id) {
		for (int i = 0; i < currentUsers.get(0).size(); i++) {
			if (currentUsers.get(0).get(i).id == id) {
				currentUsers.get(0).remove(i);
				return;
			}
		}
	}
	
	public void removeUser(short id) {
		for (int i = 0; i < channels.size(); i++) {
			for (int j = 0; j < users.get(i).size(); j++) {
				if (users.get(i).get(j).id == id) {
					users.get(i).remove(j);
					return;
				}
			}
		}
	}
	
	public void setCurrentChannel(short id) {
		currentChannels.clear();
		currentUsers.get(0).clear();
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).id == id) {
				currentChannels.add(0, channels.get(i));
				
				for (int j = 0; j < users.get(i).size(); j++) {
					currentUsers.get(0).add(users.get(i).get(j));
				}
				return;
			}
		}
	}
	
	public void setLobby(Item.Channel lobby) {
		channels.set(0, lobby);
		currentChannels.set(0, lobby);
	}
	
	public void setXmit(short id, int xmit) {
		Item.User u = getUserById(id);
		try {
			u.xmit = xmit;
		} catch (NullPointerException e) { }
	}
	
	public int getPing() {
		return ping;
	}
	
	public void setPing(int ping) {
		this.ping = ping;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getIntegrationText() {
		return integrationText;
	}

	public void setIntegrationText(String integrationText) {
		this.integrationText = integrationText;
	}
	
	public void createChat(short id) {
		chats.put(id, new ArrayList<ChatMessage>());
	}
	
	public ArrayList<ChatMessage> getChat(short id) {
		return chats.get(id);
	}
	
	public void addMessage(short id, String username, String message) {
		getChat(id).add(new ChatMessage(username, ": " + message));
	}
	
	public void addChatUser(short id) {
		Item.User user = getUserById(id);
		user.inChat = true;
		user.updateStatus();
		if (inChat) {
			chatUsers.add(user);
			getChat((short) 0).add(new ChatMessage(user.name, " has joined the chat."));
		}
	}
	
	public void removeChatUser(short id) {
		Item.User user = getUserById(id);
		user.inChat = false;
		user.updateStatus();
		if (inChat) {
			for (int i = 0; i < chatUsers.size(); i++) {
				if (user.id == id) {
					chatUsers.remove(i);
					break;
				}
			}
			getChat((short) 0).add(new ChatMessage(user.name, " has left the chat."));
		}
	}
	
	public void joinChat() {
		VentriloInterface.joinchat();
		inChat = true;
	}
	
	public void leaveChat() {
		VentriloInterface.leavechat();
		inChat = false;
	}
	
}
