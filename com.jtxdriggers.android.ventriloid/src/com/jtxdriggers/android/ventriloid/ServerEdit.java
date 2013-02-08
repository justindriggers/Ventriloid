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

import org.holoeverywhere.app.Activity;
import org.holoeverywhere.widget.EditText;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;

public class ServerEdit extends Activity {
	
	private EditText username, phonetic, servername, hostname, port, password;
	private ServerAdapter db;
	private Server server;
	private boolean editMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.server_edit);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		db = new ServerAdapter(this);
		
		username = (EditText) findViewById(R.id.username);
		phonetic = (EditText) findViewById(R.id.phonetic);
		servername = (EditText) findViewById(R.id.servername);
		hostname = (EditText) findViewById(R.id.hostname);
		port = (EditText) findViewById(R.id.port);
		password = (EditText) findViewById(R.id.password);
		
        username.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                            phonetic.setText(username.getText());
                            return false;
                    }
	    });
	    
	    phonetic.setOnFocusChangeListener(new OnFocusChangeListener() {
	            public void onFocusChange(View v, boolean hasFocus) {
	                    if (hasFocus)
	                            phonetic.selectAll();
	            }
	    });
	    
	    port.setText("3784");
	    port.setOnFocusChangeListener(new OnFocusChangeListener() {
	                    public void onFocusChange(View v, boolean hasFocus) {
	                            if (hasFocus)
	                                    port.selectAll();
	                    }
	    });
		
		int serverIndex;
		if (editMode = (serverIndex = getIntent().getIntExtra("index", -1)) >= 0) {
			getSupportActionBar().setTitle("Edit Server");
			server = db.getAllServers().get(serverIndex);
			username.setText(server.getUsername());
			phonetic.setText(server.getPhonetic());
			servername.setText(server.getServername());
			hostname.setText(server.getHostname());
			port.setText(server.getPort() + "");
			password.setText(server.getPassword());
		} else
			server = new Server();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.server_edit, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.save:
			if (username.getText().toString().length() == 0 ||
            		servername.getText().toString().length() == 0 ||
            		hostname.getText().toString().length() == 0 ||
            		port.getText().toString().length() == 0) {
				new AlertDialog.Builder(this)
					.setTitle("Please enter all required information")
					.setPositiveButton("OK", null)
					.show();
				return true;
			}
			
			server.setUsername(username.getText().toString());
			server.setPhonetic(phonetic.getText().toString());
			server.setServername(servername.getText().toString());
			server.setHostname(hostname.getText().toString());
			server.setPort(Integer.parseInt(port.getText().toString()));
			server.setPassword(password.getText().toString());
			if (editMode)
				db.updateServer(server);
			else
				db.addServer(server);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
