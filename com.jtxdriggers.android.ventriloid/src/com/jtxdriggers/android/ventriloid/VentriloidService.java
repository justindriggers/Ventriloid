package com.jtxdriggers.android.ventriloid;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class VentriloidService extends Service {
	
	static {
		System.loadLibrary("ventrilo_interface");
	}
	
	public static String SERVICE_INTENT = "com.jtxdriggers.android.ventriloid.SERVICE";
	
	private final IBinder mBinder = new MyBinder();
	private boolean running = false;
	private ItemData items = new ItemData();
	private int start;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int id = intent.getExtras().getInt("id");
		ServerAdapter db = new ServerAdapter(this);
		final Server server = db.getServer(id);
		
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
		VentriloInterface.debuglevel(1 << 11);
		running = true;
		new Thread(eventHandler).start();
	}

	@Override
	public void onDestroy() {
		VentriloInterface.logout();
		running = false;
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent i) {
		return mBinder;
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
			Bundle extras;
			
			while (running) {
				VentriloEventData data = new VentriloEventData();
				VentriloInterface.getevent(data);
				//Log.d("ventriloid", "Processing event type " + data.type);
				
				item = new Item();
				
				extras = new Bundle();
				extras.putInt("type", data.type);

				switch (data.type) {
				case VentriloEvents.V3_EVENT_PING:
					extras.putInt("ping", data.ping);
					break;
					
				case VentriloEvents.V3_EVENT_USER_LOGIN:
					item = getUserFromData(data);
					if (item.id != 0) {
						items.addUser((Item.User) item);
						items.addCurrentUser((Item.User) item);
						//int flags = data.flags;

						//if ((flags & (1 << 0)) == 0)
							//sv.createNotification(sv.NOTIF_ONGOING, "Ventriloid", item.name + " has logged in.");
					} else {
						Item.Channel c = item.new Channel(item.name, item.phonetic, item.comment);
						items.setLobby(c);
					}
					break;
					
				case VentriloEvents.V3_EVENT_USER_LOGOUT:
					Player.close(data.user.id);
					items.removeUser(data.user.id);
					items.removeCurrentUser(data.user.id);
					break;

				case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
					Recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())));
					sendBroadcast(new Intent(Main.RECEIVER).putExtras(extras));
					break;

				case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
					item = getUserFromData(data);
					if (item.id == VentriloInterface.getuserid()) {
						Player.clear();
						Recorder.rate(VentriloInterface.getchannelrate(data.channel.id));
						items.setCurrentChannel(item.parent);
						items.addCurrentUser((Item.User) item);
					} else {
						if (item.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
							items.addCurrentUser((Item.User) item);
						else if (items.getUserById(data.user.id).parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid()))
							items.removeCurrentUser(item.id);
						Player.close(data.user.id);
					}
					item = items.getUserById(data.user.id);
					item.parent = data.channel.id;
					items.removeUser(data.user.id);
					items.addUser((Item.User) item);
					break;

				case VentriloEvents.V3_EVENT_CHAN_ADD:
					item = getChannelFromData(data);
					items.addChannel((Item.Channel) item);
					break;
					
				case VentriloEvents.V3_EVENT_USER_TALK_START:
					items.setXmit(data.user.id, Item.User.XMIT_INIT);
					break;

				case VentriloEvents.V3_EVENT_PLAY_AUDIO:
					Player.write(data.user.id, data.pcm.rate, data.pcm.channels, data.data.sample, data.pcm.length);
					if (items.getUserById(data.user.id).xmit != Item.User.XMIT_ON)
						items.setXmit(data.user.id, Item.User.XMIT_ON);
					break;

				case VentriloEvents.V3_EVENT_USER_TALK_END:
				case VentriloEvents.V3_EVENT_USER_TALK_MUTE:
				case VentriloEvents.V3_EVENT_USER_GLOBAL_MUTE_CHANGED:
				case VentriloEvents.V3_EVENT_USER_CHANNEL_MUTE_CHANGED:
					Player.close(data.user.id);
					items.setXmit(data.user.id, Item.User.XMIT_OFF);
					break;
					
				case VentriloEvents.V3_EVENT_USER_MODIFY:
					item = getUserFromData(data);
					items.removeUser(item.id);
					items.addUser((Item.User) item);
					items.removeCurrentUser(item.id);
					items.addCurrentUser((Item.User) item);
					break;
				}
				sendBroadcast(new Intent(ViewPagerActivity.RECEIVER).putExtras(extras));
			}
			Player.clear();
			Recorder.stop();
		}
	};
	
	public ItemData getItemData() {
		return items;
	}
	
	private Item.User getUserFromData(VentriloEventData data) {
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
	
	private Item.Channel getChannelFromData(VentriloEventData data) {
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

}
