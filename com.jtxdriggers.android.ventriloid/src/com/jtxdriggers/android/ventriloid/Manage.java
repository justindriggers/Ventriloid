package com.jtxdriggers.android.ventriloid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Spinner;

public class Manage extends Activity {
	
	private Spinner servers;
	private Button delete, edit, add, reset;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.manage);
        
        servers = (Spinner) findViewById(R.id.servers);
        delete = (Button) findViewById(R.id.delete);
        edit = (Button) findViewById(R.id.edit);
        add = (Button) findViewById(R.id.add);
        reset = (Button) findViewById(R.id.reset);
        
        add.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(Manage.this, Add.class));
			}
        });
    }
    
}