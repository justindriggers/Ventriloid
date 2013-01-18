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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class VentriloidService extends Service {
	
	static {
		System.loadLibrary("ventrilo_interface");
	}
	
	public static String SERVICE_INTENT = "com.jtxdriggers.android.ventriloid.SERVICE";
	
	private final IBinder mBinder = new MyBinder();
	private WindowManager wm;
	private NotificationManager nm;
	private Intent notifIntent;
	private PendingIntent pendingIntent;
	private Vibrator vibrator;
	private SharedPreferences prefs, volumePrefs;
	private Server server;
	private boolean running = false; //, processed = false;
	private ItemData items = new ItemData();
	private VentriloidListAdapter sAdapter, cAdapter;
	private Recorder recorder = new Recorder(this);
	private Player player = new Player();
	private ConcurrentLinkedQueue<VentriloEventData> queue;
	private Button ptt;
	private boolean voiceActivation = false,
		vibrate = false,
		admin = false;
	private int start;
	private double threshold = -1;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int id = intent.getExtras().getInt("id");
		ServerAdapter db = new ServerAdapter(this);
		server = db.getServer(id);

		volumePrefs = getSharedPreferences("VOLUMES" + server.getId(), Context.MODE_PRIVATE);

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
					sendBroadcast(new Intent(Main.RECEIVER)
						.putExtra("type", VentriloEvents.V3_EVENT_LOGIN_FAIL)
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
		sAdapter = new VentriloidListAdapter(
				getApplicationContext(),
				this,
				false,
				getItemData().getChannels(),
				R.layout.channel_row,
				new String[] { "indent", "status", "name", "comment" },
				new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
				getItemData().getUsers(),
				R.layout.user_row,
				new String[] { "indent", "xmit", "status", "rank", "name", "comment", "integration" },
				new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
		
		cAdapter = new VentriloidListAdapter(
				getApplicationContext(),
				this,
				true,
				getItemData().getChannels(),
				R.layout.channel_row,
				new String[] { "indent", "status", "name", "comment" },
				new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
				getItemData().getUsers(),
				R.layout.user_row,
				new String[] { "indent", "xmit", "status", "rank", "name", "comment", "integration" },
				new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		voiceActivation = prefs.getBoolean("voice_activation", false);
		if (voiceActivation)
			threshold = 55.03125;
		
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notifIntent = new Intent(this, ViewPagerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		pendingIntent = PendingIntent.getActivity(VentriloidService.this, 0, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);
		
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (prefs.getBoolean("vibrate", true));
			vibrate = true;
		queue = new ConcurrentLinkedQueue<VentriloEventData>();
		//VentriloInterface.debuglevel(65535);
		running = true;
		new Thread(eventHandler).start();
	}

	@Override
	public void onDestroy() {
		player.stop();
        if(ptt != null) {
        	try {
        		wm.removeView(ptt);
        	} catch (IllegalArgumentException e) { }
            ptt = null;
        }
		VentriloInterface.logout();
		nm.cancelAll();
		running = false;
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		createPTT();
		return mBinder;
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
	
	private void createPTT() {		
		final boolean show = prefs.getBoolean("show_ptt", false);
		final boolean toggle = prefs.getBoolean("toggle_mode", false);
		ptt = new Button(this);
		ptt.setOnTouchListener(new OnTouchListener() {
			boolean toggleOn = false;
			public boolean onTouch(View v, MotionEvent event) {
				if (show) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						if (toggle) {
							if (toggleOn) {
								setPTTOn(false);
								ptt.setPressed(false);
								toggleOn = false;
							} else {
								setPTTOn(true);
								ptt.setPressed(true);
								toggleOn = true;
							}
						} else {
							setPTTOn(true);
							ptt.setPressed(true);
						}
					} else if (!toggle && event.getAction() == MotionEvent.ACTION_UP) {
						setPTTOn(false);
						ptt.setPressed(false);
					}
					return true;
				}
				return false;
			}
		});
		ptt.setText("Push to Talk");
		
		wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		
		int width = 0, height = 0,
			overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
			flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		
		if (show && !voiceActivation) {
			width = WindowManager.LayoutParams.FILL_PARENT;
			height = WindowManager.LayoutParams.WRAP_CONTENT;
		}
		
		if (prefs.getBoolean("screen_on", false))
			flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        		width, height, overlayType, flags, PixelFormat.TRANSPARENT);
        params.gravity = Gravity.BOTTOM;
		wm.addView(ptt, params);
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
						createNotification(item.name + " has logged in.", data.type, 0);
					break;
					
				case VentriloEvents.V3_EVENT_USER_LOGOUT:
					player.close(data.user.id);
					item = items.getUserById(data.user.id);
					if (((Item.User) item).realId == 0)
						createNotification(item.name + " has logged out.", data.type, 0);
					break;

				case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
					createNotification("Now Connected.", data.type, 0);
					recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())));
					if (voiceActivation)
						recorder.start(threshold);
					sendBroadcast(new Intent(Main.RECEIVER).putExtra("type", VentriloEvents.V3_EVENT_LOGIN_COMPLETE));
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
							createNotification(item.name + " joined the channel.", data.type, 0);
						else if (item.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
							createNotification(item.name + " left the channel.", data.type, 0);
						player.close(data.user.id);
					}
					break;

				case VentriloEvents.V3_EVENT_PLAY_AUDIO:
					player.write(data.user.id, data.pcm.rate, data.pcm.channels, data.data.sample, data.pcm.length);
					break;

				case VentriloEvents.V3_EVENT_USER_TALK_END:
				case VentriloEvents.V3_EVENT_USER_TALK_MUTE:
				case VentriloEvents.V3_EVENT_USER_GLOBAL_MUTE_CHANGED:
				case VentriloEvents.V3_EVENT_USER_CHANNEL_MUTE_CHANGED:
					player.close(data.user.id);
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
	
/*	public boolean processNext() {
		VentriloEventData data = queue.poll();
		if (data != null) {
			process(data);
			return true;
		}
		return false;
	}
	
	public void processAll() {
		VentriloEventData data;
		while ((data = queue.poll()) != null)
			process(data);
		processed = true;
	}*/
	
	public void process(VentriloEventData data) {
		Item item;
		boolean sendBroadcast = true;
		
		switch (data.type) {
		case VentriloEvents.V3_EVENT_PING:
			items.setPing(data.ping);
			sendBroadcast(new Intent(ViewPagerActivity.ACTIVITY_RECEIVER).putExtra("type", data.type).putExtra("ping", data.ping));
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
			if (item.id == VentriloInterface.getuserid()) {
				items.setCurrentChannel(item.parent);
				items.addCurrentUser((Item.User) item);
			} else {
				if (item.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
					items.addCurrentUser((Item.User) item);
				else if (from == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
					items.removeCurrentUser(item.id);
			}
			items.removeUser(data.user.id);
			items.addUser((Item.User) item);
			break;
			
		case VentriloEvents.V3_EVENT_CHAN_ADD:
			item = getChannelFromData(data);
			items.addChannel((Item.Channel) item);
			break;
			
		case VentriloEvents.V3_EVENT_CHAN_BADPASS:
			Intent i = new Intent(ViewPagerActivity.FRAGMENT_RECEIVER)
				.putExtra("type", data.type)
				.putExtra("id", data.channel.id);
			sendBroadcast(i);
			sendBroadcast = false;
			break;
			
		case VentriloEvents.V3_EVENT_ERROR_MSG:
			Toast.makeText(VentriloidService.this, bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
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
			item = getUserFromData(data);
			items.removeUser(item.id);
			items.removeCurrentUser(item.id);
			items.addUser((Item.User) item);
			items.addCurrentUser((Item.User) item);
			break;
		}

		if (sendBroadcast) {
			sendBroadcast(new Intent(ViewPagerActivity.ACTIVITY_RECEIVER).putExtra("type", data.type));
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
	
	private void createNotification(String text, short type, int id) {
		NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(VentriloidService.this)
			.setTicker(text)
	        .setSmallIcon(R.drawable.ic_launcher)
			.setContentIntent(pendingIntent);
		
		switch (type) {
		case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
		case VentriloEvents.V3_EVENT_USER_LOGIN:
		case VentriloEvents.V3_EVENT_USER_LOGOUT:
		case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
	        notifBuilder.setContentText("Connected to " + server.getServername())
	        	.setContentTitle("Ventriloid")
	        	.setOngoing(true)
	        	.setAutoCancel(false);
			break;
		case VentriloEvents.V3_EVENT_USER_PAGE:
			notifBuilder.setContentText(text)
        		.setContentTitle("Page Received")
        		.setAutoCancel(true)
        		.setDefaults(Notification.DEFAULT_VIBRATE)
        		.setContentIntent(pendingIntent);
			break;
		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_MESSAGE:
			// TO-DO Make private chat ID's negative so they won't overwrite page notifications
			break;
		}
		nm.notify(id, notifBuilder.getNotification());
	}

	public VentriloidListAdapter getServerAdapter() {
		return sAdapter;
	}

	public void setServerAdapter(VentriloidListAdapter sAdapter) {
		this.sAdapter = sAdapter;
	}

	public VentriloidListAdapter getChannelAdapter() {
		return cAdapter;
	}

	public void setChannelAdapter(VentriloidListAdapter cAdapter) {
		this.cAdapter = cAdapter;
	}
	
	public void updateViews() {
		sAdapter.notifyDataSetChanged();
		cAdapter.notifyDataSetChanged();
	}

}
