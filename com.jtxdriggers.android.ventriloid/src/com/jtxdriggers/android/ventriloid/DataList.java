/*
 * Copyright 2010 Justin Driggers <jtxdriggers@gmail.com>
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
import java.util.Iterator;
import java.util.ListIterator;

public class DataList {
	
	public static ArrayList<HashMap<String, String>> channeldata = new ArrayList<HashMap<String, String>>();
	public static ArrayList<ArrayList<HashMap<String, String>>> userdata = new ArrayList<ArrayList<HashMap<String, String>>>();
	public static ArrayList<HashMap<String, String>> users = new ArrayList<HashMap<String, String>>();
	
	public static void add(VentriloidListItem item) {
		String indent = "";
		switch (item.type) {
		case VentriloidListItem.CHANNEL:
			if (item.id == 0) {
				channeldata.add(item.toHashMap());
				userdata.add(new ArrayList<HashMap<String, String>>());
			} else {
				indent = "     ";
				if (item.parentid == 0) {
					item.indent = indent;
					channeldata.add(item.toHashMap());
					userdata.add(new ArrayList<HashMap<String, String>>());
				} else {
					int position = 0;
					for(ListIterator<HashMap<String, String>> iterator = channeldata.listIterator(); iterator.hasNext(); ) {
						if(Short.parseShort(iterator.next().get("id")) == item.parentid) {
							short parent = item.parentid;
							while (parent != 0) {
								VentriloidListItem parentItem = new VentriloidListItem(VentriloidListItem.CHANNEL, parent);
								parent = parentItem.parentid;
								indent = indent + "     ";
							}
							item.indent = indent;
							position = iterator.nextIndex();
							while (iterator.hasNext() && Short.parseShort(iterator.next().get("parentid")) == item.parentid)
								position = iterator.nextIndex();
						}
					}
					channeldata.add(position, item.toHashMap());
					userdata.add(position, new ArrayList<HashMap<String, String>>());
				}
			}
			break;
			
		case VentriloidListItem.USER:
			int channel = 0;
			int position = 0;
			if (!item.rank.equals("") && !item.rank.startsWith("["))
				item.rank = "[" + item.rank + "] ";
			for(ListIterator<HashMap<String, String>> iterator = channeldata.listIterator(); iterator.hasNext(); ) {
				HashMap<String, String> current = iterator.next();
				if((current.get("id")).equals(Short.toString(item.parentid))) {
					channel = iterator.previousIndex();
					item.indent = current.get("indent") + "     ";
					break;
				}
			}
			for(ListIterator<HashMap<String, String>> subiterator = userdata.get(channel).listIterator(); subiterator.hasNext(); ) {
				HashMap<String, String> next = subiterator.next();
				if ((item.rank + item.name).compareToIgnoreCase((next.get("rank") + next.get("name"))) > 0)
					position = subiterator.nextIndex();
				else {
					position = subiterator.previousIndex();
					break;
				}
			}
			userdata.get(channel).add(position, item.toHashMap());
			break;
		}
	}
	
	public static void addLobby (String channelname) {
		VentriloidListItem lobby = new VentriloidListItem();//getChannel((short)0));
		lobby.name = channelname;
		lobby.indent = "";
		channeldata.add(lobby.toHashMap());
		//channeldata.set(0, lobby.toHashMap());
	}
	
	public static void addUser(VentriloidListItem item) {
		users.add(item.toHashMap());
	}
	
	public static void changeUserChannel(short userid, short channelid) {
		for(ListIterator<HashMap<String, String>> iterator = users.listIterator(); iterator.hasNext(); ) {
			HashMap<String, String> user = iterator.next();
			if (Short.parseShort(user.get("id")) == userid) {
				user.put("parentid", Short.toString(channelid));
				return;
			}
		}
	}
	
	public static void addUserToChat(short id) {
		for(ListIterator<HashMap<String, String>> iterator = users.listIterator(); iterator.hasNext(); ) {
			HashMap<String, String> user = iterator.next();
			if (Short.parseShort(user.get("id")) == id) {
				user.put("userinchat", "1");
				return;
			}
		}
	}
	
	public static void removeUserFromChat(short id) {
		for(ListIterator<HashMap<String, String>> iterator = users.listIterator(); iterator.hasNext(); ) {
			HashMap<String, String> user = iterator.next();
			if (Short.parseShort(user.get("id")) == id) {
				user.put("userinchat", "0");
				return;
			}
		}
	}
	
	public static boolean isUserInChat(short id) {
		for(ListIterator<HashMap<String, String>> iterator = users.listIterator(); iterator.hasNext(); ) {
			HashMap<String, String> user = iterator.next();
			if (Short.parseShort(user.get("id")) == id) {
				if (Integer.parseInt(user.get("userinchat")) == 1)
					return true;
				else
					return false;
			}
		}
		return false;
	}
	
	public static int getChannelIndex(short channelid) {
		for(ListIterator<HashMap<String, String>> iterator = channeldata.listIterator(); iterator.hasNext(); ) {
			if(Short.parseShort(iterator.next().get("id")) == channelid) {
				return iterator.previousIndex();
			}
		}
		return -1;
	}
	
	public static HashMap<String, String> getChannel(short id) {
		for(ListIterator<HashMap<String, String>> iterator = channeldata.listIterator(); iterator.hasNext(); ) {
			HashMap<String, String> channel = iterator.next();
			if (Short.parseShort(channel.get("id")) == id) {
				return channeldata.get(iterator.previousIndex());
			}
		}
		return null;
	}
	
	public static HashMap<String, String> getUser(short userid) {
		for(Iterator<HashMap<String, String>> iterator = users.iterator(); iterator.hasNext(); ) {
			HashMap<String, String> user = iterator.next();
			if(Short.parseShort(user.get("id")) == userid) {
				return user;
			}
		}
		return null;
	}
	
	public static short getUserChannel(short userid) {
		for(ListIterator<HashMap<String, String>> iterator = users.listIterator(); iterator.hasNext(); ) {
			if(Short.parseShort(iterator.next().get("id")) == userid) {
					return Short.parseShort(iterator.previous().get("parentid"));
				}
			}
		return -1;
	}
	
	public static void delUser(short userid) {
		for(ListIterator<ArrayList<HashMap<String, String>>> iterator = userdata.listIterator(); iterator.hasNext(); ) {
			for(Iterator<HashMap<String, String>> subiterator = iterator.next().iterator(); subiterator.hasNext(); ) {
				if(Short.parseShort(subiterator.next().get("id")) == userid) {
					subiterator.remove();
					return;
				}
			}
		}
	}
	
	public static void removeUser(short userid) {
		for(Iterator<HashMap<String, String>> iterator = users.iterator(); iterator.hasNext(); ) {
			if(Short.parseShort(iterator.next().get("id")) == userid) {
				iterator.remove();
				return;
			}
		}
	}
	
	public static void clear() {
		channeldata.clear();
		userdata.clear();
		users.clear();
	}
	
}