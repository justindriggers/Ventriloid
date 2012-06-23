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
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;

public class VentriloidService extends Service {
	
	static {
		System.loadLibrary("ventrilo_interface");
	}
	
	public static String SERVICE_INTENT = "com.jtxdriggers.android.ventriloid.SERVICE";
	
	private final IBinder mBinder = new MyBinder();
	private WindowManager wm;
	private NotificationManager nm;
	private Vibrator vibrator;
	private Notification notif;
	private SharedPreferences prefs;
	private Server server;
	private boolean running = false;
	private boolean voiceActivation = false;
	private boolean vibrate = false;
	private ItemData items = new ItemData();
	private Recorder recorder = new Recorder(this);
	private ConcurrentLinkedQueue<VentriloEventData> queue;
	private int start;
	private double threshold = -1;
	private Button ptt;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int id = intent.getExtras().getInt("id");
		ServerAdapter db = new ServerAdapter(this);
		server = db.getServer(id);

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
				} else
					start = Service.START_NOT_STICKY;
			}
		}).start();
		
		return start;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		voiceActivation = prefs.getBoolean("voice_activation", false);
		if (voiceActivation)
			threshold = 55.03125;
		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		if (prefs.getBoolean("vibrate", true));
			vibrate = true;
		queue = new ConcurrentLinkedQueue<VentriloEventData>();
		//VentriloInterface.debuglevel(1 << 11);
		running = true;
		new Thread(eventHandler).start();
	}

	@Override
	public void onDestroy() {
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
	
	private void createPTT() {		
		final boolean show = prefs.getBoolean("show_ptt", false);
		final boolean toggle = prefs.getBoolean("toggle_mode", false);
		ptt = new Button(this);
		ptt.setOnTouchListener(new OnTouchListener() {
			boolean toggleOn = false;
			public boolean onTouch(View v, MotionEvent event) {
				if (show) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						if (vibrate)
							vibrator.vibrate(25);
						if (toggle) {
							if (toggleOn) {
								recorder.stop();
								setXmit(false);
								ptt.setPressed(false);
								toggleOn = false;
							} else {
								recorder.start();
								setXmit(true);
								ptt.setPressed(true);
								toggleOn = true;
							}
						} else {
							recorder.start();
							setXmit(true);
							ptt.setPressed(true);
						}
					} else if (!toggle && event.getAction() == MotionEvent.ACTION_UP) {
						recorder.stop();
						setXmit(false);
						if (vibrate)
							vibrator.vibrate(25);
						ptt.setPressed(false);
					}
					return true;
				}
				return false;
			}
		});
		ptt.setText("Push to Talk");
		
		wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		
		int wrap = 0,
			overlayType = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
			flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
		
		if (show && !voiceActivation)
			wrap = WindowManager.LayoutParams.WRAP_CONTENT;
		
		if (prefs.getBoolean("screen_on", false))
			flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        		wrap, wrap, overlayType, flags, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.CENTER_VERTICAL | Gravity.BOTTOM;
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
				VentriloEventData data = new VentriloEventData();
				VentriloInterface.getevent(data);
				
				switch (data.type) {
				case VentriloEvents.V3_EVENT_USER_LOGIN:
					item = getUserFromData(data);
					if ((data.flags & (1 << 0)) == 0)
						createNotification(item.name + " has logged in.", true);
					break;
					
				case VentriloEvents.V3_EVENT_USER_LOGOUT:
					Player.close(data.user.id);
					item = items.getUserById(data.user.id);
					createNotification(item.name + " has logged out.", true);
					break;

				case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
					notif = new Notification(R.drawable.icon, "Now Connected", 0);
					Intent notifIntent = new Intent(VentriloidService.this, ViewPagerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					notif.setLatestEventInfo(getApplicationContext(), "Ventriloid", "Connected to " + server.getServername(), PendingIntent.getActivity(VentriloidService.this, 0, notifIntent, 0));
					notif.flags |= Notification.FLAG_ONGOING_EVENT;
					/*RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.noti);
					contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
					contentView.setTextViewText(R.id.status_text, "Ventriloid");
					notif.contentView = contentView;
					notif.contentIntent = pIntent;*/
					nm.notify(0, notif);
					recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())));
					if (voiceActivation)
						recorder.start(threshold);
					sendBroadcast(new Intent(Main.RECEIVER).putExtra("type", VentriloEvents.V3_EVENT_LOGIN_COMPLETE));
					break;

				case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
					if (data.user.id == VentriloInterface.getuserid()) {
						Player.clear();
						recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(data.user.id)));
						if (voiceActivation)
							recorder.start(threshold);
					} else {
						item = items.getUserById(data.user.id);
						if (data.channel.id == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
							createNotification(item.name + " joined the channel.", true);
						else if (item.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
							createNotification(item.name + " left the channel.", true);
						Player.close(data.user.id);
					}
					break;

				case VentriloEvents.V3_EVENT_PLAY_AUDIO:
					Player.write(data.user.id, data.pcm.rate, data.pcm.channels, data.data.sample, data.pcm.length);
					break;

				case VentriloEvents.V3_EVENT_USER_TALK_END:
				case VentriloEvents.V3_EVENT_USER_TALK_MUTE:
				case VentriloEvents.V3_EVENT_USER_GLOBAL_MUTE_CHANGED:
				case VentriloEvents.V3_EVENT_USER_CHANNEL_MUTE_CHANGED:
					Player.close(data.user.id);
					break;
					
				case VentriloEvents.V3_EVENT_USER_PAGE:
					item = getUserFromData(data);
					createNotification("Page from " + item.name, false);
					break;
				}
				queue.add(data);
				sendBroadcast(new Intent(ViewPagerActivity.ACTIVITY_RECEIVER));
			}
			Player.clear();
			recorder.stop();
		}
	};
	
	public boolean processNext() {
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
	}
	
	public void process(VentriloEventData data) {
		Item item;
		boolean sendBroadcast = false;
		
		switch (data.type) {
		case VentriloEvents.V3_EVENT_PING:
			items.setPing(data.ping);
			sendBroadcast(new Intent(ViewPagerActivity.ACTIVITY_RECEIVER).putExtra("ping", data.ping));
			break;
			
		case VentriloEvents.V3_EVENT_USER_LOGIN:
			item = getUserFromData(data);
			if (item.id != 0) {
				items.addUser((Item.User) item);
				items.addCurrentUser((Item.User) item);
			} else {
				Item.Channel c = item.new Channel(item.name, item.phonetic, item.comment);
				items.setLobby(c);
			}
			sendBroadcast = true;
			break;
			
		case VentriloEvents.V3_EVENT_USER_LOGOUT:
			items.removeUser(data.user.id);
			items.removeCurrentUser(data.user.id);
			sendBroadcast = true;
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
			sendBroadcast = true;
			break;
			
		case VentriloEvents.V3_EVENT_CHAN_ADD:
			item = getChannelFromData(data);
			items.addChannel((Item.Channel) item);
			sendBroadcast = true;
			break;

		case VentriloEvents.V3_EVENT_PLAY_AUDIO:
			Item.User u = items.getUserById(data.user.id);
			if (u.xmit != Item.User.XMIT_ON) {
				items.setXmit(data.user.id, Item.User.XMIT_ON);
				sendBroadcast = true;
			}
			break;
			
		case VentriloEvents.V3_EVENT_USER_TALK_START:
			items.setXmit(data.user.id, Item.User.XMIT_INIT);
			sendBroadcast = true;
			break;

		case VentriloEvents.V3_EVENT_USER_TALK_END:
		case VentriloEvents.V3_EVENT_USER_TALK_MUTE:
		case VentriloEvents.V3_EVENT_USER_GLOBAL_MUTE_CHANGED:
		case VentriloEvents.V3_EVENT_USER_CHANNEL_MUTE_CHANGED:
			items.setXmit(data.user.id, Item.User.XMIT_OFF);
			sendBroadcast = true;
			break;
			
		case VentriloEvents.V3_EVENT_USER_MODIFY:
			item = getUserFromData(data);
			items.removeUser(item.id);
			items.removeCurrentUser(item.id);
			items.addUser((Item.User) item);
			items.addCurrentUser((Item.User) item);
			sendBroadcast = true;
			break;
		}
		if (sendBroadcast)
			sendBroadcast(new Intent(ViewPagerActivity.FRAGMENT_RECEIVER));
	}
	
	public void setXmit(boolean on) {
		VentriloEventData data = new VentriloEventData();
		data.user.id = VentriloInterface.getuserid();
		if (on)
			data.type = VentriloEvents.V3_EVENT_PLAY_AUDIO;
		else
			data.type = VentriloEvents.V3_EVENT_USER_TALK_END;
		queue.add(data);
		sendBroadcast(new Intent(ViewPagerActivity.ACTIVITY_RECEIVER));
	}
	
	public ItemData getItemData() {
		return items;
	}
	
	public Item.User getUserFromData(VentriloEventData data) {
		VentriloInterface.getuser(data, data.user.id);
		Item item = new Item();
		Item.User u = item.new User(data.user.id,
						VentriloInterface.getuserchannel(data.user.id),
						bytesToString(data.text.name),
						bytesToString(data.text.phonetic),
						bytesToString(data.data.rank.name),
						bytesToString(data.text.comment),
						bytesToString(data.text.url),
						bytesToString(data.text.integration_text));
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
						data.data.channel.password_protected);
		return c;
	}
	
	private void createNotification(String text, boolean autoCancel) {
		//RemoteViews notifView = new RemoteViews(getPackageName(), R.layout.notif_layout);
		//notifView.setOnClickPendingIntent(R.id.status_icon, PendingIntent.getBroadcast(context, requestCode, intent, flags));
		//notif.contentView = notifView;
		Intent notifIntent = new Intent(this, ViewPagerActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (autoCancel) {
			notif = new Notification(R.drawable.icon, text, 0);
			notif.setLatestEventInfo(this, "Ventriloid", "Connected to " + server.getServername(), PendingIntent.getActivity(this, 0, notifIntent, 0));
			nm.notify(1, notif);
			nm.cancel(1);
		} else {
			notif = new Notification(R.drawable.icon, text, System.currentTimeMillis());
			notif.setLatestEventInfo(this, "Ventriloid", text, PendingIntent.getActivity(this, 0, notifIntent, 0));
			notif.flags = Notification.FLAG_AUTO_CANCEL;
			notif.defaults = Notification.DEFAULT_VIBRATE;
			nm.notify(2, notif);
		}
	}

}
