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

public class Manage extends Activity {
	
	private Spinner spinner;
	private Button delete, edit, add, reset;
	private ServerAdapter db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage);
        
        spinner = (Spinner) findViewById(R.id.servers);
        delete = (Button) findViewById(R.id.delete);
        edit = (Button) findViewById(R.id.edit);
        add = (Button) findViewById(R.id.add);
        reset = (Button) findViewById(R.id.reset);
        
        db = new ServerAdapter(this);
        
        delete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				db.deleteServer(db.getServer(getCurrentItemID(spinner)));
				loadServers();
			}
        });
        
        edit.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
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
        		db.clearServers();
        		loadServers();
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
        finish();
      }
    }
    
    private void loadServers() {
    	ArrayList<String> servers = db.getAllServersAsStrings();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, servers);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
    	
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