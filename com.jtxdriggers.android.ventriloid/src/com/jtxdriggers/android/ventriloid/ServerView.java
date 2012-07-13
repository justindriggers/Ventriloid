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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class ServerView extends Fragment {

	private EditText input;
	private ExpandableListView list;
	private VentriloidListAdapter adapter;
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
		// Define this as a class variable to work around the bug with SubMenus
		packedPosition = ((ExpandableListContextMenuInfo) menuInfo).packedPosition;
		int packedPositionType = ExpandableListView.getPackedPositionType(packedPosition);
		
		Item.Channel c = s.getItemData().getChannels()
			.get(ExpandableListView.getPackedPositionGroup(packedPosition));
		
		if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_GROUP) {
			// Do stuff for group long click
			if (c.id != VentriloInterface.getuserchannel(VentriloInterface.getuserid())) {
				// Do stuff if you don't select your current channel
				menu.add(ContextMenu.NONE, ContextMenuItems.MOVE_TO_CHANNEL, ContextMenu.NONE, "Move to Channel");
			}
			if (c.allowPhantoms && !c.hasPhantom)
				menu.add(ContextMenu.NONE, ContextMenuItems.ADD_PHANTOM, ContextMenu.NONE, "Add Phantom");
			else if (c.allowPhantoms && c.hasPhantom)
				menu.add(ContextMenu.NONE, ContextMenuItems.REMOVE_PHANTOM, ContextMenu.NONE, "Remove Phantom");
			if (passwordPrefs.getString(c.id + "pw", "").length() > 0)
				menu.add(ContextMenu.NONE, ContextMenuItems.CLEAR_PASSWORD, ContextMenu.NONE, "Clear Saved Password");
		} else if (packedPositionType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
			// Do stuff for child long click
			Item.User u = s.getItemData().getUsers()
				.get(ExpandableListView.getPackedPositionGroup(packedPosition))
				.get(ExpandableListView.getPackedPositionChild(packedPosition));
			
			if (u.parent != VentriloInterface.getuserchannel(VentriloInterface.getuserid())) {
				// Do stuff if you select a user not in your current channel
				menu.add(ContextMenu.NONE, ContextMenuItems.MOVE_TO_CHANNEL, ContextMenu.NONE, "Move to Channel");
			}
			
			if (u.id == VentriloInterface.getuserid()) {
				// Do stuff if you select yourself
				if (s.isAdmin())
					menu.add(ContextMenu.NONE, ContextMenuItems.ADMIN_LOGOUT, ContextMenu.NONE, "Admin Logout");
				else
					menu.add(ContextMenu.NONE, ContextMenuItems.ADMIN_LOGIN, ContextMenu.NONE, "Admin Login");
				menu.add(ContextMenu.NONE, ContextMenuItems.SET_VOLUME, ContextMenu.NONE, "Set Transmit Volume");
				menu.add(ContextMenu.NONE, ContextMenuItems.SET_COMMENT, ContextMenu.NONE, "Set Comment");
				menu.add(ContextMenu.NONE, ContextMenuItems.SET_URL, ContextMenu.NONE, "Set URL");
			} else {
				// Do stuff for other users
				if (u.realId == VentriloInterface.getuserid())
					menu.add(ContextMenu.NONE, ContextMenuItems.REMOVE_PHANTOM, ContextMenu.NONE, "Remove Phantom");
				if (u.comment.length() > 0)
					menu.add(ContextMenu.NONE, ContextMenuItems.VIEW_COMMENT, ContextMenu.NONE, "View Comment");
				if (u.url.length() > 0)
					menu.add(ContextMenu.NONE, ContextMenuItems.VIEW_URL, ContextMenu.NONE, "View URL");
				if (s.isAdmin() || VentriloInterface.getpermission("sendpage"))
					menu.add(ContextMenu.NONE, ContextMenuItems.SEND_PAGE, ContextMenu.NONE, "Send Page");
				// menu.add(ContextMenu.NONE, ContextMenuItems.PRIVATE_CHAT, ContextMenu.NONE, "Private Chat");
				menu.add(ContextMenu.NONE, ContextMenuItems.MUTE, ContextMenu.NONE, u.muted ? "Unmute" : "Mute");
				menu.add(ContextMenu.NONE, ContextMenuItems.SET_VOLUME, ContextMenu.NONE, "Set Volume");
				
				boolean kick = false,
						ban = false;
				
				if (s.isAdmin() || (kick = VentriloInterface.getpermission("kickuser")) ||
						(ban = VentriloInterface.getpermission("banuser"))) {
					SubMenu adminMenu = menu.addSubMenu(ContextMenu.NONE, ContextMenuItems.SERVER_ADMIN, ContextMenu.NONE, "Server Admin Functions");
					if (s.isAdmin() || kick)
						adminMenu.add(ContextMenu.NONE, ContextMenuItems.KICK_USER, ContextMenu.NONE, "Kick User");
					if (s.isAdmin() || ban)
						adminMenu.add(ContextMenu.NONE, ContextMenuItems.BAN_USER, ContextMenu.NONE, "Ban User");
					if (s.isAdmin() && u.realId == 0)
						adminMenu.add(ContextMenu.NONE, ContextMenuItems.GLOBALLY_MUTE, ContextMenu.NONE, u.globalMute ? "Globally Unmute" : "Globally Mute");
				}
				
				if (s.isAdmin() || c.isAdmin) {
					SubMenu cAdminMenu = menu.addSubMenu(ContextMenu.NONE, ContextMenuItems.CHANNEL_ADMIN, ContextMenu.NONE, "Channel Admin Functions");
					cAdminMenu.add(ContextMenu.NONE, ContextMenuItems.CHANNEL_KICK, ContextMenu.NONE, "Kick from Channel");
					cAdminMenu.add(ContextMenu.NONE, ContextMenuItems.CHANNEL_BAN, ContextMenu.NONE, "Ban from Channel");
					cAdminMenu.add(ContextMenu.NONE, ContextMenuItems.CHANNEL_MUTE, ContextMenu.NONE, u.channelMute ? "Channel Unmute" : "Channel Mute");
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
						moveMenu.add(ContextMenu.NONE, ContextMenuItems.MOVE_USER_TO + adminChannels.get(i).id, i, name);
					}
				}
			}
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem menuItem) {
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
		
		Item.Channel c = s.getItemData().getChannels().get(ExpandableListView.getPackedPositionGroup(packedPosition));
		final Item.User u = (ExpandableListView.getPackedPositionType(packedPosition) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) ?
				s.getItemData().getUsers().get(ExpandableListView.getPackedPositionGroup(packedPosition))
				.get(ExpandableListView.getPackedPositionChild(packedPosition)) : null;
		
		switch (menuItem.getItemId()) {
		case ContextMenuItems.MOVE_TO_CHANNEL:
			changeChannel(c);
			return true;
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
			getActivity().sendBroadcast(new Intent(ViewPagerActivity.FRAGMENT_RECEIVER));
			return true;
		case ContextMenuItems.SET_VOLUME:
			final TextView percent = new TextView(getActivity());
			final SeekBar volume = new SeekBar(getActivity());
			volume.setMax(148);
			volume.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (progress >= 67 && progress <= 81) {
						seekBar.setProgress(74);
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
						getActivity().sendBroadcast(new Intent(ViewPagerActivity.FRAGMENT_RECEIVER));
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
						getActivity().sendBroadcast(new Intent(ViewPagerActivity.FRAGMENT_RECEIVER));
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
			return false;
		}
	}
	
	public void update() {
		adapter.update();
	}
	
	private void changeChannel(final Item.Channel c) {
		String password = passwordPrefs.getString(c.id + "pw", "");
		if (c.reqPassword) {
			if (password.length() > 0) {
				VentriloInterface.changechannel(c.id, password);
			} else {
				Builder passwordDialog = new AlertDialog.Builder(getActivity());
			    input = new EditText(getActivity());
			    input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
				passwordDialog.setTitle("Enter Channel Password:")
				.setView(input)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						VentriloInterface.changechannel(c.id, input.getText().toString());
						passwordPrefs.edit().putString(c.id + "pw", input.getText().toString()).commit();
						return;
					}
				})
				.setNegativeButton("Cancel", null);
				passwordDialog.show();
			}
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
			changeChannel(s.getItemData().getChannels().get(groupPosition));
			return true;
		}
	};
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			
			getActivity().registerReceiver(receiver, new IntentFilter(ViewPagerActivity.FRAGMENT_RECEIVER));
			
			volumePrefs = getActivity().getSharedPreferences("VOLUMES" + s.getServerId(), Context.MODE_PRIVATE);
			passwordPrefs = getActivity().getSharedPreferences("PASSWORDS" + s.getServerId(), Context.MODE_PRIVATE);

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
				passwordPrefs.edit().remove(c.id + "pw").commit();
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
							passwordPrefs.edit().putString(c.id + "pw", input.getText().toString()).commit();
							return;
						}
					})
					.setNegativeButton("Cancel", null);
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
