package com.jtxdriggers.android.ventriloid;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

public class ChatView extends Fragment {

	private ExpandableListView list;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		list = (ExpandableListView) inflater.inflate(R.layout.serverview, container, false);
		
		ExpandableListAdapter adapter = new VentriloidListAdapter(
				getActivity(),
				DataList.channeldata,
				R.layout.channel_row,
				new String[] { "indent", "status", "name", "commenttext" },
				new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
				DataList.userdata,
				R.layout.user_row,
				new String[] { "indent", "xmitStatus", "status", "rank", "name", "commenttext", "integration" },
				new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
		
		list.setGroupIndicator(null);
		list.setAdapter(adapter);
		
		return list;
	}
}
