package com.jtxdriggers.android.ventriloid;

import java.util.ArrayList;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.TextView;

import android.content.Context;
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
		
		if (convertView == null)
			convertView = LayoutInflater.inflate(mContext, R.layout.simple_list_item_1);
		
		TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
		textView.setText(message.getUsername() + message.getMessage());
		
		return convertView;
	}

}
