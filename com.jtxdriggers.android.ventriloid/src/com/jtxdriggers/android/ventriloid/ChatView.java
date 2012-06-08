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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

public class ChatView extends Fragment {

	private ExpandableListView list;
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null)
            return null;
		list = (ExpandableListView) inflater.inflate(R.layout.serverview, container, false);
		
		/*ExpandableListAdapter adapter = new VentriloidListAdapter(
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
		list.setAdapter(adapter);*/
		
		return list;
	}
}
