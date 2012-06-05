package com.jtxdriggers.android.ventriloid;

import java.util.ArrayList;

public class ItemData {

	private ArrayList<Item.Channel> channels = new ArrayList<Item.Channel>();
	private ArrayList<ArrayList<Item.User>> users = new ArrayList<ArrayList<Item.User>>();
	
	public ItemData() {
		Item i = new Item();
		channels.add(i.new Channel());
		users.add(new ArrayList<Item.User>());
	}
	
	public void setLobby(Item.Channel lobby) {
		channels.set(0, lobby);
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
	
	public void addUser(Item.User user) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).id == user.parent) {
				user.indent = channels.get(i).indent + "     ";

				users.get(i).add(user);
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
	
	public ArrayList<Item.Channel> getChannels() {
		return channels;
	}
	
	public Item.Channel getChannelById(short id) {
		for (int i = 0; i < channels.size(); i++) {
			if (channels.get(i).id == id)
				return channels.get(i);
		}
		return null;
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
	
}
