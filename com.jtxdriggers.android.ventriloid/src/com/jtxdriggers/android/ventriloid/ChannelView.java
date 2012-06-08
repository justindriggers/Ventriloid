/*
 * Copyright 2012 Justin Driggers <jtxdriggers@gmail.com>
 *
 * This file is part of Ventriloid.
 *
 * Ventriloid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ventriloid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ventriloid.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jtxdriggers.android.ventriloid;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;

public class ChannelView extends Fragment {

	private ExpandableListView list;
	private VentriloidListAdapter adapter;
	private VentriloidService s;
	private boolean adapterReady = false;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		
		list = (ExpandableListView) inflater.inflate(R.layout.serverview, container, false);
		list.setGroupIndicator(null);
		list.setOnGroupClickListener(new OnGroupClickListener() {
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				return true;
			}
		});

		adapter = new VentriloidListAdapter(
			getActivity(),
			s,
			VentriloidListAdapter.CHANNEL_VIEW,
			s.getItemData().getCurrentChannel(),
			R.layout.channel_row,
			new String[] { "indent", "status", "name", "comment" },
			new int[] { R.id.crowindent, R.id.crowstatus, R.id.crowtext, R.id.crowcomment },
			s.getItemData().getCurrentUsers(),
			R.layout.user_row,
			new String[] { "indent", "xmit", "status", "rank", "name", "comment", "integration" },
			new int[] { R.id.urowindent, R.id.IsTalking, R.id.urowstatus, R.id.urowrank, R.id.urowtext, R.id.urowcomment, R.id.urowint });
	
		list.setAdapter(adapter);
		adapter.notifyDataSetChanged();

		for (int i = 0; i < adapter.getGroupCount(); i++) {
			list.expandGroup(i);
		}
		
		adapterReady = true;
		
		return list;
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		s = ((ViewPagerActivity) activity).s;
	}
	
	public void process() {		
		if (adapterReady)
			adapter.update();
	}
}
