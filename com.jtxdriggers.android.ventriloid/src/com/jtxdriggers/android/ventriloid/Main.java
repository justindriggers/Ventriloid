package com.jtxdriggers.android.ventriloid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Spinner;

public class Main extends Activity {
	
	private Spinner servers;
	private Button connect, manage, settings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        servers = (Spinner) findViewById(R.id.servers);
        connect = (Button) findViewById(R.id.connect);
        manage = (Button) findViewById(R.id.manage);
        settings = (Button) findViewById(R.id.settings);
        
        manage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Main.this, Manage.class));
			}
        });
    }
    
}