package com.jtxdriggers.android.ventriloid;

import org.holoeverywhere.ArrayAdapter;
import org.holoeverywhere.app.AlertDialog;
import org.holoeverywhere.app.ListActivity;
import org.holoeverywhere.widget.ListView;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;

public class Manage extends ListActivity implements ActionMode.Callback {
	
	public static final int REQUEST_CODE_MANAGE = 1;
	
	private ServerAdapter db;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.manage);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		db = new ServerAdapter(this);

		getListView().setLongClickable(true);
	    getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
	    getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (db.getServersCount() > 0) {
					getListView().clearChoices();
					getListView().setItemChecked(position, true);
					startActionMode(Manage.this);
					return true;
				}
				return false;
			}
	    });
		
	}
    
    @Override
    public void onStart() {
        super.onStart();
        loadServers();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.manage, menu);
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (menu.findItem(R.id.clear) != null) 
			menu.findItem(R.id.clear).setVisible(db.getServersCount() > 0);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.add:
			startActivity(new Intent(this, ServerEdit.class));
			return true;
		case R.id.clear:
			new AlertDialog.Builder(this)
				.setTitle("Clear all server data?")
				.setMessage("This action cannot be undone.")
				.setNegativeButton("Cancel", null)
				.setPositiveButton("Clear", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						db.clearServers();
						loadServers();
					}
				}).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (db.getServersCount() > 0)
			startActivity(new Intent(this, ServerEdit.class).putExtra("index", position));
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.manage_action, menu);
        mode.setTitle(db.getAllServersAsStrings().get(getListView().getCheckedItemPosition()));
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		return false;
	}

	@Override
	public boolean onActionItemClicked(final ActionMode mode, MenuItem item) {
		final int position = getListView().getCheckedItemPosition();
		switch (item.getItemId()) {
		case R.id.edit:
			startActivity(new Intent(this, ServerEdit.class).putExtra("index", position));
	        mode.finish();
			return true;
		case R.id.remove:
			new AlertDialog.Builder(this)
			.setTitle("Remove server?")
			.setMessage("This action cannot be undone.")
			.setNegativeButton("Cancel", null)
			.setPositiveButton("Remove", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Server s = db.getAllServers().get(position);
					if (s.getId() == getDefaultSharedPreferences().getInt("default", -1))
						getDefaultSharedPreferences().edit().remove("default").commit();
					db.deleteServer(db.getAllServers().get(position));
					loadServers();
			        mode.finish();
				}
			}).show();
			return true;
		case R.id.copy:
			Server server = db.getAllServers().get(position);
			server.setServername(server.getServername() + " - Copy");
			db.addServer(server);
			loadServers();
	        mode.finish();
			return true;
		}
		return false;
	}

	@Override
	public void onDestroyActionMode(ActionMode mode) { }
	
	private void loadServers() {
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.simple_list_item_1,
				android.R.id.text1, db.getAllServersAsStrings());
		setListAdapter(adapter);
		
    	invalidateOptionsMenu();
	}

}
