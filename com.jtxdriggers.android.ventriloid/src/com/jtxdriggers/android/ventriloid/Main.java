/*
 * Copyright 2013 Justin Driggers <jtxdriggers@gmail.com>
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

import java.util.ArrayList;

import android.widget.ImageButton;
import org.holoeverywhere.ArrayAdapter;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.ProgressDialog;
import org.holoeverywhere.widget.LinearLayout;
import org.holoeverywhere.widget.TextView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.View;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class Main extends Activity {
	
	public static final String SERVICE_RECEIVER = "com.jtxdriggers.android.ventriloid.Main.SERVICE_RECEIVER";

	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	
	private ServerAdapter db;
	private ActionBar ab;
	private ProgressDialog dialog;
	private Intent serviceIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		if (VentriloidService.isConnected()) {
			startActivity(new Intent(this, Connected.class));
			finish();
		}
		
		db = new ServerAdapter(this);
		
		ab = getSupportActionBar();
		ab.setDisplayShowTitleEnabled(false);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		
		registerReceiver(serviceReceiver, new IntentFilter(SERVICE_RECEIVER));

        findViewById(R.id.aboutButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder about = new AlertDialog.Builder(Main.this);
                View layout = getLayoutInflater().inflate(R.layout.about);
                Linkify.addLinks((TextView) layout.findViewById(R.id.ventriloidSource), Linkify.ALL);
                Linkify.addLinks((TextView) layout.findViewById(R.id.manglerSource), Linkify.ALL);
                Linkify.addLinks((TextView) layout.findViewById(R.id.absSource), Linkify.ALL);
                Linkify.addLinks((TextView) layout.findViewById(R.id.holoSource), Linkify.ALL);
                Linkify.addLinks((TextView) layout.findViewById(R.id.smSource), Linkify.ALL);
                about.setView(layout);
                about.setNeutralButton("Donate", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=N95UGEQ6FAKPN")));
                    }
                });
                about.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                about.show();
            }
        });

        findViewById(R.id.settingsButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Main.this, Settings.class));
            }
        });
		
		if (getDefaultSharedPreferences().getBoolean("v3FirstRun", true)) {
			AlertDialog.Builder firstRun = new AlertDialog.Builder(this);
			firstRun.setView((LinearLayout) getLayoutInflater().inflate(R.layout.first_run));
			firstRun.setNegativeButton("Close", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			firstRun.setPositiveButton("Add Server", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(Main.this, ServerEdit.class));
				}
			});
			firstRun.show();
			getDefaultSharedPreferences().edit().putBoolean("v3FirstRun", false).commit();
		}
	}
    
    @Override
    public void onStart() {
        super.onStart();
        loadServers();
    }
    
    @Override
    public void onDestroy() {
    	unregisterReceiver(serviceReceiver);
    	super.onDestroy();
    }

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getSupportActionBar().getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.connect).setEnabled(db.getServersCount() > 0);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.connect:
    		dialog = new ProgressDialog(Main.this);
			dialog.setMessage("Connecting. Please wait...");
			dialog.setCancelable(true);
			dialog.setOnCancelListener(new OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					stopService(serviceIntent);
				}
			});
			dialog.show();
			
			int serverId = db.getAllServers().get(getSupportActionBar().getSelectedNavigationIndex()).getId();
			getDefaultSharedPreferences().edit().putInt("default", serverId).commit();
			
    		serviceIntent = new Intent(VentriloidService.SERVICE_INTENT).putExtra("id", serverId);
			startService(serviceIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void loadServers() {
        ArrayList<String> serverList = db.getAllServersAsStrings();
        serverList.add("Manage Servers...");

		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(ab.getThemedContext(), R.layout.spinner_item,
			android.R.id.text1, serverList);
		adapter.setDropDownViewResource(R.layout.server_selection_item);
		ab.setListNavigationCallbacks(adapter, new ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                if (itemPosition == adapter.getCount() - 1) {
                    startActivity(new Intent(Main.this, Manage.class));
                    return true;
                }
                return false;
            }
        });

		ArrayList<Server> servers = db.getAllServers();
		for (int i = 0; i < servers.size(); i++) {
			if (servers.get(i).getId() == getDefaultSharedPreferences().getInt("default", -1)) {
				ab.setSelectedNavigationItem(i);
				break;
			}
		}

    	supportInvalidateOptionsMenu();
	}
    
    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getShortExtra("type", (short) 0)) {
			case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
				if (dialog != null && dialog.isShowing())
					dialog.dismiss();
				startActivity(new Intent(Main.this, Connected.class));
				finish();
				break;
			case VentriloEvents.V3_EVENT_LOGIN_FAIL:
			case VentriloEvents.V3_EVENT_ERROR_MSG:
				if (dialog != null && dialog.isShowing())
					dialog.dismiss();
				break;
			case -1:
		    	int timer = intent.getIntExtra("timer", -1);
				if (dialog == null || !dialog.isShowing()) {
		    		dialog = new ProgressDialog(Main.this);
					dialog.setMessage(timer > 0 ? "Reconnecting in " + timer + "..." : "Reconnecting...");
					dialog.setCancelable(true);
					dialog.setOnCancelListener(new OnCancelListener() {
						public void onCancel(DialogInterface dialog) {
							sendBroadcast(new Intent(VentriloidService.ACTIVITY_RECEIVER).putExtra("type", VentriloEvents.V3_EVENT_DISCONNECT));
						}
					});
					dialog.show();
				} else {
					dialog.setMessage(timer > 0 ? "Reconnecting in " + timer + "..." : "Reconnecting...");
				}
				break;
			}
		}
    };

}
