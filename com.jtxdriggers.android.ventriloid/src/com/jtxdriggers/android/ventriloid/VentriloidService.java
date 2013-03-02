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

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.holoeverywhere.widget.Toast;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class VentriloidService extends Service {
	
	static {
		System.loadLibrary("ventrilo_interface");
	}
	
	public static String SERVICE_INTENT = "com.jtxdriggers.android.ventriloid.SERVICE";
	public static String ACTIVITY_RECEIVER = "com.jtxdriggers.android.ventriloid.VentriloidService.ACTIVITY_RECEIVER";
	
	private static boolean connected = false;
	
	private final IBinder BINDER = new MyBinder();
	
	private Handler handler = new Handler();
	private Runnable r;
	private SharedPreferences prefs, volumePrefs, passwordPrefs;
	private PowerManager.WakeLock wakeLock;
	private WifiManager.WifiLock wifiLock;
	private NotificationManager nm;
	private TelephonyManager tm;
	private AudioManager am;
	private Vibrator vibrator;
	private TextToSpeech tts;
	private Ringtone ringtone;
	private Server server;
	private ConcurrentLinkedQueue<VentriloEventData> queue;
	private ItemData items;
	private HashMap<Short, Boolean> notifyMap = new HashMap<Short, Boolean>();
	
	private Recorder recorder;
	private Player player;
	
	private boolean manualSorting = false,
		voiceActivation = false,
		ttsActive = false,
		ringtoneActive = false,
		muted = false,
		vibrate = false,
		admin = false,
		rejoinChat = false,
		running = false,
		bluetoothConnected = false,
		timeout = false,
		disconnect;
	private int reconnectTimer,
		viewType = ViewFragment.VIEW_TYPE_SERVER;
	private short chatId = -1;
	private double threshold;
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		int id = intent.getExtras().getInt("id");
		ServerAdapter db = new ServerAdapter(this);
		server = db.getServer(id);
		
		items = new ItemData(this);
		queue.clear();

		volumePrefs = getSharedPreferences("VOLUMES" + server.getId(), Context.MODE_PRIVATE);
        passwordPrefs = getSharedPreferences("PASSWORDS" + server.getId(), Context.MODE_PRIVATE);
        
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "VentriloidWakeLock");
        wakeLock.acquire();
        
        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "VentriloidWifiLock");
        wifiLock.acquire();
        
        disconnect = true;

		r = new Runnable() {
			public void run() {
				timeout = true;
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						if (timeout) {
							VentriloInterface.logout();
							sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
								.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
							Toast.makeText(getApplicationContext(), "Connection timed out.", Toast.LENGTH_SHORT).show();
							reconnectTimer = 10;
							handler.post(r);
						}
					}
				}, 9500);
				if (VentriloInterface.login(server.getHostname() + ":" + server.getPort(),
						server.getUsername(), server.getPassword(), server.getPhonetic())) {
					new Thread(new Runnable() {
						public void run() {
							while (VentriloInterface.recv());
						}
					}).start();
				} else {
					handler.post(new Runnable() {
						@Override
						public void run() {
							VentriloEventData data = new VentriloEventData();
							VentriloInterface.error(data);
							sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
								.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
							Toast.makeText(getApplicationContext(), bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
						}
					});
				}
				timeout = false;
			}
		};
		handler.post(r);
		
		return Service.START_NOT_STICKY;
	}
	
	@Override
	public void onCreate() {
		stopForeground(true);

		new Thread(new Runnable() {
			@Override
			public void run() {
				Looper.prepare();
				handler = new Handler();
				Looper.loop();
			}
		}).start();

		items = new ItemData(this);
		player = new Player(this);
		recorder = new Recorder(this);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (prefs.getString("notification_type", "Text to Speech").equals("Text to Speech")) {
			ttsActive = true;
			tts = new TextToSpeech(VentriloidService.this, new TextToSpeech.OnInitListener() {				
				@Override
				public void onInit(int status) {
					if (status == TextToSpeech.SUCCESS)
						ttsActive = true;
					else {
						ttsActive = false;
						handler.post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), "TTS Initialization faled.", Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
			});
		} else if (prefs.getString("notification_type", "Text to Speech").equals("Ringtone"))
			ringtoneActive = true;
		
		registerReceiver(activityReceiver, new IntentFilter(ACTIVITY_RECEIVER));

		nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		voiceActivation = prefs.getBoolean("voice_activation", false);
		threshold = voiceActivation ? 55.03125 : -1;
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		vibrate = prefs.getBoolean("vibrate", true);
		
		queue = new ConcurrentLinkedQueue<VentriloEventData>();
		
		//VentriloInterface.debuglevel(65535);
		new Thread(eventHandler).start();
	}

	@Override
	public void onDestroy() {
		running = false;
		connected = false;
        if(tm != null)
            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        
		try {
			unregisterReceiver(bluetoothReceiver);
		} catch (IllegalArgumentException e) { }
		
		unregisterReceiver(activityReceiver);
		VentriloInterface.logout();
		wifiLock.release();
		wakeLock.release();
		nm.cancelAll();
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

	public String bytesToString(byte[] bytes) {
		try {
			return new String(bytes, 0, (new String(bytes).indexOf(0)), prefs.getString("charset", "ISO-8859-1"));
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	private Runnable eventHandler = new Runnable() {
		public void run() {

			running = true;
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
					if ((data.flags & (1 << 0)) == 0 && data.text.real_user_id == 0) {
						nm.notify(VentriloInterface.getuserid(), createNotification(item.name + " has logged in.", data.type, item.id));
						if (ttsActive && !muted && prefs.getBoolean("tts_server", true)) {
							HashMap<String, String> params = new HashMap<String, String>();
							if (am.isBluetoothScoOn())
								params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
							tts.speak((item.phonetic.length() > 0 ? item.phonetic : item.name) + " has logged in.", TextToSpeech.QUEUE_ADD, params);
						} else if (ringtoneActive && !muted) {
							ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("login_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
							ringtone.play();
						}
					}
					break;
					
				case VentriloEvents.V3_EVENT_USER_LOGOUT:
					player.close(data.user.id);
					item = items.getUserById(data.user.id);
					if (item != null && ((Item.User) item).realId == 0) {
						nm.notify(VentriloInterface.getuserid(), createNotification(item.name + " has logged out.", data.type, item.id));
						if (ttsActive && !muted && prefs.getBoolean("tts_server", true)) {
							HashMap<String, String> params = new HashMap<String, String>();
							if (am.isBluetoothScoOn())
								params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
							tts.speak((item.phonetic.length() > 0 ? item.phonetic : item.name) + " has logged out.", TextToSpeech.QUEUE_ADD, params);
						} else if (ringtoneActive && !muted) {
							ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("logout_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
							ringtone.play();
						}
					}
					break;

				case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
					connected = true;
					disconnect = false;
					items.setUserId();
					nm.cancelAll();
					startForeground(VentriloInterface.getuserid(), createNotification("Now Connected.", data.type, (short) 0));
					if (ttsActive && !muted && prefs.getBoolean("tts_connect", true)) {
						HashMap<String, String> params = new HashMap<String, String>();
						if (am.isBluetoothScoOn())
							params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
						tts.speak("Connected.", TextToSpeech.QUEUE_ADD, params);
					} else if (ringtoneActive && !muted) {
						ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("connect_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
						ringtone.play();
					}
					recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())));
					if (voiceActivation)
						recorder.start(threshold);
					sendBroadcast(new Intent(Main.SERVICE_RECEIVER).putExtra("type", data.type));
					if (rejoinChat)
						joinChat();
			        
			        if (tm != null)
			            tm.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
					break;
					
				case VentriloEvents.V3_EVENT_LOGIN_FAIL:
					sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
						.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
					break;

				case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
					if (data.user.id == VentriloInterface.getuserid()) {
						player.clear();
						recorder.rate(VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(data.user.id)));
						if (voiceActivation)
							recorder.start(threshold);
						if (ringtoneActive && !muted) {
							ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("move_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
							ringtone.play();
						}
					} else {
						item = items.getUserById(data.user.id);
						if (item != null) {
							if (data.channel.id == VentriloInterface.getuserchannel(VentriloInterface.getuserid())) {
								nm.notify(VentriloInterface.getuserid(), createNotification(item.name + " joined the channel.", data.type, item.id));
								if (ttsActive && !muted && prefs.getBoolean("tts_channel", true)) {
									HashMap<String, String> params = new HashMap<String, String>();
									if (am.isBluetoothScoOn())
										params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
									tts.speak((item.phonetic.length() > 0 ? item.phonetic : item.name) + " joined the channel.", TextToSpeech.QUEUE_ADD, params);
								} else if (ringtoneActive && !muted) {
									ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("join_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
									ringtone.play();
								}
							} else if (item.parent == VentriloInterface.getuserchannel(VentriloInterface.getuserid())) {
								nm.notify(VentriloInterface.getuserid(), createNotification(item.name + " left the channel.", data.type, item.id));
								if (ttsActive && !muted && prefs.getBoolean("tts_channel", true)) {
									HashMap<String, String> params = new HashMap<String, String>();
									if (am.isBluetoothScoOn())
										params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
									tts.speak((item.phonetic.length() > 0 ? item.phonetic : item.name) + " left the channel.", TextToSpeech.QUEUE_ADD, params);
								} else if (ringtoneActive && !muted) {
									ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("leave_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
									ringtone.play();
								}
							}
						}
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
					recorder.stop();
					
					VentriloInterface.error(data);
					if (data.error.message.length > 0 && !disconnect)
						handler.post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
							}
						});
					
					if (!disconnect) {
						if (ttsActive && !muted && prefs.getBoolean("tts_disconnect", true)) {
							HashMap<String, String> params = new HashMap<String, String>();
							if (am.isBluetoothScoOn())
								params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
							tts.speak("Disconnected.", TextToSpeech.QUEUE_ADD, params);
						} else if (ringtoneActive && !muted) {
							ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("disconnect_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
							ringtone.play();
						}
						
						if (!running) break;
						
						item = items.getCurrentChannel().get(0);
						final short id = item.id;
						final short userId = items.getUserId();
						final String comment = items.getComment();
						final String url = items.getUrl();
						final String integrationText = items.getIntegrationText();
						final boolean inChat = items.inChat();
						reconnectTimer = 10;
						items = new ItemData(VentriloidService.this);
						r = new Runnable() {
							@Override
							public void run() {
								if (!disconnect) {
									if (reconnectTimer > 0) {
										nm.notify(userId, createNotification("Reconnecting in " + reconnectTimer + " seconds", data.type, (short) 0));
										sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
											.putExtra("type", (short) -1)
											.putExtra("timer", reconnectTimer));
										reconnectTimer--;
										handler.postDelayed(this, 1000);
									} else {
										nm.notify(userId, createNotification("Reconnecting...", data.type, (short) 0));
										sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
											.putExtra("type", (short) -1));
										reconnectTimer--;
										timeout = true;
										handler.postDelayed(new Runnable() {
											@Override
											public void run() {
												if (timeout) {
													VentriloInterface.logout();
													sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
														.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
													Toast.makeText(getApplicationContext(), "Connection timed out.", Toast.LENGTH_SHORT).show();
													reconnectTimer = 10;
													handler.post(r);
												}
											}
										}, 9500);
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
											rejoinChat = inChat;
										} else {
											VentriloEventData data = new VentriloEventData();
											VentriloInterface.error(data);
											sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
												.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
											Toast.makeText(getApplicationContext(), bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
											if (data.error.disconnected)
												disconnect();
											reconnectTimer = 10;
											handler.post(this);
										}
										timeout = false;
									}
								}
							}
						};
						handler.post(r);
					}
					break;
					
				case VentriloEvents.V3_EVENT_USER_PAGE:
					item = items.getUserById(data.user.id);
					nm.notify(item.id, createNotification("Page from " + item.name, data.type, item.id));
					if (ttsActive && !muted && prefs.getBoolean("tts_page", true)) {
						HashMap<String, String> params = new HashMap<String, String>();
						if (am.isBluetoothScoOn())
							params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
						tts.speak("Page from " + (item.phonetic.length() > 0 ? item.phonetic : item.name), TextToSpeech.QUEUE_ADD, params);
					} else if (ringtoneActive && !muted) {
						ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("page_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
						ringtone.play();
					}
					break;

				case VentriloEvents.V3_EVENT_PRIVATE_CHAT_MESSAGE:
					if (data.user.privchat_user2 != VentriloInterface.getuserid() && (notifyMap.get(data.user.privchat_user2) == null || notifyMap.get(data.user.privchat_user2))) {
						nm.notify(-data.user.privchat_user2, createNotification(bytesToString(data.data.chatmessage), data.type, data.user.privchat_user2));
					}
					if (ringtoneActive && !muted) {
						ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("pm_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
						ringtone.play();
					}
					break;
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
	
	public synchronized void process(final VentriloEventData data) {
		Item item;
		boolean sendBroadcast = true;
		
		if (data == null)
			return;
		
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
			items.refreshAll();
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
			if (item != null) {
				if (items.chatOpened(data.user.id)) {
					items.chatDisconnect(item.id, item.name);
					sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
				}
				if (((Item.User) item).inChat) {
					items.removeChatUser(data.user.id);
					sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
				}
				if (((Item.User) item).realId == VentriloInterface.getuserid())
					items.getChannelById(item.parent).hasPhantom = false;
			}
			items.removeUser(data.user.id);
			items.removeCurrentUser(data.user.id);
			break;

		case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
			item = items.getUserById(data.user.id);
			if (item != null) {
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
				if (((Item.User) item).realId == VentriloInterface.getuserid()) {
					items.getChannelById(from).hasPhantom = false;
					items.getChannelById(item.parent).hasPhantom = true;
				}
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
			else
				if (bytesToString(data.error.message).length() > 0)
					handler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
						}
					});
			sendBroadcast = false;
			break;

		case VentriloEvents.V3_EVENT_PLAY_AUDIO:
			if (((Item.User) items.getUserById(data.user.id)).xmit != Item.User.XMIT_ON) {
				items.setXmit(data.user.id, Item.User.XMIT_ON);
			} else
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
			
		case VentriloEvents.V3_EVENT_CHAT_MESSAGE:
			item = items.getUserById(data.user.id);
			items.addMessage((short) 0, item.name, bytesToString(data.data.chatmessage));
			sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
			sendBroadcast = false;
			break;
			
		case VentriloEvents.V3_EVENT_CHAT_JOIN:
			items.addChatUser(data.user.id);
			sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
			break;
			
		case VentriloEvents.V3_EVENT_CHAT_LEAVE:
			items.removeChatUser(data.user.id);
			sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
			break;
			

		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_MESSAGE:
			short id = data.user.privchat_user1 == VentriloInterface.getuserid() ? data.user.privchat_user2 : data.user.privchat_user1;
			item = items.getUserById(data.user.privchat_user2);
			if (data.flags > 0)
				items.chatError(id, item.name);
			else
				items.addMessage(id, item.name, bytesToString(data.data.chatmessage));
			sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
			break;
			
		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_START:
			item = items.getUserById(data.user.privchat_user1 == VentriloInterface.getuserid() ? data.user.privchat_user2 : data.user.privchat_user1);
			if (items.chatOpened(item.id))
				items.reopenChat(item.id, item.name);
			else
				items.addChat(item.id, item.name);
			sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
			sendBroadcast = false;
			break;
			
		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_END:
			item = items.getUserById(data.user.privchat_user1 == VentriloInterface.getuserid() ? data.user.privchat_user2 : data.user.privchat_user1);
			items.closeChat(item.id, item.name);
			sendBroadcast(new Intent(ChatFragment.SERVICE_RECEIVER));
			break;
			
		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_AWAY:
		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_BACK:
			break;
			
		case VentriloEvents.V3_EVENT_SERVER_PROPERTY_UPDATED:
			switch (data.serverproperty.property) {
			case VentriloEvents.V3_SRV_PROP_CHAN_ORDER:
				manualSorting = data.serverproperty.value == 0 ? false : true;
				items.refreshAll();
			}
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
		try {
			process(data);
		} catch (NullPointerException e) { }
	}
	
	public boolean isAdmin() {
		return admin;
	}
	
	public void setAdmin(boolean isAdmin) {
		admin = isAdmin;
		Item.Channel c = items.getChannels().get(0);
		c.changeStatus(admin);
		items.setIsAdmin(isAdmin);
		sendBroadcast(new Intent(ViewFragment.SERVICE_RECEIVER));
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
						volumePrefs.getBoolean("mute" + data.user.id, false),
						items.isUserInChat(data.user.id));
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
	
	private Notification createNotification(String text, short type, short id) {
		Intent notifIntent = new Intent(this, Connected.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(VentriloidService.this);
		
		switch (type) {
		case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
		case VentriloEvents.V3_EVENT_USER_LOGIN:
		case VentriloEvents.V3_EVENT_USER_LOGOUT:
		case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
	        notifBuilder.setSmallIcon(R.drawable.headset)
	        	.setContentText("Connected to " + server.getServername())
	        	.setTicker(text)
	        	.setContentTitle("Ventriloid")
	        	.setOngoing(true)
	        	.setAutoCancel(false);
			break;
		case VentriloEvents.V3_EVENT_USER_PAGE:
			notifBuilder.setSmallIcon(R.drawable.about)
				.setContentText(text)
        		.setTicker(text)
        		.setContentTitle("Page Received")
        		.setAutoCancel(true)
	        	.setOngoing(false)
        		.setDefaults(Notification.DEFAULT_VIBRATE);
			break;
		case VentriloEvents.V3_EVENT_PRIVATE_CHAT_MESSAGE:
			notifIntent.putExtra("id", id);
			Item.User u = items.getUserById(id);
			notifBuilder.setSmallIcon(R.drawable.comment)
				.setContentText(text)
				.setTicker(text)
				.setContentTitle(u.name + " - Private Message")
				.setOnlyAlertOnce(true)
				.setAutoCancel(true)
				.setOngoing(false)
				.setDefaults(Notification.DEFAULT_ALL);
			break;
		case VentriloEvents.V3_EVENT_DISCONNECT:
			notifIntent = new Intent(this, Main.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			notifBuilder.setSmallIcon(R.drawable.headset)
				.setContentTitle("Disconnected from Server")
				.setContentText(text)
				.setOngoing(true)
				.setAutoCancel(false);
			if (text.contains(10 + ""))
				notifBuilder.setTicker("Reconnecting to Server");
			else if (text.contains("call"))
				notifBuilder.setTicker(text);
			break;
		}
		
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        notifBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.launcher))
			.setContentIntent(pendingIntent);
        
		return notifBuilder.getNotification();
	}
	
	@SuppressWarnings("deprecation")
	public boolean disconnect() {
		disconnect = true;
		connected = false;
		
		if (ttsActive && !muted && prefs.getBoolean("tts_disconnect", true)) {
			HashMap<String, String> params = new HashMap<String, String>();
			if (am.isBluetoothScoOn())
				params.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_VOICE_CALL));
			params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "Disconnected");
			tts.speak("Disconnected.", TextToSpeech.QUEUE_ADD, params);
			tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
				@Override
				public void onUtteranceCompleted(String utteranceId) {
					if (utteranceId.equals("Disconnected")) {
						tts.shutdown();
						
						stopForeground(true);
						stopSelf();
					}
				}
			});
		} else if (ringtoneActive && !muted) {
			ringtone = RingtoneManager.getRingtone(VentriloidService.this, Uri.parse(prefs.getString("disconnect_notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())));
			ringtone.play();
			
			stopForeground(true);
			stopSelf();
		} else {
			stopForeground(true);
			stopSelf();
		}
		
		return true;
	}
	
	public static boolean isConnected() {
		return connected;
	}
	
	public void setViewType(int viewType, short chatId) {
		this.viewType = viewType;
		this.chatId = chatId;
	}
	
	public int getViewType() {
		return viewType;
	}
	
	public short getChatId() {
		return chatId;
	}
	
	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}
	
	public boolean isManuallySorted() {
		return manualSorting;
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
	
	public void joinChat() {
		items.setInChat(true);
		VentriloInterface.joinchat();
	}
	
	public void leaveChat() {
		items.setInChat(false);
		VentriloInterface.leavechat();
	}
	
	public void setNotify(short userid, boolean notify) {
		notifyMap.put(userid, notify);
	}
	
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.FROYO)
	public void toggleBluetooth() {
		recorder.stop();
		setXmit(false);
		sendBroadcast(new Intent(Connected.SERVICE_RECEIVER).putExtra("type", (short) -1));
		
		if (am.isBluetoothScoOn()) {
			player.setBlock(true);
			player.clear();
			recorder.stop();
			am.stopBluetoothSco();
			items.setBluetooth(false);
			player.setBlock(false);
		} else {
			player.setBlock(true);
			bluetoothConnected = false;
			items.setBluetoothConnecting("Connecting...");
			sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
			am.startBluetoothSco();
			registerReceiver(bluetoothReceiver, new IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_CHANGED));
			handler.post(new Runnable() {
				int counter = 0;
				String text = "Connecting.";
				@Override
				public void run() {
					if (!bluetoothConnected) {
						if (counter <= 10) {
							if (text.equals("Connecting..."))
								text = "Connecting.";
							else
								text += ".";
							items.setBluetoothConnecting(text);
							sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
							
							counter++;
							
							if (!bluetoothConnected)
								handler.postDelayed(this, 1000);
						} else {
							am.stopBluetoothSco();
							items.setBluetooth(false);
							player.setBlock(false);
							sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
							Toast.makeText(VentriloidService.this, "Bluetooth request timed out.", Toast.LENGTH_SHORT).show();
							try {
								unregisterReceiver(bluetoothReceiver);
							} catch (IllegalArgumentException e) { }
						}
					}
				}
			});
		}
	}
	
	private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
		@TargetApi(Build.VERSION_CODES.FROYO)
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!bluetoothConnected && intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) == AudioManager.SCO_AUDIO_STATE_CONNECTED) {
				player.clear();
				items.setBluetooth(true);
				am.setBluetoothScoOn(true);
				bluetoothConnected = true;
				player.setBlock(false);
				sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
				Toast.makeText(VentriloidService.this, "Bluetooth connected.", Toast.LENGTH_SHORT).show();
			} else if (bluetoothConnected && intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, -1) == AudioManager.SCO_AUDIO_STATE_DISCONNECTED) {
				player.setBlock(true);
				player.clear();
				recorder.stop();
				am.stopBluetoothSco();
				items.setBluetooth(false);
				player.setBlock(false);
				sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
				Toast.makeText(VentriloidService.this, "Bluetooth disconnected.", Toast.LENGTH_SHORT).show();
				try {
					unregisterReceiver(bluetoothReceiver);
				} catch (IllegalArgumentException e) { }
			}
		}
	};
	
    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
    	
    	private Item.Channel c;
    	private short channelId, userId;
    	private String comment, url, integrationText;
    	private boolean inChat, reconnectBluetooth;
    	
        @TargetApi(Build.VERSION_CODES.FROYO)
		@Override
        public void onCallStateChanged(int state, String incomingNumber) {
        	switch (state) {
        	case TelephonyManager.CALL_STATE_OFFHOOK:
        		disconnect = true;
        		connected = false;
        		recorder.stop();
				c = items.getCurrentChannel().get(0);
				channelId = c.id;
				userId = items.getUserId();
				comment = items.getComment();
				url = items.getUrl();
				integrationText = items.getIntegrationText();
				inChat = items.inChat();
				reconnectBluetooth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? am.isBluetoothScoOn() : false;
				VentriloInterface.logout();
				nm.notify(userId, createNotification("Disconnected while in call", (short) VentriloEvents.V3_EVENT_DISCONNECT, (short) 0));
	        	
	            super.onCallStateChanged(state, incomingNumber);
	            
        		break;
        	case TelephonyManager.CALL_STATE_IDLE:
                super.onCallStateChanged(state, incomingNumber);
                
        		if (!disconnect)
        			break;
        		
        		reconnectTimer = 10;
				items = new ItemData(VentriloidService.this);
				r = new Runnable() {					
					@Override
					public void run() {
						if (reconnectTimer > 0) {
							nm.notify(userId, createNotification("Reconnecting in " + reconnectTimer + " seconds", (short) VentriloEvents.V3_EVENT_DISCONNECT, (short) 0));
							sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
								.putExtra("type", (short) -1)
								.putExtra("timer", reconnectTimer));
							reconnectTimer--;
							handler.postDelayed(this, 1000);
						} else {
							nm.notify(userId, createNotification("Reconnecting...", (short) VentriloEvents.V3_EVENT_DISCONNECT, (short) 0));
							sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
								.putExtra("type", (short) -1));
							reconnectTimer--;
							timeout = true;
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									if (timeout) {
										VentriloInterface.logout();
										sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
											.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
										Toast.makeText(getApplicationContext(), "Connection timed out.", Toast.LENGTH_SHORT).show();
										reconnectTimer = 10;
										handler.post(r);
									}
								}
							}, 9500);
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
								VentriloInterface.changechannel(channelId, passwordPrefs.getString(channelId + "pw", ""));
								rejoinChat = inChat;
				        		
				        		if (reconnectBluetooth) {
					        		player.setBlock(true);
									bluetoothConnected = false;
									items.setBluetoothConnecting("Connecting...");
									sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
									am.startBluetoothSco();
									handler.post(new Runnable() {
										int counter = 0;
										String text = "Connecting.";
										@TargetApi(Build.VERSION_CODES.FROYO)
										@Override
										public void run() {
											if (!bluetoothConnected) {
												if (counter <= 10) {
													if (text.equals("Connecting..."))
														text = "Connecting.";
													else
														text += ".";
													items.setBluetoothConnecting(text);
													sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
													
													counter++;
													
													if (!bluetoothConnected)
														handler.postDelayed(this, 1000);
												} else {
													am.stopBluetoothSco();
													items.setBluetooth(false);
													player.setBlock(false);
													sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
													Toast.makeText(VentriloidService.this, "Bluetooth request timed out.", Toast.LENGTH_SHORT).show();
													try {
														unregisterReceiver(bluetoothReceiver);
													} catch (IllegalArgumentException e) { }
												}
											}
										}
									});
				        		} else {
				        			items.setBluetooth(false);
									sendBroadcast(new Intent(Connected.SERVICE_RECEIVER));
				        		}
							} else {
								VentriloEventData data = new VentriloEventData();
								VentriloInterface.error(data);
								sendBroadcast(new Intent(Main.SERVICE_RECEIVER)
									.putExtra("type", (short)VentriloEvents.V3_EVENT_LOGIN_FAIL));
								Toast.makeText(getApplicationContext(), bytesToString(data.error.message), Toast.LENGTH_SHORT).show();
								if (data.error.disconnected)
									disconnect();
								reconnectTimer = 10;
								handler.post(this);
							}
							timeout = false;
						}
					}
				};
				handler.post(r);
        		break;
        	default:
                super.onCallStateChanged(state, incomingNumber);
        	}
        }
    };
	
}