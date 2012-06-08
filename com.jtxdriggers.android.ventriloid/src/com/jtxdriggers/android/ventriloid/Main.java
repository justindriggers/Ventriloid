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

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class Main extends Activity {
	
	public static String RECEIVER = "com.jtxdriggers.android.ventriloid.MAIN_RECEIVER";
	
	private Spinner spinner;
	private Button connect, manage, settings;
	private ServerAdapter db;
	private ProgressDialog dialog;
	
	private Intent serviceIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        spinner = (Spinner) findViewById(R.id.servers);
        connect = (Button) findViewById(R.id.connect);
        manage = (Button) findViewById(R.id.manage);
        settings = (Button) findViewById(R.id.settings);
        
        connect.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		dialog = new ProgressDialog(Main.this);
				dialog.setMessage("Connecting. Please wait...");
				dialog.setCancelable(true);
				dialog.setOnCancelListener(new OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						stopService(serviceIntent);
					}
				});
				dialog.show();
				
        		serviceIntent = new Intent(VentriloidService.SERVICE_INTENT).putExtra("id", getCurrentItemID(spinner));
				startService(serviceIntent);
        	}
        });
        
        manage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Main.this, Manage.class));
			}
        });
        
        settings.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		return;
        	}
        });
        
        db = new ServerAdapter(this);
        loadServers();
        
        registerReceiver(receiver, new IntentFilter(RECEIVER));
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	loadServers();
    }
    
    @Override
    public void onDestroy() {
    	unregisterReceiver(receiver);
    	super.onDestroy();
    }
    
    private void loadServers() {
    	ArrayList<String> servers = db.getAllServersAsStrings();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, servers);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
    	
    	boolean isEnabled = servers.size() > 0;
		spinner.setEnabled(isEnabled);
		connect.setEnabled(isEnabled);
    }
    
    private int getCurrentItemID(Spinner s) {
    	ArrayList<Server> servers = db.getAllServers();
    	String currentItem = s.getSelectedItem().toString();
    	String[] split1 = currentItem.split("\\@");
    	String[] split2 = split1[1].split("\\:");
    	
    	String username = split1[0];
    	String servername = split2[0];
    	String hostname = split2[1].trim();
    	int port = Integer.parseInt(split2[2]);
    	
    	for (int i = 0; i < servers.size(); i++) {
    		Server current = servers.get(i);
    		if (username.equals(current.getUsername())
    			&& servername.equals(current.getServername())
    			&& hostname.equals(current.getHostname())
    			&& port == current.getPort()) {
    				return current.getId();
    		}
    	}
    	
    	return -1;
    }
    
    private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			int type = intent.getExtras().getInt("type");
			switch (type) {
			case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
				dialog.dismiss();
				startActivity(new Intent(Main.this, ViewPagerActivity.class));
			}
		}
    };
    
}