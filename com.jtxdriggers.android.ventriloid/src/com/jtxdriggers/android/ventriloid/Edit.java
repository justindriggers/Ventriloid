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

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class Edit extends Activity {
	
	EditText username, phonetic, servername, hostname, port, password;
	Button save;
	ServerAdapter db;
	int id;
	Server server;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add);
        
		db = new ServerAdapter(this);
        
        id = getIntent().getExtras().getInt("id");
        server = db.getServer(id);
        
        ((TextView) findViewById(R.id.title)).setText("Edit Server");
        
        username = (EditText) findViewById(R.id.username);
        username.setText(server.getUsername());
        phonetic = (EditText) findViewById(R.id.phonetic);
        phonetic.setText(server.getPhonetic());
        servername = (EditText) findViewById(R.id.servername);
        servername.setText(server.getServername());
        hostname = (EditText) findViewById(R.id.hostname);
        hostname.setText(server.getHostname());
        port = (EditText) findViewById(R.id.port);
        port.setText(server.getPort() + "");
        password = (EditText) findViewById(R.id.password);
        password.setText(server.getPassword());
        
    	username.setOnKeyListener(new OnKeyListener(){
    		public boolean onKey(View v, int keyCode, KeyEvent event) {
				phonetic.setText(username.getText());
				return false;
			}
    	});
        
        save = (Button) findViewById(R.id.add);
        save.setText("Save Changes");
        save.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				server.setUsername(username.getText().toString().trim());
				server.setPhonetic(phonetic.getText().toString().trim());
				server.setServername(servername.getText().toString().trim());
				server.setHostname(hostname.getText().toString().trim());
				server.setPort(Integer.parseInt(port.getText().toString().trim()));
				server.setPassword(password.getText().toString().trim());
				
				db.updateServer(server);
				finish();
			}
        });
    }
    
}