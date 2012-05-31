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
        
    	username.setOnKeyListener(new OnKeyListener(){
    		public boolean onKey(View v, int keyCode, KeyEvent event) {
				phonetic.setText(username.getText());
				return false;
			}
    	});
        
        save = (Button) findViewById(R.id.add);
        save.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
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