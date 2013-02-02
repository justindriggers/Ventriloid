package com.jtxdriggers.android.ventriloid;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.app.Activity;
import org.holoeverywhere.app.Fragment;
import org.holoeverywhere.widget.Button;
import org.holoeverywhere.widget.EditText;
import org.holoeverywhere.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

public class Chat extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);

		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.content_frame, ChatFragment.newInstance((short) 0))
			.commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public static class ChatFragment extends Fragment {
		
		public static final String SERVICE_RECEIVER = "com.jtxdriggers.android.ventriloid.ChatFragment.SERVICE_RECEIVER";
		
		private VentriloidService s;
		private ListView list;
		private ChatListAdapter adapter;
		
		private short id;
		
		public static ChatFragment newInstance(short id) {
			ChatFragment fragment = new ChatFragment();

			Bundle args = new Bundle();
			args.putShort("viewId", id);
			fragment.setArguments(args);
			
			return fragment;
		}
		
	    @Override
	    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	    	id = getArguments().getShort("viewId", (short) 0);
	    	
	    	RelativeLayout layout = (RelativeLayout) LayoutInflater.inflate(getActivity(), R.layout.chat_fragment);
	    	list = (ListView) layout.findViewById(android.R.id.list);
	    	list.setDivider(getResources().getDrawable(R.drawable.abs__list_divider_holo_light));
	    	
	    	final EditText message = (EditText) layout.findViewById(R.id.message);
	    	Button send = (Button) layout.findViewById(R.id.send);
	    	send.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					VentriloInterface.sendchatmessage(message.getText().toString());
					message.setText("");
				}
	    	});
			
			if (getDefaultSharedPreferences().getBoolean("screen_on", false))
				list.setKeepScreenOn(true);
			
	        return layout;
	    }
		
		@Override
		public void onStart() {
			super.onStart();
			getActivity().bindService(new Intent(VentriloidService.SERVICE_INTENT), serviceConnection, Context.BIND_AUTO_CREATE);
		}
		
		@Override
		public void onStop() {
			getActivity().unregisterReceiver(serviceReceiver);
			getActivity().unbindService(serviceConnection);
			super.onStop();
		}
	    
	    private ServiceConnection serviceConnection = new ServiceConnection() {
			public void onServiceConnected(ComponentName className, IBinder binder) {
				s = ((VentriloidService.MyBinder) binder).getService();
				
				getActivity().registerReceiver(serviceReceiver, new IntentFilter(SERVICE_RECEIVER));

		    	adapter = new ChatListAdapter(getActivity(), s.getItemData().getChat(id));
		    	list.setAdapter(adapter);
			}

			public void onServiceDisconnected(ComponentName className) {
				s = null;
			}
		};
		
		private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				adapter.notifyDataSetChanged();
			}
		};
	}

}
