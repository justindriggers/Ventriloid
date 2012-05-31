package com.jtxdriggers.android.ventriloid;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class Main extends Activity {
	
	private Spinner spinner;
	private Button connect, manage, settings;
	private ServerAdapter db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        spinner = (Spinner) findViewById(R.id.servers);
        connect = (Button) findViewById(R.id.connect);
        manage = (Button) findViewById(R.id.manage);
        settings = (Button) findViewById(R.id.settings);
        
        manage.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Main.this, Manage.class));
			}
        });
        
        db = new ServerAdapter(this);
        loadServers();
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	loadServers();
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
    
}