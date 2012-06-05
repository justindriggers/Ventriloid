package com.jtxdriggers.android.ventriloid;

import java.util.ArrayList;
import java.util.HashMap;

public class ItemData {

	public ArrayList<Item.Channel> channels = new ArrayList<Item.Channel>();
	public ArrayList<ArrayList<Item.User>> users = new ArrayList<ArrayList<Item.User>>();
	
	public ItemData() {
		Item i = new Item();
		channels.add(i.new Channel());
		users.add(new ArrayList<Item.User>());
	}
	
	public void setLobby(Item.Channel lobby) {
		channels.set(0, lobby);
	}
	
	public void addChannel(Item.Channel channel) {
		int i;
		for (i = 0; i < channels.size(); i++) {
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
		int i;
		for (i = 0; i < channels.size(); i++) {
			if (channels.get(i).id == user.parent) {
				user.indent = channels.get(i).indent + "     ";

				users.get(i).add(user);
				return;
			}
		}
	}
	
	public ArrayList<HashMap<String, Object>> getChannels() {
		ArrayList<HashMap<String, Object>> channelList = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < channels.size(); i++) {
			HashMap<String, Object> c = channels.get(i).toHashMap();
			channelList.add(c);
		}
		
		return channelList;
	}
	
	public ArrayList<ArrayList<HashMap<String, Object>>> getUsers() {
		ArrayList<ArrayList<HashMap<String, Object>>> userList = new ArrayList<ArrayList<HashMap<String, Object>>>();
		for (int i = 0; i < channels.size(); i++) {
			userList.add(new ArrayList<HashMap<String, Object>>());
			for (int j = 0; j < users.get(i).size(); j++) {
				HashMap<String, Object> u = users.get(i).get(j).toHashMap();
				userList.get(i).add(u);
			}
		}
		
		return userList;
	}
	
}
