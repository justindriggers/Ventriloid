package com.jtxdriggers.android.ventriloid;

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ExpandableListView;
import org.holoeverywhere.widget.ExpandableListView.OnChildClickListener;
import org.holoeverywhere.widget.LinearLayout;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout.LayoutParams;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class Connected extends Activity {
	
	public static final String SERVICE_RECEIVER = "com.jtxdriggers.android.ventriloid.Connected.SERVICE_RECEIVER";
	
	private VentriloidService s;
	private VentriloidSlidingMenu sm;
	private ViewFragment fragment;
	
	private Button ptt;
	private boolean pttToggle = false,
		pttEnabled = false,
		toggleOn = false;
	private int pttKey;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.connected);
		
		getSupportActionBar().setSubtitle("Checking Latency...");
		
		sm = new VentriloidSlidingMenu(this);
		sm.attachToActivity(this, VentriloidSlidingMenu.SLIDING_CONTENT);
		sm.getListView().setOnChildClickListener(menuClickListener);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (!prefs.getBoolean("voice_activation", false)) {
			pttToggle = prefs.getBoolean("toggle_mode", false);
			pttEnabled = prefs.getBoolean("custom_ptt", false);
			pttKey = pttEnabled ? prefs.getInt("ptt_key", KeyEvent.KEYCODE_CAMERA) : -1;
		
			ptt = (Button) findViewById(R.id.ptt);
	        ptt.setOnTouchListener(new OnTouchListener() {
	            public boolean onTouch(View v, MotionEvent event) {
	                if (event.getAction() == MotionEvent.ACTION_DOWN) {
	                    if (pttToggle) {
	                    	toggleOn = !toggleOn;
	                    	s.setPTTOn(toggleOn);
	                    	ptt.setPressed(toggleOn);
	                    	return true;
	                    } else {
		                    s.setPTTOn(true);
		                    ptt.setPressed(true);
		                    return true;
	                    }
	                } else if (!pttToggle && event.getAction() == MotionEvent.ACTION_UP) {
	                    s.setPTTOn(false);
	                    ptt.setPressed(false);
	                    return true;
	                }
	                return false;
	            }
	        });
		} else
			((LinearLayout) findViewById(R.id.bottomBar)).setVisibility(LinearLayout.GONE);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		bindService(new Intent(VentriloidService.SERVICE_INTENT), serviceConnection, Context.BIND_AUTO_CREATE);
	}
    
	@Override
    public void onStop() {
    	unregisterReceiver(serviceReceiver);
    	unbindService(serviceConnection);
    	super.onStop();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.connected, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (menu.findItem(R.id.mute) != null && s != null) 
			menu.findItem(R.id.mute).setIcon(s.isMuted() ? R.drawable.muted : R.drawable.unmuted);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.mute:
			if (s.isMuted()) {
				s.setMuted(false);
				item.setIcon(R.drawable.unmuted);
				item.setTitle("Mute");
			} else {
				s.setMuted(true);
				item.setIcon(R.drawable.muted);
				item.setTitle("Unmute");
			}
			return true;
		case R.id.show_menu:
			if (sm.isMenuShowing())
				sm.showContent();
			else
				sm.showMenu();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		if (sm.isMenuShowing()) {
			sm.showContent();
			return;
		}

		super.onBackPressed();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!pttEnabled || s == null || keyCode != pttKey)
			return super.onKeyDown(keyCode, event);
		
		if (pttToggle) {
			toggleOn = !toggleOn;
			s.setPTTOn(toggleOn);
			ptt.setPressed(toggleOn);
		} else {
			s.setPTTOn(true);
            ptt.setPressed(true);
		}
		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (pttEnabled && !pttToggle && keyCode == pttKey) {
			s.setPTTOn(false);
            ptt.setPressed(false);
            return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU && !sm.isMenuShowing())
			sm.showMenu();

		return super.onKeyUp(keyCode, event);
	}
	
	private OnChildClickListener menuClickListener = new OnChildClickListener() {
		@Override
		public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
			switch (groupPosition) {
			case VentriloidSlidingMenu.MENU_SWITCH_VIEW:
				switch (childPosition) {
				case VentriloidSlidingMenu.MENU_SERVER_VIEW:
					s.setViewType(ViewFragment.VIEW_TYPE_SERVER);
					fragment = ViewFragment.newInstance(s.getViewType());
					getSupportFragmentManager()
						.beginTransaction()
						.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
						.replace(R.id.content_frame, fragment)
						.commit();
					sm.setActiveView(VentriloidSlidingMenu.MENU_SERVER_VIEW);
					sm.notifyDataSetChanged();
					return true;
				case VentriloidSlidingMenu.MENU_CHANNEL_VIEW:
					s.setViewType(ViewFragment.VIEW_TYPE_CHANNEL);
					fragment = ViewFragment.newInstance(s.getViewType());
					getSupportFragmentManager()
						.beginTransaction()
						.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
						.replace(R.id.content_frame, fragment)
						.commit();
					sm.setActiveView(VentriloidSlidingMenu.MENU_CHANNEL_VIEW);
					sm.notifyDataSetChanged();
					return true;
				}
				break;
			case VentriloidSlidingMenu.MENU_USER_OPTIONS:
				switch (childPosition) {
				case VentriloidSlidingMenu.MENU_SET_COMMENT:
					break;
				case VentriloidSlidingMenu.MENU_SET_URL:
					break;
				}
				break;
			case VentriloidSlidingMenu.MENU_CLOSE:
				switch (childPosition) {
				case VentriloidSlidingMenu.MENU_MINIMIZE:
					finish();
					return true;
				case VentriloidSlidingMenu.MENU_DISCONNECT:
					s.disconnect();
					startActivity(new Intent(Connected.this, Main.class));
					finish();
					return true;
				}
				break;
			}
			return false;
		}
	};
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			registerReceiver(serviceReceiver, new IntentFilter(SERVICE_RECEIVER));

			getSupportActionBar().setTitle(s.getServername());

			fragment = ViewFragment.newInstance(s.getViewType());
			getSupportFragmentManager()
				.beginTransaction()
				.replace(R.id.content_frame, fragment)
				.commit();
			sm.setActiveView(s.getViewType());
			sm.notifyDataSetChanged();
			
			setPing(s.getItemData().getPing());
		}

		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};
	
	private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			short type = intent.getShortExtra("type", (short)0);
			switch (type) {
			case VentriloEvents.V3_EVENT_DISCONNECT:
				startActivity(new Intent(Connected.this, Main.class));
				finish();
				break;
			case VentriloEvents.V3_EVENT_PING:
				setPing(intent.getIntExtra("ping", -1));
				break;
			case VentriloEvents.V3_EVENT_CHAN_BADPASS:
				final SharedPreferences passwordPrefs = getSharedPreferences("PASSWORDS" + s.getServerId(), Context.MODE_PRIVATE);
	            final Item.Channel c = s.getItemData().getChannelById(intent.getShortExtra("id", (short)0));
	            passwordPrefs.edit().remove(c.id + "pw").commit();
	            if (c.reqPassword) {
                    AlertDialog.Builder passwordDialog = new AlertDialog.Builder(Connected.this);
                    LinearLayout layout = new LinearLayout(Connected.this);
                    final EditText input = new EditText(Connected.this);
                    input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
            	    int pixels = (int) (getResources().getDisplayMetrics().density * 20);
            	    LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            	    params.setMargins(pixels, pixels, pixels, pixels);
            	    input.setLayoutParams(params);
            	    layout.addView(input);
	                passwordDialog.setTitle("Enter Channel Password:")
	                    .setView(layout)
	                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                            public void onClick(DialogInterface dialog, int which) {
	                                    VentriloInterface.changechannel(c.id, input.getText().toString());
	                                    passwordPrefs.edit().putString(c.id + "pw", input.getText().toString()).commit();
	                                    return;
	                            }
	                    })
	                    .setNegativeButton("Cancel", null)
	                    .show();
	            } else
	                    VentriloInterface.changechannel(c.id, "");
	            break;
			default:
				fragment.notifyDataSetChanged();
			}
		}
	};
	
	private void setPing(int ping) {
		if (ping < 65535 && ping > 0)
			getSupportActionBar().setSubtitle("Ping: " + ping + "ms");
		else
			getSupportActionBar().setSubtitle("Checking latency...");
	}

}
