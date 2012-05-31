package com.jtxdriggers.android.ventriloid;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class VentriloidService extends Service {
	
	static {
		System.loadLibrary("ventrilo_interface");
	}
	
	private VentriloEventHandler handler;
	
	private final IBinder mBinder = new MyBinder();
	private boolean running = false;
	private static ConcurrentLinkedQueue<VentriloEventData> eventQueue;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int id = intent.getExtras().getInt("id");
		ServerAdapter db = new ServerAdapter(this);
		Server server = db.getServer(id);
		Log.d("ventriloid", "Username: " + server.getUsername());
		Log.d("ventriloid", "Phonetic: " + server.getPhonetic());
		Log.d("ventriloid", "Servername: " + server.getServername());
		Log.d("ventriloid", "Hostname: " + server.getHostname());
		Log.d("ventriloid", "Port: " + server.getPort());
		Log.d("ventriloid", "Password: " + server.getPassword());
		
		if (VentriloInterface.login(server.getHostname() + ":" + server.getPort(),
				server.getUsername(), server.getPassword(), server.getPhonetic())) {
			startRecvThread();
		
			return Service.START_STICKY;
		} else
			return Service.START_FLAG_RETRY;
	}
	
	@Override
	public void onCreate() {
		VentriloInterface.debuglevel(1 << 11);
		handler = new VentriloEventHandler();
		eventQueue = new ConcurrentLinkedQueue<VentriloEventData>();
		running = true;
		Thread t = new Thread(eventRunnable);
		t.start();
	}

	@Override
	public void onDestroy() {
		running = false;
		eventQueue = null;
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
	
	private void startRecvThread() {
		Runnable recvRunnable = new Runnable() {
			public void run() {
				while (VentriloInterface.recv())
					;
			}
		};
		(new Thread(recvRunnable)).start();
	}

	public static String StringFromBytes(byte[] bytes) {
		return new String(bytes, 0, (new String(bytes).indexOf(0)));
	}

	private Runnable eventRunnable = new Runnable() {

		public void run() {
			boolean forwardToUI = true;
			final int INIT = 0;
			final int ON = 1;
			final int OFF = 2;
			HashMap<Short, Integer> talkState = new HashMap<Short, Integer>();
			while (running) {
				forwardToUI = true;

				VentriloEventData data = new VentriloEventData();
				VentriloInterface.getevent(data);

				// Process audio packets here and let everything else queue up
				// for the UI thread
				switch (data.type) {
					case VentriloEvents.V3_EVENT_USER_LOGOUT:
						Player.close(data.user.id);
						talkState.put(data.user.id, OFF);
						break;

					case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
						Recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())));
						break;

					case VentriloEvents.V3_EVENT_USER_TALK_START:
						talkState.put(data.user.id, INIT);
						break;

					case VentriloEvents.V3_EVENT_PLAY_AUDIO:
						Player.write(data.user.id, data.pcm.rate, data.pcm.channels, data.data.sample, data.pcm.length);
						// Only forward the first play audio event to the UI
						if (talkState.get(data.user.id) != ON) {
							talkState.put(data.user.id, ON);
						} else {
						forwardToUI = false;
						}
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
				if (forwardToUI) {
					// In order to conserve memory, let the consumer catch up
					// before putting too many objects in the event queue
					while (eventQueue.size() > 25) {
						try {
							this.wait(10);
						} catch (Exception e) {
						}
					}
					eventQueue.add(data);
					handler.process();
					//sendBroadcast(new Intent(ServerView.EVENT_ACTION));
				}
			}
			Player.clear();
			Recorder.stop();
		}
	};

	public static VentriloEventData getNext() {
		if (eventQueue == null) {
			return null;
		}
		return eventQueue.poll();
	}
	
	public static void clearEvents() {
		eventQueue.clear();
	}

}
