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
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class Connected extends Activity {
	
	public static final String SERVICE_RECEIVER = "com.jtxdriggers.android.ventriloid.Connected.SERVICE_RECEIVER";
	
	public static final int SMALL = 1, MEDIUM = 2, LARGE = 3;
	
	private VentriloidService s;
	private VentriloidSlidingMenu sm;
	private ViewFragment fragment;
	
	private Button ptt, pttSizeUp, pttSizeDown;
	private RelativeLayout bottomBar;
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

			bottomBar = (RelativeLayout) findViewById(R.id.bottomBar);
			pttSizeUp = (Button) findViewById(R.id.pttSizeUp);
			pttSizeUp.setOnClickListener(sizeChangeListener);
			pttSizeDown = (Button) findViewById(R.id.pttSizeDown);
			pttSizeDown.setOnClickListener(sizeChangeListener);
			ptt = (Button) findViewById(R.id.ptt);
	        ptt.setOnTouchListener(new OnTouchListener() {
	            public boolean onTouch(View v, MotionEvent event) {
	                if (event.getAction() == MotionEvent.ACTION_DOWN) {
	                    if (pttToggle) {
	                    	toggleOn = !toggleOn;
	                    	s.setPTTOn(toggleOn);
	                    	bottomBar.setBackgroundResource(toggleOn ? R.drawable.blue_gradient_bg : R.drawable.abs__ab_bottom_solid_light_holo);
	                    	return true;
	                    } else {
		                    s.setPTTOn(true);
		                    bottomBar.setBackgroundResource(R.drawable.blue_gradient_bg);
		                    return true;
	                    }
	                } else if (!pttToggle && event.getAction() == MotionEvent.ACTION_UP) {
	                    s.setPTTOn(false);
	                    bottomBar.setBackgroundResource(R.drawable.abs__ab_bottom_solid_light_holo);
	                    return true;
	                }
	                return false;
	            }
	        });
	        
			setPTTSize(prefs.getInt("ptt_size", SMALL));
		} else
			bottomBar.setVisibility(LinearLayout.GONE);
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
			bottomBar.setBackgroundResource(toggleOn ? R.drawable.blue_gradient_bg : R.drawable.abs__ab_bottom_solid_light_holo);
		} else {
			s.setPTTOn(true);
            bottomBar.setBackgroundResource(R.drawable.blue_gradient_bg);
		}
		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (pttEnabled && !pttToggle && keyCode == pttKey) {
			s.setPTTOn(false);
            bottomBar.setBackgroundResource(R.drawable.abs__ab_bottom_solid_light_holo);
            return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU && !sm.isMenuShowing()) {
			sm.showMenu();
			return true;
		}

		return super.onKeyUp(keyCode, event);
	}
	
	private void setPTTSize(int size) {
		int newSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48 * size, getResources().getDisplayMetrics());

		ViewGroup.LayoutParams params = bottomBar.getLayoutParams();
		ViewGroup.LayoutParams btnParams = ptt.getLayoutParams();
		
		Resizer animation = new Resizer(bottomBar, params.width, newSize);
		bottomBar.startAnimation(animation);
		Resizer btnAnimation = new Resizer(ptt, btnParams.width, newSize);
		ptt.startAnimation(btnAnimation);
		
		switch (size) {
		case SMALL:
			pttSizeUp.setVisibility(View.VISIBLE);
			pttSizeDown.setVisibility(View.GONE);
			break;
		case MEDIUM:
			pttSizeUp.setVisibility(View.VISIBLE);
			pttSizeDown.setVisibility(View.VISIBLE);
			break;
		case LARGE:
			pttSizeUp.setVisibility(View.GONE);
			pttSizeDown.setVisibility(View.VISIBLE);
			break;
		}
		
		PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("ptt_size", size).commit();
	}
	
	private OnClickListener sizeChangeListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.pttSizeUp:
				setPTTSize(PreferenceManager.getDefaultSharedPreferences(Connected.this).getInt("ptt_size", SMALL) + 1);
				break;
			case R.id.pttSizeDown:
				setPTTSize(PreferenceManager.getDefaultSharedPreferences(Connected.this).getInt("ptt_size", SMALL) - 1);		
				break;
			}
		}
	};
	
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
	
	public class Resizer extends Animation {
		
	    private View mView;
	    private float mHeight;
	    private float mWidth;

	    public Resizer(View v, float newWidth, float newHeight) {
	        mHeight = newHeight;
	        mWidth = newWidth;
	        mView = v;
	        setDuration(300);
	    }

	    @Override
	    protected void applyTransformation(float interpolatedTime, Transformation t) {
	        ViewGroup.LayoutParams p = mView.getLayoutParams();
	        p.height = (int)((mHeight - p.height) * interpolatedTime + p.height);
	        p.width = (int)((mWidth - p.width) * interpolatedTime + p.width);
	        mView.requestLayout();
	    }
	}

}
