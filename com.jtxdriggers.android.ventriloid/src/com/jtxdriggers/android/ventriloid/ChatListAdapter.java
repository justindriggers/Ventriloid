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
import android.graphics.Color;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class ChatListAdapter extends BaseAdapter {
	
	private Context mContext;
	private ArrayList<ChatMessage> messages;
	
	public ChatListAdapter(Context context, ArrayList<ChatMessage> messages) {
		mContext = context;
		this.messages = messages;
	}

	@Override
	public int getCount() {
		return messages.size();
	}

	@Override
	public ChatMessage getItem(int position) {
		return messages.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ChatMessage message = messages.get(position);
		
		switch (message.getType()) {
		case ChatMessage.TYPE_MESSAGE:
			convertView = LayoutInflater.inflate(mContext, R.layout.chat_message);
			
			TextView nameView = (TextView) convertView.findViewById(R.id.name);
			nameView.setText(message.getUsername() + ": ");
			
			TextView messageView = (TextView) convertView.findViewById(R.id.message);
			messageView.setText(message.getMessage());
			Linkify.addLinks(messageView, Linkify.ALL);
			break;
		case ChatMessage.TYPE_ENTER_CHAT:
		case ChatMessage.TYPE_LEAVE_CHAT:
		case ChatMessage.TYPE_DISCONNECT:
			convertView = LayoutInflater.inflate(mContext, R.layout.simple_list_item_1);
			
			TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
			textView.setText(message.getUsername() + " " + message.getMessage());
			break;
		case ChatMessage.TYPE_CLOSE_CHAT:
		case ChatMessage.TYPE_REOPEN_CHAT:
		case ChatMessage.TYPE_ERROR:
			convertView = LayoutInflater.inflate(mContext, R.layout.simple_list_item_1);
			
			TextView notif = (TextView) convertView.findViewById(android.R.id.text1);
			notif.setText(message.getMessage());
			if (message.getType() == ChatMessage.TYPE_ERROR)
				notif.setTextColor(Color.RED);
			break;
		}
		
		return convertView;
	}

}