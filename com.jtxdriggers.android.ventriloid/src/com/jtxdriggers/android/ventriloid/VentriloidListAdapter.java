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
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class VentriloidListAdapter extends SimpleExpandableListAdapter {
	private List<? extends Item.Channel> mGroupData;
	private List<? extends List<? extends Item.User>> mChildData;
	private String[] mChildFrom;
	private int[] mChildTo;

	public VentriloidListAdapter(Context context, List<? extends Item.Channel> groupData,
		int groupLayout, String[] groupFrom, int[] groupTo,
		List<? extends List<? extends Item.User>> childData,
		int childLayout, String[] childFrom, int[] childTo) {
		super(context, getChannelHashMaps(groupData), groupLayout, groupFrom, groupTo, getUserHashMaps(childData), childLayout, childFrom, childTo);

		mGroupData = groupData;
		mChildData = childData;
		mChildFrom = childFrom;
		mChildTo = childTo;
	}
	
	public VentriloidListAdapter(Context context, Item.Channel groupData,
			int groupLayout, String[] groupFrom, int[] groupTo,
			List<? extends Item.User> childData,
			int childLayout, String[] childFrom, int[] childTo) {
		super(context, getChannelHashMaps(getCurrentChannel(groupData)), groupLayout, groupFrom, groupTo, getUserHashMaps(getCurrentUsers(childData)), childLayout, childFrom, childTo);
		
		mGroupData = getCurrentChannel(groupData);
		mChildData = getCurrentUsers(childData);
		mChildFrom = childFrom;
		mChildTo = childTo;
	}
	 
	private static List<? extends Map<String, ?>> getChannelHashMaps(List<? extends Item.Channel> channels) {
		ArrayList<HashMap<String, Object>> channelList = new ArrayList<HashMap<String, Object>>();
		for (int i = 0; i < channels.size(); i++) {
			HashMap<String, Object> c = channels.get(i).toHashMap();
			channelList.add(c);
		}
		return channelList;
	}
	
	public static ArrayList<Item.Channel> getCurrentChannel(Item.Channel currentChannel) {
		ArrayList<Item.Channel> c = new ArrayList<Item.Channel>();
		c.add(currentChannel);
		return c;
	}
	 
	private static List<? extends List<? extends Map<String, ?>>> getUserHashMaps(List<? extends List<? extends Item.User>> users) {
		ArrayList<ArrayList<HashMap<String, Object>>> userList = new ArrayList<ArrayList<HashMap<String, Object>>>();
		for (int i = 0; i < users.size(); i++) {
			userList.add(new ArrayList<HashMap<String, Object>>());
			for (int j = 0; j < users.get(i).size(); j++) {
				HashMap<String, Object> u = users.get(i).get(j).toHashMap();
				userList.get(i).add(u);
			}
		}
		return userList;
	}
	

	public static ArrayList<ArrayList<Item.User>> getCurrentUsers(List<? extends Item.User> currentUsers) {
		ArrayList<ArrayList<Item.User>> u = new ArrayList<ArrayList<Item.User>>();
		u.add(new ArrayList<Item.User>());
		for (int i = 0; i < currentUsers.size(); i++) {
			u.get(0).add(currentUsers.get(i));
		}
		return u;
	}

	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		View v;
		 
		if (convertView == null)
			v = newChildView(isLastChild, parent);
		else
			v = convertView;
	     
		bindView(v, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo);
		return v;
	}
	
	public int getGroupCount() {
		return mGroupData.size();
	}
	
	public int getChildrenCount(int groupPosition) {
		return mChildData.get(groupPosition).size();
	}
	 
	private void bindView(View view, Item.User data, String[] from, int[] to) {
		HashMap<String, Object> map = data.toHashMap();
		for (int i = 0; i < to.length; i++) {
			if (i == 1) {
				ImageView imgV = (ImageView) view.findViewById(to[i]);
				if (imgV != null)
					imgV.setImageResource((Integer) map.get(from[i]));
			} else {
				TextView v = (TextView) view.findViewById(to[i]);
				if (v != null)
					v.setText(map.get(from[i]).toString());
			}
		}
	}

}
