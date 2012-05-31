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
				
			}
        });
        
        add.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Manage.this, Add.class));
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
    
    private void loadServers() {
    	ArrayList<String> servers = db.getAllServers();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, servers);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
    	
    	boolean isEnabled = servers.size() > 0;
		spinner.setEnabled(isEnabled);
		delete.setEnabled(isEnabled);
		edit.setEnabled(isEnabled);
		reset.setEnabled(isEnabled);
    }
    
}