package com.jtxdriggers.android.ventriloid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;

public class ServerView extends Fragment {

	private EditText input;
	private ExpandableListView list;
	private VentriloidListAdapter adapter;
	private VentriloidService s;
	private boolean processEvents = false;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		
		list = (ExpandableListView) inflater.inflate(R.layout.serverview, container, false);
		list.setGroupIndicator(null);
		list.setOnGroupClickListener(onChannelClick);

		adapter = new VentriloidListAdapter(
			getActivity(),
			s.getItemData().getChannels(),
			R.layout.channel_row,
			new String[] { "indent", "status", "name", "comment" },
			new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
			s.getItemData().getUsers(),
			R.layout.user_row,
			new String[] { "indent", "xmit", "status", "rank", "name", "comment", "integration" },
			new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
	
		list.setAdapter(adapter);
		adapter.notifyDataSetChanged();

		for (int i = 0; i < adapter.getGroupCount(); i++) {
			list.expandGroup(i);
		}
		
		processEvents = true;
		
		return list;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		s = ((ViewPagerActivity) activity).s;
	}
	
	public void process(Intent intent) {
		if (processEvents) {
		//Bundle e = intent.getExtras();
		//int type = e.getInt("type");
			adapter.notifyDataSetChanged();
		}
	}
	 
	public void updateList() {
	}
	
	private OnGroupClickListener onChannelClick = new OnGroupClickListener() {
		public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
			final Item.Channel c = s.getItemData().getChannels().get(groupPosition);
			if (c.reqPassword) {
				Builder passwordDialog = new AlertDialog.Builder(getActivity());
			    input = new EditText(getActivity());
			    input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
			    input.setTransformationMethod(PasswordTransformationMethod.getInstance());
				passwordDialog.setTitle("Enter Channel Password: ")
				.setView(input)
				.setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						VentriloInterface.changechannel(c.id, input.getText().toString());
						return;
					}
				})
				.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				passwordDialog.show();
			} else
				VentriloInterface.changechannel(c.id, "");
			return true;
		}
	};
}
