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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class Manage extends Activity {
	
	private Spinner spinner;
	private Button delete, edit, add, reset;
	private ServerAdapter db;
	private SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage);
        
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        spinner = (Spinner) findViewById(R.id.servers);
        delete = (Button) findViewById(R.id.delete);
        edit = (Button) findViewById(R.id.edit);
        add = (Button) findViewById(R.id.add);
        reset = (Button) findViewById(R.id.reset);
        
        db = new ServerAdapter(this);
        
        delete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (spinner.getSelectedItemPosition() == prefs.getInt("server", 0))
					prefs.edit().remove("server").commit();
				db.deleteServer(db.getServer(getCurrentItemID(spinner)));
				loadServers();
        		Toast.makeText(Manage.this, "Server removed", Toast.LENGTH_SHORT).show();
			}
        });
        
        edit.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		prefs.edit().putInt("server", spinner.getSelectedItemPosition()).commit();
        		startActivity(new Intent(Manage.this, Edit.class).putExtra("id", getCurrentItemID(spinner)));
        	}
        });
        
        add.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivityForResult(new Intent(Manage.this, Add.class), 1);
			}
        });
        
        reset.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		prefs.edit().remove("server").commit();
        		db.clearServers();
        		loadServers();
        		Toast.makeText(Manage.this, "All servers have been cleared", Toast.LENGTH_SHORT).show();
        	}
        });
        
        loadServers();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	loadServers();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode == RESULT_OK && requestCode == 1) {
    		if (prefs.edit().putInt("server", db.getServersCount() - 1).commit())
    			finish();
    	}
    }
    
    private void loadServers() {
    	ArrayList<String> servers = db.getAllServersAsStrings();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, servers);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
		spinner.setSelection(prefs.getInt("server", 0));
    	
    	boolean isEnabled = servers.size() > 0;
		spinner.setEnabled(isEnabled);
		delete.setEnabled(isEnabled);
		edit.setEnabled(isEnabled);
		reset.setEnabled(isEnabled);
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
    
}