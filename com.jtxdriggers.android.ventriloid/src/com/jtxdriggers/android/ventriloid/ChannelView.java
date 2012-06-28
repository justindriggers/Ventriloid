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

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnGroupClickListener;

public class ChannelView extends Fragment {

	private ExpandableListView list;
	private VentriloidListAdapter adapter;
	private VentriloidService s;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		
		list = new ExpandableListView(getActivity());
		list.setGroupIndicator(null);
		list.setBackgroundColor(Color.WHITE);
		list.setCacheColorHint(0);
		list.setOnGroupClickListener(new OnGroupClickListener() {
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				return true;
			}
		});
		
		return list;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		getActivity().bindService(new Intent(VentriloidService.SERVICE_INTENT), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
		getActivity().unregisterReceiver(receiver);
		getActivity().unbindService(mConnection);
		super.onStop();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		long packedPosition = ((ExpandableListContextMenuInfo) menuInfo).packedPosition;
		int packedPositionType = ExpandableListView.getPackedPositionType(packedPosition);
		
		if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			// Do stuff for group long click
			menu.add(ContextMenu.NONE, ContextMenuItems.ChannelContext.CLEAR_PASSWORD, ContextMenu.NONE, "Clear Saved Password");
		} else if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			// Do stuff for child long click
			Item.User u = s.getItemData().getCurrentUsers().get(0)
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			if (u.id == VentriloInterface.getuserid()) {
				// Do stuff if you select yourself
			} else {
				// Do stuff for other users
				menu.add(ContextMenu.NONE, ContextMenuItems.ChannelContext.SET_VOLUME, ContextMenu.NONE, "Set Volume");
				menu.add(ContextMenu.NONE, ContextMenuItems.ChannelContext.MUTE, ContextMenu.NONE, u.muted ? "Unmute" : "Mute");
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		long packedPosition = ((ExpandableListContextMenuInfo) menuItem.getMenuInfo()).packedPosition;
		
		switch (menuItem.getItemId()) {
		case ContextMenuItems.ChannelContext.CLEAR_PASSWORD:
			break;
		case ContextMenuItems.ChannelContext.MUTE:
			Item.User u = s.getItemData().getCurrentUsers().get(0)
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			u.muted = !u.muted;
			VentriloInterface.setuservolume(u.id, u.muted ? 0 : u.volume);
			u.updateStatus();
			getActivity().sendBroadcast(new Intent(ViewPagerActivity.FRAGMENT_RECEIVER));
			break;
		}
		return super.onContextItemSelected(menuItem);
	}
	
	public void update() {
		adapter.update();

		for (int i = 0; i < adapter.getGroupCount(); i++) {
			list.expandGroup(i);
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			
			getActivity().registerReceiver(receiver, new IntentFilter(ViewPagerActivity.FRAGMENT_RECEIVER));

			adapter = new VentriloidListAdapter(
				getActivity(),
				s,
				true,
				s.getItemData().getCurrentChannel(),
				R.layout.channel_row,
				new String[] { "indent", "status", "name", "comment" },
				new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
				s.getItemData().getCurrentUsers(),
				R.layout.user_row,
				new String[] { "indent", "xmit", "status", "rank", "name", "comment", "integration" },
				new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
		
			list.setAdapter(adapter);
			update();
			
			registerForContextMenu(list);
		}

		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			update();
		}
	};
}
