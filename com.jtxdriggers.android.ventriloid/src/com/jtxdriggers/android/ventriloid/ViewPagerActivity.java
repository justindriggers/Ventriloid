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

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.app.AlertDialog;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TabHost;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class ViewPagerActivity extends FragmentActivity {
	
	public static final String ACTIVITY_RECEIVER = "com.jtxdriggers.android.ventriloid.ACTIVITY_RECEIVER";
	public static final String FRAGMENT_RECEIVER = "com.jtxdriggers.android.ventriloid.FRAGMENT_RECEIVER";

	private TabHost mTabHost;
	private ViewPager mViewPager;
	private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, ViewPagerActivity.TabInfo>();
	private FragmentPagerAdapter mPagerAdapter;
	private List<Fragment> fragments;
	private int position = 0;
	private SharedPreferences prefs, volumePrefs;
	private boolean pttEnabled, toggle, toggleOn = false;
	private int pttKey;
	
	private ServerView sv = new ServerView();
	private ChannelView cv = new ChannelView();
	private ChatView chat = new ChatView();

	public VentriloidService s;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpager);
		
		if (savedInstanceState != null)
			position = savedInstanceState.getInt("tab");
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (pttEnabled = prefs.getBoolean("custom_ptt", false)) {
			toggle = prefs.getBoolean("toggle_mode", false);
			pttKey = prefs.getInt("ptt_key", KeyEvent.KEYCODE_CAMERA);
		}
		
		initialiseTabHost(savedInstanceState);
		intialiseViewPager();
		
		setTitle("Checking latency...");
	}

	@Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
    	savedInstanceState.putInt("tab", mViewPager.getCurrentItem());
        super.onSaveInstanceState(savedInstanceState);
    }
	
	@Override
	public void onStart() {
		super.onStart();
		bindService(new Intent(VentriloidService.SERVICE_INTENT), mConnection, Context.BIND_AUTO_CREATE);
	}
    
	@Override
    public void onStop() {
    	unregisterReceiver(receiver);
    	if (isFinishing())
    		s.stopSelf();
    	unbindService(mConnection);
    	super.onStop();
    }
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!pttEnabled || s == null || keyCode != pttKey)
			return super.onKeyDown(keyCode, event);
		
		if (toggle) {
			if (toggleOn) {
				toggleOn = false;
				s.setPTTOn(false);
			} else {
				toggleOn = true;
				s.setPTTOn(true);
			}
		} else
			s.setPTTOn(true);
		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (!pttEnabled || s == null || keyCode != pttKey)
			return super.onKeyUp(keyCode, event);
		
		if (!toggle)
			s.setPTTOn(false);
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	SubMenu options = menu.addSubMenu(Menu.NONE, ContextMenuItems.USER_OPTIONS, Menu.NONE, "User Options").setIcon(android.R.drawable.ic_menu_manage);
		options.add(Menu.NONE, ContextMenuItems.ADMIN_LOGOUT, Menu.NONE, "Admin Logout").setVisible(!s.isAdmin());
		options.add(Menu.NONE, ContextMenuItems.ADMIN_LOGIN, Menu.NONE, "Admin Login").setVisible(s.isAdmin());
		options.add(Menu.NONE, ContextMenuItems.SET_VOLUME, Menu.NONE, "Set Transmit Level");
		options.add(Menu.NONE, ContextMenuItems.SET_COMMENT, Menu.NONE, "Set Comment");
		options.add(Menu.NONE, ContextMenuItems.SET_URL, Menu.NONE, "Set URL");
    	menu.add(Menu.NONE, ContextMenuItems.DISCONNECT, Menu.NONE, "Disconnect").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    	return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(ContextMenuItems.USER_OPTIONS).getSubMenu().findItem(ContextMenuItems.ADMIN_LOGOUT).setVisible(s.isAdmin());
		menu.findItem(ContextMenuItems.USER_OPTIONS).getSubMenu().findItem(ContextMenuItems.ADMIN_LOGIN).setVisible(!s.isAdmin());
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
	
		LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		
		final EditText input = new EditText(this);
	    InputFilter[] FilterArray = new InputFilter[1];
	    FilterArray[0] = new InputFilter.LengthFilter(127);
	    input.setFilters(FilterArray);
		layout.addView(input);
		
		final CheckBox silent = new CheckBox(this);
		silent.setChecked(true);
		silent.setText(" Send Silently ");
		
		LinearLayout frame = new LinearLayout(this);
		frame.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		frame.setGravity(Gravity.CENTER);
		
		final Item.User u = s.getItemData().getUserById(VentriloInterface.getuserid());
			
		switch (item.getItemId()) {
		case ContextMenuItems.SET_VOLUME:
			final TextView percent = new TextView(this);
			final SeekBar volume = new SeekBar(this);
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
			LinearLayout volumeLayout = new LinearLayout(this);
			volumeLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, Gravity.CENTER));
			volumeLayout.setOrientation(LinearLayout.VERTICAL);
			volumeLayout.addView(volume);
			frame.addView(percent);
			volumeLayout.addView(frame);
			dialog.setView(volumeLayout);
			volume.setProgress(u.volume);
			percent.setText((u.volume * 200) / volume.getMax() + "%");
			dialog.setTitle("Set Transmit Volume:");
			dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.setxmitvolume(volume.getProgress());
					u.volume = volume.getProgress();
					u.updateStatus();
					sendBroadcast(new Intent(ViewPagerActivity.FRAGMENT_RECEIVER));
					volumePrefs.edit().putInt("transmit", volume.getProgress()).commit();
				}
			});
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
			dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.settext(input.getText().toString(), u.url, "", silent.isChecked());
				}
			});
			dialog.setNegativeButton("Cancel", null);
			input.setText(u.comment);
			dialog.show();
			return true;
		case ContextMenuItems.SET_URL:
			dialog.setTitle("Set URL:");
			frame.addView(silent);
			layout.addView(frame);
			dialog.setView(layout);
			input.setText(u.url.length() > 0 ? u.url : "http://");
			input.setSingleLine();
			dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					VentriloInterface.settext(u.comment, input.getText().toString(), "", silent.isChecked());
				}
			});
			dialog.setNegativeButton("Cancel", null);
			dialog.show();
			return true;
		case ContextMenuItems.ADMIN_LOGIN:
			dialog.setTitle("Enter Admin Password:");
			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
		    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
			dialog.setView(layout);
			dialog.setPositiveButton("Login", new DialogInterface.OnClickListener() {
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
		case ContextMenuItems.DISCONNECT:
			finish();
			return true;
		default:
			return false;
		}
	}
    
    @SuppressWarnings("unused")
	private class TabInfo {
		private String tag;
        private Class<?> clss;
		private Bundle args;
        private Fragment fragment;
        
        TabInfo(String tag, Class<?> clss, Bundle args) {
        	this.tag = tag;
        	this.clss = clss;
        	this.args = args;
        }
	}

	private class TabFactory implements TabContentFactory {
		private final Context mContext;

	    public TabFactory(Context context) {
	        mContext = context;
	    }

	    public View createTabContent(String tag) {
	        View v = new View(mContext);
	        v.setMinimumWidth(0);
	        v.setMinimumHeight(0);
	        return v;
	    }
	}

    private void intialiseViewPager() {
		fragments = new Vector<Fragment>();
		fragments.add(sv);
		fragments.add(cv);
		fragments.add(chat);
		mPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
			@Override
			public Fragment getItem(int position) {
				return fragments.get(position);
			}

			@Override
			public int getCount() {
				return fragments.size();
			}
		};

		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			public void onPageScrollStateChanged(int state) { }
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
			public void onPageSelected(int position) {
				mTabHost.setCurrentTabByTag("Tab" + position);
				ViewPagerActivity.this.position = position;
			}
		});
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setCurrentItem(position);
		if (prefs.getBoolean("screen_on", false))
			mViewPager.setKeepScreenOn(true);
    }
    
	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        
        ImageView divider1 = new ImageView(this);
        divider1.setImageResource(R.drawable.tab_separator);
        ImageView divider2 = new ImageView(this);
        divider2.setImageResource(R.drawable.tab_separator);
        
        TabInfo tabInfo = null;
        final View[] tabs = new View[3];
        for (int i = 0; i < tabs.length; i++) {
        	tabs[i] = getLayoutInflater().inflate(R.layout.tab_indicator, null);
        }
        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (scale + 0.5f);
        
        ((TextView)tabs[0].findViewById(android.R.id.text1)).setText("Server");
        addTab(mTabHost.newTabSpec("Tab0").setIndicator(tabs[0]), (tabInfo = new TabInfo("Tab0", ServerView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);
        mTabHost.getTabWidget().addView(divider1, pixels, LayoutParams.FILL_PARENT);
        ((TextView)tabs[1].findViewById(android.R.id.text1)).setText("Channel");
        addTab(mTabHost.newTabSpec("Tab1").setIndicator(tabs[1]), (tabInfo = new TabInfo("Tab1", ChannelView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);
        mTabHost.getTabWidget().addView(divider2, pixels, LayoutParams.FILL_PARENT);
        ((TextView)tabs[2].findViewById(android.R.id.text1)).setText("Chat");
        addTab(mTabHost.newTabSpec("Tab2").setIndicator(tabs[2]), (tabInfo = new TabInfo("Tab2", ChatView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);

        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				for (int i = 0; i < 3; i++) {
					if (tabId.equals("Tab" + i)) {
						position = i;
						mViewPager.setCurrentItem(i);
						tabs[i].findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.blue));
						((TextView) tabs[i].findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.white));
					} else {
						tabs[i].findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.black));
						((TextView) tabs[i].findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.gray));
					}
				}
			}
        });
        
		mTabHost.getCurrentTabView().findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.blue));
		((TextView) mTabHost.getCurrentTabView().findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.white));
	}

	private void addTab(TabSpec tabSpec, final TabInfo tabInfo) {
		tabSpec.setContent(new TabFactory(this));
        mTabHost.addTab(tabSpec);
        mTabHost.getTabWidget().getChildTabViewAt(mTabHost.getTabWidget().getTabCount() - 1).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mTabHost.setCurrentTabByTag(tabInfo.tag);
			}
        });
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			registerReceiver(receiver, new IntentFilter(ACTIVITY_RECEIVER));
			
			volumePrefs = getSharedPreferences("VOLUMES" + s.getServerId(), Context.MODE_PRIVATE);
			
			s.processAll();
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
			case VentriloEvents.V3_EVENT_DISCONNECT:
				finish();
				break;
			case VentriloEvents.V3_EVENT_PING:
				setPing(intent.getIntExtra("ping", -1));
				break;
			default:
				s.updateViews();
			}
		}
	};
	
	private void setPing(int ping) {
		if (ping < 65535 && ping > 0)
			setTitle("Ping: " + ping + "ms");
		else
			setTitle("Checking latency...");
	}
}
