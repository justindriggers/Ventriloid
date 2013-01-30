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

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupCollapseListener;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class ChannelView extends Fragment {

	private EditText input;
	private ExpandableListView list;
	private VentriloidService s;
	private SharedPreferences volumePrefs, passwordPrefs;
	
	// Stupid workaround for broken ExpanableListView SubMenus
	private long packedPosition;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		
		list = new ExpandableListView(getActivity());
		list.setGroupIndicator(null);
		list.setBackgroundColor(Color.WHITE);
		list.setCacheColorHint(0);
		
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
		getActivity().unbindService(mConnection);
		super.onStop();
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		// Define this as a class variable to work around the bug with SubMenus
		packedPosition = ((ExpandableListContextMenuInfo) menuInfo).packedPosition;
		int packedPositionType = ExpandableListView.getPackedPositionType(packedPosition);
		
		Item.Channel c = s.getItemData().getCurrentChannel().get(0);
	
		if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			// Do stuff for group long click
			if (c.allowPhantoms && !c.hasPhantom)
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.ADD_PHANTOM, ContextMenu.NONE, "Add Phantom");
			else if (c.allowPhantoms && c.hasPhantom)
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.REMOVE_PHANTOM, ContextMenu.NONE, "Remove Phantom");
			if (passwordPrefs.getString(c.id + "pw", "").length() > 0)
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.CLEAR_PASSWORD, ContextMenu.NONE, "Clear Saved Password");
		} else if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			// Do stuff for child long click
			Item.User u = s.getItemData().getCurrentUsers().get(0)
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			
			if (u.id == VentriloInterface.getuserid()) {
				// Do stuff if you select yourself
				if (s.isAdmin())
					menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.ADMIN_LOGOUT, ContextMenu.NONE, "Admin Logout");
				else
					menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.ADMIN_LOGIN, ContextMenu.NONE, "Admin Login");
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.SET_VOLUME, ContextMenu.NONE, "Set Transmit Volume");
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.SET_COMMENT, ContextMenu.NONE, "Set Comment");
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.SET_URL, ContextMenu.NONE, "Set URL");
			} else {
				// Do stuff for other users
				if (u.realId == VentriloInterface.getuserid())
					menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.REMOVE_PHANTOM, ContextMenu.NONE, "Remove Phantom");
				if (u.comment.length() > 0)
					menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.VIEW_COMMENT, ContextMenu.NONE, "View Comment");
				if (u.url.length() > 0)
					menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.VIEW_URL, ContextMenu.NONE, "View URL");
				if (s.isAdmin() || VentriloInterface.getpermission("sendpage"))
					menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.SEND_PAGE, ContextMenu.NONE, "Send Page");
				// menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.PRIVATE_CHAT, ContextMenu.NONE, "Private Chat");
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.MUTE, ContextMenu.NONE, u.muted ? "Unmute" : "Mute");
				menu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.SET_VOLUME, ContextMenu.NONE, "Set Volume");
				
				boolean kick = false,
						ban = false;
				
				if (s.isAdmin() || (kick = VentriloInterface.getpermission("kickuser")) ||
						(ban = VentriloInterface.getpermission("banuser"))) {
					SubMenu adminMenu = menu.addSubMenu(ContextMenu.NONE, ContextMenuItems.SERVER_ADMIN, ContextMenu.NONE, "Server Admin Functions");
					if (s.isAdmin() || kick)
						adminMenu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.KICK_USER, ContextMenu.NONE, "Kick User");
					if (s.isAdmin() || ban)
						adminMenu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.BAN_USER, ContextMenu.NONE, "Ban User");
					if (s.isAdmin() && u.realId == 0)
						adminMenu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.GLOBALLY_MUTE, ContextMenu.NONE, u.globalMute ? "Globally Unmute" : "Globally Mute");
				}
				
				if (s.isAdmin() || c.isAdmin) {
					SubMenu cAdminMenu = menu.addSubMenu(ContextMenu.NONE, ContextMenuItems.CHANNEL_ADMIN, ContextMenu.NONE, "Channel Admin Functions");
					cAdminMenu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.CHANNEL_KICK, ContextMenu.NONE, "Kick from Channel");
					cAdminMenu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.CHANNEL_BAN, ContextMenu.NONE, "Ban from Channel");
					cAdminMenu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.CHANNEL_MUTE, ContextMenu.NONE, u.channelMute ? "Channel Unmute" : "Channel Mute");
				}
				
				ArrayList<Item.Channel> adminChannels = new ArrayList<Item.Channel>();
				for (int i = 0; i < s.getItemData().getChannels().size(); i++) {
					Item.Channel adminChannel = s.getItemData().getChannels().get(i);
					if ((s.isAdmin() || adminChannel.isAdmin) && u.parent != adminChannel.id)
						adminChannels.add(adminChannel);
				}
				if ((s.isAdmin() || VentriloInterface.getpermission("moveuser") || c.isAdmin) && adminChannels.size() > 0) {
					SubMenu moveMenu = menu.addSubMenu(ContextMenu.NONE, ContextMenuItems.MOVE_USER, ContextMenu.NONE, "Move User To");
					for (int i = 0; i < adminChannels.size(); i++) {
						String name = adminChannels.get(i).name;
						if (adminChannels.get(i).id == 0)
							name = "(Lobby)";
						else if (adminChannels.get(i).parent != 0) {
							Item.Channel parent = s.getItemData().getChannelById(adminChannels.get(i).parent);
							while (parent.id != 0) {
								name = parent.name + " / " + name;
								parent = s.getItemData().getChannelById(parent.parent);
							}
						}
						moveMenu.add(ContextMenuItems.CHANNEL_VIEW, ContextMenuItems.MOVE_USER_TO + adminChannels.get(i).id, i, name);
					}
				}
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
		if (menuItem.getGroupId() != ContextMenuItems.CHANNEL_VIEW)
			return super.onContextItemSelected(menuItem);
		
		// Instead of declaring this here, just use the class variable.
		// Attempting to declare this here will cause a NullPointerException from the SubMenu admin functions.
		// Since the SubMenu items are built from the primary menu, they don't contain the packedPosition variable
		// that the primary items have (because they were built from the ExpandableListView's registered ContextMenu).
		// long packedPosition = ((ExpandableListContextMenuInfo) menuItem.getMenuInfo()).packedPosition;
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		
		LinearLayout layout = new LinearLayout(getActivity());
		layout.setOrientation(LinearLayout.VERTICAL);
		
		input = new EditText(getActivity());
	    InputFilter[] FilterArray = new InputFilter[1];
	    FilterArray[0] = new InputFilter.LengthFilter(127);
	    input.setFilters(FilterArray);
		layout.addView(input);
		
		final CheckBox silent = new CheckBox(getActivity());
		silent.setChecked(true);
		silent.setText(" Send Silently ");
		
		LinearLayout frame = new LinearLayout(getActivity());
		frame.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		frame.setGravity(Gravity.CENTER);
		
		Item.Channel c = s.getItemData().getCurrentChannel().get(0);
		final Item.User u = (ExpandableListView.getPackedPositionType(packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) ?
				s.getItemData().getCurrentUsers().get(0).get(ExpandableListView.getPackedPositionChild(packedPosition)) : null;
		
		switch (menuItem.getItemId()) {
		case ContextMenuItems.CLEAR_PASSWORD:
			passwordPrefs.edit().remove(c.id + "pw").commit();
			return true;
		case ContextMenuItems.ADD_PHANTOM:
			VentriloInterface.phantomadd(c.id);
			return true;
		case ContextMenuItems.REMOVE_PHANTOM:
			VentriloInterface.phantomremove(c.id);
			return true;
		case ContextMenuItems.SEND_PAGE:
			VentriloInterface.sendpage(u.id);
			return true;
		case ContextMenuItems.PRIVATE_CHAT:
			// TODO Private Chat
			return true;
		case ContextMenuItems.MUTE:
			u.muted = !u.muted;
			VentriloInterface.setuservolume(u.id, u.muted ? 0 : u.volume);
			volumePrefs.edit().putBoolean("mute" + u.id, u.muted).commit();
			u.updateStatus();
			s.updateViews();
			return true;
		case ContextMenuItems.SET_VOLUME:
			final TextView percent = new TextView(getActivity());
			final SeekBar volume = new SeekBar(getActivity());
			volume.setMax(158);
			volume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (progress >= 72 && progress <= 86 && progress != 79) {
						seekBar.setProgress(79);
						percent.setText("100%");
					} else
						percent.setText((progress * 200) / seekBar.getMax() + "%");
				}
				public void onStartTrackingTouch(SeekBar seekBar) { }
				public void onStopTrackingTouch(SeekBar seekBar) { }
			});
			LinearLayout volumeLayout = new LinearLayout(getActivity());
			volumeLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, Gravity.CENTER));
			volumeLayout.setOrientation(LinearLayout.VERTICAL);
			volumeLayout.addView(volume);
			frame.addView(percent);
			volumeLayout.addView(frame);
			dialog.setView(volumeLayout);
			volume.setProgress(u.volume);
			percent.setText((u.volume * 200) / volume.getMax() + "%");
			if (u.id == VentriloInterface.getuserid()) {
				dialog.setTitle("Set Transmit Volume:");
				dialog.setPositiveButton("OK", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						VentriloInterface.setxmitvolume(volume.getProgress());
						u.volume = volume.getProgress();
						u.updateStatus();
						s.updateViews();
						volumePrefs.edit().putInt("transmit", volume.getProgress()).commit();
					}
				});
			} else {
				dialog.setTitle("Set Volume for " + u.name);
				dialog.setPositiveButton("OK", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (!u.muted)
							VentriloInterface.setuservolume(u.id, volume.getProgress());
						u.volume = volume.getProgress();
						u.updateStatus();
						s.updateViews();
						volumePrefs.edit().putInt("vol" + u.id, volume.getProgress()).commit();
					}
				});
			}
			dialog.setNegativeButton("Cancel", null);
			dialog.show();
			return true;
		case ContextMenuItems.SET_COMMENT:
			dialog.setTitle("Set Comment:");
			frame.addView(silent);
			layout.addView(frame);
			dialog.setView(layout);
			input.setSingleLine();
			input.setText(u.comment);
			dialog.setPositiveButton("OK", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.settext(input.getText().toString(), u.url, "", silent.isChecked());
				}
			});
			dialog.setNegativeButton("Cancel", null);
			input.setText(u.comment);
			dialog.show();
			return true;
		case ContextMenuItems.VIEW_COMMENT:
			dialog.setTitle(u.name);
			dialog.setView(layout);
			input.setSingleLine();
			input.setText(u.comment);
			dialog.setPositiveButton("Copy", new OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE); 
					clipboard.setText(input.getText().toString());
				}
			});
			dialog.setNegativeButton("Done", null);
			dialog.show();
			return true;
		case ContextMenuItems.SET_URL:
			dialog.setTitle("Set URL:");
			frame.addView(silent);
			layout.addView(frame);
			dialog.setView(layout);
			input.setText(u.url.length() > 0 ? u.url : "http://");
			input.setSingleLine();
			dialog.setPositiveButton("OK", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.settext(u.comment, input.getText().toString(), "", silent.isChecked());
				}
			});
			dialog.setNegativeButton("Cancel", null);
			dialog.show();
			return true;
		case ContextMenuItems.VIEW_URL:
			dialog.setTitle(u.name);
			dialog.setView(layout);
			input.setText(u.url);
			input.setSingleLine();
			dialog.setPositiveButton("Copy", new OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {
					ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE); 
					clipboard.setText(input.getText().toString());
				}
			});
			dialog.setNeutralButton("Open", new OnClickListener(){
				public void onClick(DialogInterface dialog, int which) {	
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(input.getText().toString())));
	            }
			});
			dialog.setNegativeButton("Done", null);
			dialog.show();
			return true;
		case ContextMenuItems.ADMIN_LOGIN:
			dialog.setTitle("Enter Admin Password:");
			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
			dialog.setView(layout);
			dialog.setPositiveButton("Login", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.adminlogin(input.getText().toString());
					s.setAdmin(true);
				}
			});
			dialog.setNegativeButton("Cancel", null);
			dialog.show();
			return true;
		case ContextMenuItems.ADMIN_LOGOUT:
			VentriloInterface.adminlogout();
			s.setAdmin(false);
			return true;
		case ContextMenuItems.KICK_USER:
			dialog.setTitle("Enter Reason for Kick:");
			dialog.setView(layout);
			dialog.setPositiveButton("Kick", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.kick(u.id, input.getText().toString());
				}
			});
			dialog.setNegativeButton("Cancel", null);
			dialog.show();
			return true;
		case ContextMenuItems.BAN_USER:
			dialog.setTitle("Enter Reason for Ban:");
			dialog.setView(layout);
			dialog.setPositiveButton("Ban", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.ban(u.id, input.getText().toString());
				}
			});
			dialog.setNegativeButton("Cancel", null);
			dialog.show();
			return true;
		case ContextMenuItems.GLOBALLY_MUTE:
			VentriloInterface.globalmute(u.id);
			return true;
		case ContextMenuItems.CHANNEL_KICK:
			VentriloInterface.channelkick(u.id);
			return true;
		case ContextMenuItems.CHANNEL_BAN:
			dialog.setTitle("Enter Reason for Ban:");
			dialog.setView(layout);
			dialog.setPositiveButton("Ban", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.channelban(u.id, input.getText().toString());
				}
			});
			dialog.setNegativeButton("Cancel", null);
			dialog.show();
			return true;
		case ContextMenuItems.CHANNEL_MUTE:
			VentriloInterface.channelmute(u.id);
			return true;
		case ContextMenuItems.SERVER_ADMIN:
		case ContextMenuItems.CHANNEL_ADMIN:
			return true;
		default:
			if (menuItem.getItemId() >= ContextMenuItems.MOVE_USER_TO) { 
				short channelid = (short) (menuItem.getItemId() - ContextMenuItems.MOVE_USER_TO);
				VentriloInterface.forcechannelmove(u.id, channelid);
				return true;
			}
			return super.onContextItemSelected(menuItem);
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			
			volumePrefs = getActivity().getSharedPreferences("VOLUMES" + s.getServerId(), Context.MODE_PRIVATE);
			passwordPrefs = getActivity().getSharedPreferences("PASSWORDS" + s.getServerId(), Context.MODE_PRIVATE);
		
			list.setAdapter(s.getChannelAdapter());
			
			list.expandGroup(0);

			list.setOnGroupExpandListener(new OnGroupExpandListener() {
				public void onGroupExpand(int groupPosition) { }
			});
			list.setOnGroupCollapseListener(new OnGroupCollapseListener() {
				public void onGroupCollapse(int groupPosition) {
					list.expandGroup(groupPosition);
				}
			});
			
			registerForContextMenu(list);
		}

		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};
}
