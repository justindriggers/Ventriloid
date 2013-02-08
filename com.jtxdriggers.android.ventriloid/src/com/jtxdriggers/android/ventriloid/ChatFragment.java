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

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class ChatFragment extends Fragment {
	
	public static final String SERVICE_RECEIVER = "com.jtxdriggers.android.ventriloid.ChatFragment.SERVICE_RECEIVER";
	
	private VentriloidService s;
	private ListView list;
	private ChatListAdapter adapter;
	private EditText message;
	private ImageButton send;
	private TextView title;
	
	private short id;
	private String name;
	
	public static ChatFragment newInstance(short id) {
		ChatFragment fragment = new ChatFragment();

		Bundle args = new Bundle();
		args.putShort("viewId", id);
		fragment.setArguments(args);
		
		return fragment;
	}
	
	public static ChatFragment newInstance(short id, String name) {
		ChatFragment fragment = new ChatFragment();

		Bundle args = new Bundle();
		args.putShort("viewId", id);
		args.putString("viewName", name);
		fragment.setArguments(args);
		
		return fragment;
	}
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	id = getArguments().getShort("viewId", (short) -1);
    	name = getArguments().getString("viewName");
    	
    	RelativeLayout layout = (RelativeLayout) LayoutInflater.inflate(getActivity(), R.layout.chat_fragment);
    	
    	title = (TextView) layout.findViewById(R.id.title);
		title.setText(id == 0 ? "Server Chat" : "Private Chat - " + name);
		
		ImageButton close = (ImageButton) layout.findViewById(R.id.close);
		close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				VentriloInterface.endprivatechat(id);
				getActivity().sendBroadcast(new Intent(Connected.FRAGMENT_RECEIVER)
					.putExtra("type", VentriloEvents.V3_EVENT_PRIVATE_CHAT_END)
					.putExtra("id", id));
			}
		});
		close.setVisibility(id == 0 ? View.INVISIBLE : View.VISIBLE);
    	
    	list = (ListView) layout.findViewById(android.R.id.list);
    	list.setDivider(getResources().getDrawable(R.drawable.abs__list_divider_holo_light));
    	
    	message = (EditText) layout.findViewById(R.id.message);
    	message.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (message.getText().toString().length() > 0 && (actionId == 4 || event != null)) {
					if (adapter.getCount() > 0) {
						if (adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_DISCONNECT || adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_CLOSE_CHAT)
							return false;
					}
					
					if (id == 0)
						VentriloInterface.sendchatmessage(message.getText().toString());
					else
						VentriloInterface.sendprivatemessage(id, message.getText().toString());
					message.setText("");
					return true;
				}
				return false;
			}
    	});
    	message.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
			
			@Override
			public void afterTextChanged(Editable s) {
				if (s == null || s.length() == 0)
					send.setEnabled(false);
				else if (adapter.getCount() > 0) {
					if (adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_DISCONNECT || adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_CLOSE_CHAT)
						send.setEnabled(false);
					else send.setEnabled(true);
				} else
					send.setEnabled(true);
			}
    	});
    	send = (ImageButton) layout.findViewById(R.id.send);
    	send.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (id == 0)
					VentriloInterface.sendchatmessage(message.getText().toString());
				else
					VentriloInterface.sendprivatemessage(id, message.getText().toString());
				message.setText("");
			}
    	});
        send.setEnabled(false);
		
		if (getDefaultSharedPreferences().getBoolean("screen_on", false))
			list.setKeepScreenOn(true);
    	
    	((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(-id);
		
        return layout;
    }
	
	@Override
	public void onStart() {
		super.onStart();
		getActivity().bindService(new Intent(VentriloidService.SERVICE_INTENT), serviceConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
		s.setNotify(id, true);
		getActivity().unregisterReceiver(serviceReceiver);
		getActivity().unbindService(serviceConnection);
		super.onStop();
	}
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			
			getActivity().registerReceiver(serviceReceiver, new IntentFilter(SERVICE_RECEIVER));

	    	adapter = new ChatListAdapter(getActivity(), s.getItemData().getChat(id));
	    	list.setAdapter(adapter);
			if (message.getText().toString().length() == 0)
				send.setEnabled(false);
			else if (adapter.getCount() > 0) {
				if (adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_DISCONNECT || adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_CLOSE_CHAT)
					send.setEnabled(false);
				else send.setEnabled(true);
			} else
				send.setEnabled(true);
			
			s.setNotify(id, false);
		}

		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};
	
	private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			adapter.notifyDataSetChanged();
	    	((NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(-id);
	    	
			if (message.getText().toString().length() == 0)
				send.setEnabled(false);
			else if (adapter.getCount() > 0) {
				if (adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_DISCONNECT || adapter.getItem(adapter.getCount() - 1).getType() == ChatMessage.TYPE_CLOSE_CHAT)
					send.setEnabled(false);
				else send.setEnabled(true);
			} else
				send.setEnabled(true);
		}
	};
}