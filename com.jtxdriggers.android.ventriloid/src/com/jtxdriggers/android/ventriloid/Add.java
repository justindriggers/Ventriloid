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
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Add extends Activity {
	
	EditText username, phonetic, servername, hostname, port, password;
	Button save;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add);
        
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
    	
    	port.setText("3784");
    	port.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus)
					port.selectAll();
			}
    	});
        
        save = (Button) findViewById(R.id.add);
        save.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if (username.getText().toString().length() == 0 ||
						servername.getText().toString().length() == 0 ||
						hostname.getText().toString().length() == 0 ||
						port.getText().toString().length() == 0) {
					Toast.makeText(Add.this, "Please enter all required information", Toast.LENGTH_SHORT).show();
					return;
				}
				
				Server server = new Server(username.getText().toString().trim(), phonetic.getText().toString().trim(),
						servername.getText().toString().trim(), hostname.getText().toString().trim(),
						Integer.parseInt(port.getText().toString().trim()), password.getText().toString().trim());
				
				ServerAdapter db = new ServerAdapter(Add.this);
				db.addServer(server);
				setResult(RESULT_OK);
				finish();
			}
        });
    }
    
}