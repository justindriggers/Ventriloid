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

import java.util.ArrayList;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.TextView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

public class VentriloidListAdapter extends BaseExpandableListAdapter {
	
	private Context mContext;
	private ArrayList<Item.Channel> channels;
	private ArrayList<ArrayList<Item.User>> users; 
	private boolean isChannelView;

	public VentriloidListAdapter(Context context, boolean channelView) {
		mContext = context;
		isChannelView = channelView;
	}
	
	public void setContent(ItemData items) {
		channels = isChannelView ? items.getCurrentChannel() : items.getChannels();
		users = isChannelView ? items.getCurrentUsers() : items.getUsers();
		notifyDataSetChanged();
	}

	@Override
	public Item.User getChild(int groupPosition, int childPosition) {
		try {
			return users.get(groupPosition).get(childPosition);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		try {
			return users.get(groupPosition).get(childPosition).id;
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		Item.User user = getChild(groupPosition, childPosition);
		
		if (convertView == null)
			convertView = LayoutInflater.inflate(mContext, R.layout.user_row);
		
		if (user == null)
			return convertView;
		
		TextView indent = (TextView) convertView.findViewById(R.id.indent);
		indent.setText(isChannelView ? "     " : user.indent);
		
		ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
		imageView.setImageResource(user.xmit);
		
		TextView textView = (TextView) convertView.findViewById(R.id.title);
		textView.setText(user.status + user.formatRank(user.rank) + user.name + user.formatComment(user.url, user.comment) + user.formatIntegration(user.integration));
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		try {
			return users.get(groupPosition).size();
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
	}

	@Override
	public Item.Channel getGroup(int groupPosition) {
		return channels.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return channels.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return channels.get(groupPosition).id;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		Item.Channel channel = getGroup(groupPosition);
		if (convertView == null)
			convertView = LayoutInflater.inflate(mContext, R.layout.channel_row);
		
		TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
		textView.setText((isChannelView ? "" : channel.indent) + channel.status + channel.name + channel.formatComment(channel.comment));
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

}
