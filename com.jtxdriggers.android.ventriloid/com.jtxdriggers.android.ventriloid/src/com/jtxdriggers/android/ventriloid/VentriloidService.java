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

import java.util.concurrent.ConcurrentLinkedQueue;

import org.holoeverywhere.widget.Toast;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

public class VentriloidService extends Service {
	
	static {
		System.loadLibrary("ventrilo_interface");
	}
	
	public static String SERVICE_INTENT = "com.jtxdriggers.android.ventriloid.SERVICE";
	public static String ACTIVITY_RECEIVER = "com.jtxdriggers.android.ventriloid.VentriloidService.ACTIVITY_RECEIVER";
	
	private static boolean connected = false;
	
	private final IBinder BINDER = new MyBinder();
	private final Handler HANDLER = new Handler();
	
	private SharedPreferences prefs, volumePrefs, passwordPrefs;
	private NotificationManager nm;
	private Vibrator vibrator;
	private Server server;
	private ConcurrentLinkedQueue<VentriloEventData> queue;
	private ItemData items;
	
	private VentriloidListAdapter sAdapter, cAdapter;
	
	private Recorder recorder = new Recorder(this);
	private Player player = new Player();
	
	private boolean voiceActivation = false,
		muted = false,
		vibrate = false,
		admin = false,
		running = false,
		disconnect = false;
	private int start,
		reconnectTimer,
		viewType = ViewFragment.VIEW_TYPE_SERVER;
	private double threshold;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int id = intent.getExtras().getInt("id");
		ServerAdapter db = new ServerAdapter(this);
		server = db.getServer(id);

		volumePrefs = getSharedPreferences("VOLUMES" + server.getId(), Context.MODE_PRIVATE);
        passwordPrefs = getSharedPreferences("PASSWORDS" + server.getId(), Context.MODE_PRIVATE);

		new Thread(new Runnable() {
			public void run() {
				if (VentriloInterface.login(server.getHostname() + ":" + server.getPort(),
						server.getUsername(), server.getPassword(), server.getPhonetic())) {
					new Thread(new Runnable() {
						public void run() {
							while (VentriloInterface.recv());
						}
					}).start();
				
					start = Service.START_STICKY;
				} else {
					VentriloEventData data = new VentriloEventData();
					VentriloInterface.error(data);
					sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
						.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL)
						.putExtra("message", bytesToString(data.error.message)));
					
					start = Service.START_NOT_STICKY;
				}
			}
		}).start();
		
		return start;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		registerReceiver(activityReceiver, new IntentFilter(ACTIVITY_RECEIVER));

		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		voiceActivation = prefs.getBoolean("voice_activation", false);
		threshold = voiceActivation ? 55.03125 : -1;
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibrate = prefs.getBoolean("vibrate", true);
		
		queue = new ConcurrentLinkedQueue<VentriloEventData>();
		
		items = new ItemData();
		
		//VentriloInterface.debuglevel(65535);
		running = true;
		new Thread(eventHandler).start();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(activityReceiver);
		player.stop();
		VentriloInterface.logout();
		nm.cancelAll();
		running = false;
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return BINDER;
	}
	
	public void setPTTOn(boolean on) {
		if (on) {
			if (recorder.start()) {
				setXmit(true);
				if (vibrate)
					vibrator.vibrate(25);
			}
		} else {
			recorder.stop();
			setXmit(false);
			if (vibrate)
				vibrator.vibrate(25);
		}
	}

	public class MyBinder extends Binder {
		VentriloidService getService() {
			return VentriloidService.this;
		}
	}

	public static String bytesToString(byte[] bytes) {
		return new String(bytes, 0, (new String(bytes).indexOf(0)));
	}

	private Runnable eventHandler = new Runnable() {
		public void run() {
			
			Item item;
			
			while (running) {
				final VentriloEventData data = new VentriloEventData();
				VentriloInterface.getevent(data);
				
				switch (data.type) {					
				case VentriloEvents.V3_EVENT_USER_LOGIN:
					item = getUserFromData(data);
					if (item.id == VentriloInterface.getuserid())
						VentriloInterface.setxmitvolume(((Item.User) item).volume);
					else
						VentriloInterface.setuservolume(data.user.id, ((Item.User) item).muted ? 0 : ((Item.User) item).volume);
					if ((data.flags & (1 << 0)) == 0 && data.text.real_user_id == 0)
						createNotification(item.name + " has logged in.", data.type, 1);
					break;
					
				case VentriloEvents.V3_EVENT_USER_LOGOUT:
					player.close(data.user.id);
					item = items.getUserById(data.user.id);
					if (((Item.User) item).realId == 0)
						createNotification(item.name + " has logged out.", data.type, 1);
					break;

				case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
					connected = true;
					Intent notifIntent = new Intent(VentriloidService.this, Connected.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					PendingIntent pendingIntent = PendingIntent.getActivity(VentriloidService.this, 0, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);
					NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(VentriloidService.this)
				        .setSmallIcon(R.drawable.headset)
						.setContentIntent(pendingIntent)
				        .setContentText("Connected to " + server.getServername())
			        	.setTicker("Now Connected.")
			        	.setContentTitle("Ventriloid")
			        	.setOngoing(true)
			        	.setAutoCancel(false);
					startForeground(1, notifBuilder.getNotification());
					recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())));
					if (voiceActivation)
						recorder.start(threshold);
					sendBroadcast(new Intent(Main.SERVICE_RECEIVER).putExtra("type", data.type));
					break;

				case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
					if (data.user.id == VentriloInterface.getuserid()) {
						player.clear();
						recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(data.user.id)));
						if (voiceActivation)
							recorder.start(threshold);
					} else {
						item = items.getUserById(data.user.id);
						if (data.channel.id == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
							createNotification(item.name + " joined the channel.", data.type, 1);
						else if (item.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
							createNotification(item.name + " left the channel.", data.type, 1);
						player.close(data.user.id);
					}
					break;

				case VentriloEvents.V3_EVENT_PLAY_AUDIO:
					if (!muted)
						player.write(data.user.id, data.pcm.rate, data.pcm.channels, data.data.sample, data.pcm.length);
					break;

				case VentriloEvents.V3_EVENT_USER_TALK_END:
				case VentriloEvents.V3_EVENT_USER_TALK_MUTE:
				case VentriloEvents.V3_EVENT_USER_GLOBAL_MUTE_CHANGED:
				case VentriloEvents.V3_EVENT_USER_CHANNEL_MUTE_CHANGED:
					player.close(data.user.id);
					break;
					
				case VentriloEvents.V3_EVENT_DISCONNECT:
					connected = false;
					if (!disconnect) {
						item = items.getCurrentChannel().get(0);
						final short id = item.id;
						final String comment = items.getComment();
						final String url = items.getUrl();
						final String integrationText = items.getIntegrationText();
						reconnectTimer = 10;
						items = new ItemData();
						HANDLER.post(new Runnable() {
							@Override
							public void run() {
								if (disconnect)
									return;
								
								if (reconnectTimer > 0) {
									createNotification("Reconnecting in " + reconnectTimer + " seconds", data.type, 1);
									sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
										.putExtra("type", (short) -1)
										.putExtra("timer", reconnectTimer));
									reconnectTimer--;
									HANDLER.postDelayed(this, 1000);
								} else {
									if (VentriloInterface.login(server.getHostname() + ":" + server.getPort(),
											server.getUsername(), server.getPassword(), server.getPhonetic())) {
										new Thread(new Runnable() {
											public void run() {
												while (VentriloInterface.recv());
											}
										}).start();
										VentriloInterface.settext(comment, url, integrationText, true);
										items.setComment(comment);
										items.setUrl(url);
										items.setIntegrationText(integrationText);
										VentriloInterface.changechannel(id, passwordPrefs.getString(id + "pw", ""));
									} else {
										VentriloEventData data = new VentriloEventData();
										VentriloInterface.error(data);
										sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
											.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
										Toast.makeText(getApplicationContext(), bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
										reconnectTimer = 10;
										HANDLER.post(this);
									}
								}
							}
						});
					}
					break;
					
				case VentriloEvents.V3_EVENT_USER_PAGE:
					item = items.getUserById(data.user.id);
					createNotification("Page from " + item.name, data.type, item.id);
					break;
				}
				
				if (queue.size() > 25) {
					try {
						wait(10);
					} catch (InterruptedException e) { }
				}
				
				queue.add(data);
				new Thread(new Runnable() {
					public void run() {
						process(queue.poll());
					}
				}).start();
			}
			player.clear();
			recorder.stop();
		}
	};
	
	public void process(final VentriloEventData data) {
		Item item;
		boolean sendBroadcast = true;
		
		switch (data.type) {
		case VentriloEvents.V3_EVENT_PING:
			items.setPing(data.ping);
			sendBroadcast(new Intent(Connected.SERVICE_RECEIVER).putExtra("type", data.type).putExtra("ping", data.ping));
			sendBroadcast = false;
			break;
			
		case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
		// case VentriloEvents.V3_EVENT_PERMS_UPDATED:
			admin = VentriloInterface.getpermission("serveradmin");
			item = items.getChannels().get(0);
			((Item.Channel) item).changeStatus(admin);
			break;
			
		case VentriloEvents.V3_EVENT_USER_LOGIN:
			item = getUserFromData(data);
			if (item.id != 0) {
				items.addUser((Item.User) item);
				items.addCurrentUser((Item.User) item);
				if (((Item.User) item).realId == VentriloInterface.getuserid())
					items.getChannelById(item.parent).hasPhantom = true;
			} else {
				Item.Channel c = item.new Channel(item.name, item.phonetic, item.comment);
				items.setLobby(c);
			}
			break;
			
		case VentriloEvents.V3_EVENT_USER_LOGOUT:
			item = items.getUserById(data.user.id);
			if (((Item.User) item).realId == VentriloInterface.getuserid())
				items.getChannelById(item.parent).hasPhantom = false;
			items.removeUser(data.user.id);
			items.removeCurrentUser(data.user.id);
			break;

		case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
			item = items.getUserById(data.user.id);
			short from = item.parent;
			item.parent = data.channel.id;
			items.removeUser(data.user.id);
			items.addUser((Item.User) item);
			if (item.id == VentriloInterface.getuserid()) {
				items.setCurrentChannel(item.parent);
			} else {
				if (item.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
					items.addCurrentUser((Item.User) item);
				else if (from == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
					items.removeCurrentUser(item.id);
			}
			break;
			
		case VentriloEvents.V3_EVENT_CHAN_ADD:
			item = getChannelFromData(data);
			items.addChannel((Item.Channel) item);
			break;
			
		case VentriloEvents.V3_EVENT_CHAN_BADPASS:
			Intent i = new Intent(Connected.SERVICE_RECEIVER)
				.putExtra("type", data.type)
				.putExtra("id", data.channel.id);
			sendBroadcast(i);
			sendBroadcast = false;
			break;
			
		case VentriloEvents.V3_EVENT_ERROR_MSG:
			if (data.error.disconnected)
				disconnect();
			HANDLER.post(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(getApplicationContext(), bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
				}
			});
			sendBroadcast = false;
			break;

		case VentriloEvents.V3_EVENT_PLAY_AUDIO:
			if (((Item.User) items.getUserById(data.user.id)).xmit != Item.User.XMIT_ON)
				items.setXmit(data.user.id, Item.User.XMIT_ON);
			else
				sendBroadcast = false;
			break;
			
		case VentriloEvents.V3_EVENT_USER_TALK_START:
			items.setXmit(data.user.id, Item.User.XMIT_INIT);
			break;

		case VentriloEvents.V3_EVENT_USER_TALK_END:
			items.setXmit(data.user.id, Item.User.XMIT_OFF);
			break;
			
		case VentriloEvents.V3_EVENT_USER_TALK_MUTE:
			items.setXmit(data.user.id, Item.User.XMIT_OFF);
			break;
			
		case VentriloEvents.V3_EVENT_USER_GLOBAL_MUTE_CHANGED:
			items.setXmit(data.user.id, Item.User.XMIT_OFF);
			item = items.getUserById(data.user.id);
			((Item.User) item).globalMute = !((Item.User) item).globalMute;
			((Item.User) item).updateStatus();
			break;
			
		case VentriloEvents.V3_EVENT_USER_CHANNEL_MUTE_CHANGED:
			items.setXmit(data.user.id, Item.User.XMIT_OFF);
			item = items.getUserById(data.user.id);
			((Item.User) item).channelMute = !((Item.User) item).channelMute;
			((Item.User) item).updateStatus();
			break;
			
		case VentriloEvents.V3_EVENT_USER_MODIFY:
			Item old = items.getUserById(data.user.id);
			item = getUserFromData(data);
			((Item.User) item).channelMute = ((Item.User) old).channelMute;
			((Item.User) item).globalMute = ((Item.User) old).globalMute;
			((Item.User) item).inChat = ((Item.User) old).inChat;
			((Item.User) item).updateStatus();
			items.removeUser(item.id);
			items.removeCurrentUser(item.id);
			items.addUser((Item.User) item);
			items.addCurrentUser((Item.User) item);
			break;
		}

		if (sendBroadcast) {
			sendBroadcast(new Intent(Connected.SERVICE_RECEIVER).putExtra("type", data.type));
		}
	}
	
	public void setXmit(boolean on) {
		VentriloEventData data = new VentriloEventData();
		data.user.id = VentriloInterface.getuserid();
		if (on)
			data.type = VentriloEvents.V3_EVENT_PLAY_AUDIO;
		else
			data.type = VentriloEvents.V3_EVENT_USER_TALK_END;
		process(data);
	}
	
	public boolean isAdmin() {
		return admin;
	}
	
	public void setAdmin(boolean isAdmin) {
		admin = isAdmin;
		Item.Channel c = items.getChannels().get(0);
		c.changeStatus(admin);
	}
	
	public ItemData getItemData() {
		return items;
	}
	
	public Item.User getUserFromData(VentriloEventData data) {
		VentriloInterface.getuser(data, data.user.id);
		Item item = new Item();
		Item.User u = item.new User(data.user.id,
						VentriloInterface.getuserchannel(data.user.id),
						data.text.real_user_id,
						bytesToString(data.text.name),
						bytesToString(data.text.phonetic),
						bytesToString(data.data.rank.name),
						bytesToString(data.text.comment),
						bytesToString(data.text.url),
						bytesToString(data.text.integration_text),
						data.user.id == VentriloInterface.getuserid() ?
								volumePrefs.getInt("transmit", 79) :
								volumePrefs.getInt("vol" + data.user.id, 79),
						volumePrefs.getBoolean("mute" + data.user.id, false));
		u.updateStatus();
		return u;
	}
	
	public Item.Channel getChannelFromData(VentriloEventData data) {
		VentriloInterface.getchannel(data, data.channel.id);		
		Item item = new Item();
		Item.Channel c = item.new Channel(data.channel.id,
						data.data.channel.parent,
						bytesToString(data.text.name),
						bytesToString(data.text.phonetic),
						bytesToString(data.text.comment),
						data.data.channel.password_protected,
						data.data.channel.is_admin,
						data.data.channel.allow_phantoms,
						data.data.channel.allow_paging);
		return c;
	}
	
	public int getServerId() {
		return server.getId();
	}
	
	public String getServername() {
		return server.getServername();
	}
	
	private void createNotification(String text, short type, int id) {
		Intent notifIntent = new Intent(this, Connected.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(VentriloidService.this)
	        .setSmallIcon(R.drawable.headset)
			.setContentIntent(pendingIntent);
		
		switch (type) {
		case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
		case VentriloEvents.V3_EVENT_USER_LOGIN:
		case VentriloEvents.V3_EVENT_USER_LOGOUT:
		case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
	        notifBuilder.setContentText("Connected to " + server.getServername())
	        	.setTicker(text)
	        	.setContentTitle("Ventriloid")
	        	.setOngoing(true)
	        	.setAutoCancel(false);
			break;
		case VentriloEvents.V3_EVENT_USER_PAGE:
			notifBuilder.setContentText(text)
        		.setTicker(text)
        		.setContentTitle("Page Received")
        		.setAutoCancel(true)
	        	.setOngoing(false)
        		.setDefaults(Notification.DEFAULT_VIBRATE);
			break;
		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_MESSAGE:
			// TODO Make private chat ID's negative so they won't overwrite page notifications
			break;
		case VentriloEvents.V3_EVENT_DISCONNECT:
			Intent disconnectedIntent = new Intent(this, Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			PendingIntent disconnectedPendingIntent = PendingIntent.getActivity(this, 0, disconnectedIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			notifBuilder.setContentIntent(disconnectedPendingIntent)
				.setContentTitle("Disconnected from Server")
				.setContentText(text)
				.setOngoing(true)
				.setAutoCancel(false);
			if (text.contains(10 + ""))
				notifBuilder.setTicker("Disconnected from Server");
			break;
		}
		nm.notify(id, notifBuilder.getNotification());
	}

	public VentriloidListAdapter getServerAdapter() {
		return sAdapter;
	}

	public VentriloidListAdapter getChannelAdapter() {
		return cAdapter;
	}
	
	public void notifyDataSetChanged() {
		sAdapter.notifyDataSetChanged();
		cAdapter.notifyDataSetChanged();
	}
	
	public void disconnect() {
		disconnect = true;
		connected = false;
		stopForeground(true);
		stopSelf();
	}
	
	public static boolean isConnected() {
		return connected;
	}
	
	public void setViewType(int viewType) {
		this.viewType = viewType;
	}
	
	public int getViewType() {
		return viewType;
	}
	
	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	private BroadcastReceiver activityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getIntExtra("type", -1)) {
			case VentriloEvents.V3_EVENT_DISCONNECT:
				disconnect();
				break;
			}
		}
	};
}
