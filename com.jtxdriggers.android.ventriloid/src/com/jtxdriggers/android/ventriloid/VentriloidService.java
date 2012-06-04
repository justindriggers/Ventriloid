package com.jtxdriggers.android.ventriloid;

import java.util.HashMap;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class VentriloidService extends Service {
	
	static {
		System.loadLibrary("ventrilo_interface");
	}
	
	public static String SERVICE_INTENT = "com.jtxdriggers.android.ventriloid.SERVICE";
	
	private final IBinder mBinder = new MyBinder();
	private boolean running = false;
	private int start;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int id = intent.getExtras().getInt("id");
		ServerAdapter db = new ServerAdapter(this);
		final Server server = db.getServer(id);
		
		Log.d("ventriloid", "Starting login");
		new Thread(new Runnable() {
			public void run() {
				if (VentriloInterface.login(server.getHostname() + ":" + server.getPort(),
						server.getUsername(), server.getPassword(), server.getPhonetic())) {

					new Thread(new Runnable() {
						public void run() {
							while (VentriloInterface.recv());
						}
					}).start();
				
					Log.d("ventriloid", "Login success");
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
			Bundle extras;
			final int INIT = 0;
			final int ON = 1;
			final int OFF = 2;
			HashMap<Short, Integer> talkState = new HashMap<Short, Integer>();
			
			while (running) {
				VentriloEventData data = new VentriloEventData();
				VentriloInterface.getevent(data);
				Log.d("ventriloid", "Processing event type " + data.type);
				extras = new Bundle();
				extras.putInt("type", data.type);

				switch (data.type) {
				case VentriloEvents.V3_EVENT_PING:
					extras.putInt("ping", data.ping);
					broadcast(extras);
					break;
					
				case VentriloEvents.V3_EVENT_USER_LOGOUT:
					Player.close(data.user.id);
					talkState.put(data.user.id, OFF);
					break;

				case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
					Recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())));
					sendBroadcast(new Intent(Main.RECEIVER).putExtras(extras));
					break;

				case VentriloEvents.V3_EVENT_USER_TALK_START:
					talkState.put(data.user.id, INIT);
					break;

				case VentriloEvents.V3_EVENT_PLAY_AUDIO:
					Player.write(data.user.id, data.pcm.rate, data.pcm.channels, data.data.sample, data.pcm.length);
					if (talkState.get(data.user.id) != ON)
						talkState.put(data.user.id, ON);
					break;

				case VentriloEvents.V3_EVENT_USER_TALK_END:
				case VentriloEvents.V3_EVENT_USER_TALK_MUTE:
				case VentriloEvents.V3_EVENT_USER_GLOBAL_MUTE_CHANGED:
				case VentriloEvents.V3_EVENT_USER_CHANNEL_MUTE_CHANGED:
					Player.close(data.user.id);
					talkState.put(data.user.id, OFF);
					break;

				case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
					if (data.user.id == VentriloInterface.getuserid()) {
						Player.clear();
						Recorder.rate(VentriloInterface.getchannelrate(data.channel.id));
						talkState.put(data.user.id, OFF);
					} else {
						Player.close(data.user.id);
						talkState.put(data.user.id, OFF);
					}
					break;
				}
			}
			Player.clear();
			Recorder.stop();
		}
	};
	
	public void broadcast(Bundle extras) {
		sendBroadcast(new Intent(ViewPagerActivity.RECEIVER).putExtras(extras));
	}

}
