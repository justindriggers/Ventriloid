package com.jtxdriggers.android.ventriloid;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

public class ServerView extends Fragment {

	private ProgressDialog dialog;
	private ExpandableListView list;
	private VentriloidListAdapter adapter;
	private VentriloidService s;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		
		list = (ExpandableListView) inflater.inflate(R.layout.serverview, container, false);
		
		dialog = new ProgressDialog(getActivity());
		dialog.setMessage("Loading data...");
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				getActivity().finish();
			}
		});
		dialog.show();
		
		return list;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		activity.getApplicationContext()
			.bindService(new Intent(VentriloidService.SERVICE_INTENT), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onDetach() {
		getActivity().getApplicationContext()
			.unbindService(mConnection);
		super.onDetach();
	}
    
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder binder) {
			s = ((VentriloidService.MyBinder) binder).getService();
			
			adapter = new VentriloidListAdapter(
					getActivity(),
					s.getChannels(),
					R.layout.channel_row,
					new String[] { "indent", "status", "name", "comment" },
					new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
					s.getUsers(),
					R.layout.user_row,
					new String[] { "indent", "xmit", "status", "rank", "name", "comment", "integration" },
					new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
			
			list.setGroupIndicator(null);
			list.setAdapter(adapter);
			updateList();
			dialog.dismiss();
		}

		public void onServiceDisconnected(ComponentName className) {
			s = null;
		}
	};
	 
	public void updateList() {
		adapter.notifyDataSetChanged();
		for (int i = 0; i < adapter.getGroupCount(); i++) {
			list.expandGroup(i);
		}
	}
}
