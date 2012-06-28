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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;

public class ServerView extends Fragment {

	private EditText input;
	private ExpandableListView list;
	private VentriloidListAdapter adapter;
	private VentriloidService s;
	private SharedPreferences prefs;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		
		list = new ExpandableListView(getActivity());
		list.setGroupIndicator(null);
		list.setBackgroundColor(Color.WHITE);
		list.setCacheColorHint(0);
		list.setOnGroupClickListener(onChannelClick);
		list.setOnChildClickListener(onUserClick);
		
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
			Item.Channel c = s.getItemData().getChannels()
				.get(ExpandableListView.getPackedPositionGroup(packedPosition));
			if (c.id != VentriloInterface.getuserchannel(VentriloInterface.getuserid())) {
				// Do stuff if you don't select your current channel
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.MOVE_TO_CHANNEL, ContextMenu.NONE, "Move to Channel");
			}
			if (c.allowPhantoms && !c.hasPhantom)
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.ADD_PHANTOM, ContextMenu.NONE, "Add Phantom");
			else if (c.allowPhantoms && c.hasPhantom)
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.REMOVE_PHANTOM, ContextMenu.NONE, "Remove Phantom");
			menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.CLEAR_PASSWORD, ContextMenu.NONE, "Clear Saved Password");
		} else if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			// Do stuff for child long click
			Item.User u = s.getItemData().getUsers()
				.get(ExpandableListView.getPackedPositionGroup(packedPosition))
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			
			if (u.parent != VentriloInterface.getuserchannel(VentriloInterface.getuserid())) {
				// Do stuff if you select a user not in your current channel
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.MOVE_TO_CHANNEL, ContextMenu.NONE, "Move to Channel");
			}
			
			if (u.id == VentriloInterface.getuserid()) {
				// Do stuff if you select yourself
				if (s.isAdmin())
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.ADMIN_LOGOUT, ContextMenu.NONE, "Admin Logout");
				else
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.ADMIN_LOGIN, ContextMenu.NONE, "Admin Login");
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.SET_VOLUME, ContextMenu.NONE, "Set Transmit Volume");
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.SET_COMMENT, ContextMenu.NONE, "Set Comment");
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.SET_URL, ContextMenu.NONE, "Set URL");
			} else {
				// Do stuff for other users
				if (u.realId == VentriloInterface.getuserid())
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.REMOVE_PHANTOM, ContextMenu.NONE, "Remove Phantom");
				if (s.isAdmin() || VentriloInterface.getpermission("sendpage"))
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.SEND_PAGE, ContextMenu.NONE, "Send Page");
				// menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.PRIVATE_CHAT, ContextMenu.NONE, "Private Chat");
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.MUTE, ContextMenu.NONE, u.muted ? "Unmute" : "Mute");
				menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.SET_VOLUME, ContextMenu.NONE, "Set Volume");
				if (s.isAdmin() || VentriloInterface.getpermission("moveuser"))
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.MOVE_USER, ContextMenu.NONE, "Move User");
				if (s.isAdmin() || VentriloInterface.getpermission("kickuser"))
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.KICK_USER, ContextMenu.NONE, "Kick User");
				if (s.isAdmin() || VentriloInterface.getpermission("banuser"))
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.BAN_USER, ContextMenu.NONE, "Ban User");
				if (s.isAdmin() && u.realId != 0)
					menu.add(ContextMenu.NONE, ContextMenuItems.ServerContext.GLOBALLY_MUTE, ContextMenu.NONE, "Globally Mute");
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		long packedPosition = ((ExpandableListContextMenuInfo) menuItem.getMenuInfo()).packedPosition;
		int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		final EditText input = new EditText(getActivity());
		Item.Channel c = s.getItemData().getChannels().get(groupPosition);
		Item.User u;
		
		switch (menuItem.getItemId()) {
		case ContextMenuItems.ServerContext.MOVE_TO_CHANNEL:
			changeChannel(s.getItemData().getChannels().get(groupPosition));
			break;
		case ContextMenuItems.ServerContext.CLEAR_PASSWORD:
			break;
		case ContextMenuItems.ServerContext.ADD_PHANTOM:
			VentriloInterface.phantomadd(c.id);
			c.hasPhantom = true;
			break;
		case ContextMenuItems.ServerContext.REMOVE_PHANTOM:
			VentriloInterface.phantomremove(c.id);
			c.hasPhantom = false;
			break;
		case ContextMenuItems.ServerContext.SEND_PAGE:
			u = s.getItemData().getUsers().get(groupPosition)
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			VentriloInterface.sendpage(u.id);
			break;
		case ContextMenuItems.ServerContext.PRIVATE_CHAT:
			// TO-DO
			break;
		case ContextMenuItems.ServerContext.MUTE:
			u = s.getItemData().getUsers().get(groupPosition)
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			VentriloInterface.setuservolume(u.id, u.muted ? u.volume : 0);
			u.muted = !u.muted;
			u.updateStatus();
			getActivity().sendBroadcast(new Intent(ViewPagerActivity.FRAGMENT_RECEIVER));
			break;
		case ContextMenuItems.ServerContext.SET_VOLUME:
			u = s.getItemData().getUsers().get(groupPosition)
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			if (u.id == VentriloInterface.getuserid()) {
				
			} else {
				
			}
			break;
		case ContextMenuItems.ServerContext.ADMIN_LOGIN:
			dialog.setTitle("Enter Admin Password:");
			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
			dialog.setView(input);
			dialog.setPositiveButton("Login", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.adminlogin(input.getText().toString());
				}
			});
			dialog.setNegativeButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) { }
			});
			dialog.show();
			break;
		case ContextMenuItems.ServerContext.ADMIN_LOGOUT:
			VentriloInterface.adminlogout();
			break;
		}
		return super.onContextItemSelected(menuItem);
	}
	
	public void update() {
		adapter.update();
	}
	
	private void changeChannel(final Item.Channel c) {
		if (c.reqPassword) {
			Builder passwordDialog = new AlertDialog.Builder(getActivity());
		    input = new EditText(getActivity());
		    input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
			passwordDialog.setTitle("Enter Channel Password:")
			.setView(input)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.changechannel(c.id, input.getText().toString());
					return;
				}
			})
			.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			passwordDialog.show();
		} else
			VentriloInterface.changechannel(c.id, "");
	}
	
	private OnGroupClickListener onChannelClick = new OnGroupClickListener() {
		public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
			changeChannel(s.getItemData().getChannels().get(groupPosition));
			return true;
		}
	};
	
	private OnChildClickListener onUserClick = new OnChildClickListener() {
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
			final Item.Channel c = s.getItemData().getChannels().get(groupPosition);
			if (c.reqPassword) {
				Builder passwordDialog = new AlertDialog.Builder(getActivity());
			    input = new EditText(getActivity());
			    input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
				passwordDialog.setTitle("Enter Channel Password:")
				.setView(input)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						VentriloInterface.changechannel(c.id, input.getText().toString());
						return;
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				passwordDialog.show();
			} else
				VentriloInterface.changechannel(c.id, "");
			return true;
		}
		
	};
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			
			getActivity().registerReceiver(receiver, new IntentFilter(ViewPagerActivity.FRAGMENT_RECEIVER));
			
			prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

			adapter = new VentriloidListAdapter(
				getActivity(),
				s,
				false,
				s.getItemData().getChannels(),
				R.layout.channel_row,
				new String[] { "indent", "status", "name", "comment" },
				new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
				s.getItemData().getUsers(),
				R.layout.user_row,
				new String[] { "indent", "xmit", "status", "rank", "name", "comment", "integration" },
				new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
		
			list.setAdapter(adapter);
			update();

			for (int i = 0; i < adapter.getGroupCount(); i++) {
				list.expandGroup(i);
			}
			
			registerForContextMenu(list);
		}

		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			short type = intent.getShortExtra("type", (short)0);
			switch (type) {
			case VentriloEvents.V3_EVENT_CHAN_BADPASS:
				final Item.Channel c = s.getItemData().getChannelById(intent.getShortExtra("id", (short)0));
				if (c.reqPassword) {
					Builder passwordDialog = new AlertDialog.Builder(getActivity());
				    input = new EditText(getActivity());
				    input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
				    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
					passwordDialog.setTitle("Error!")
					.setMessage("Incorrect password. Please try again:")
					.setView(input)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							VentriloInterface.changechannel(c.id, input.getText().toString());
							return;
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							return;
						}
					});
					passwordDialog.show();
				} else
					VentriloInterface.changechannel(c.id, "");
				break;
			default:
				update();
			}
		}
	};
}
