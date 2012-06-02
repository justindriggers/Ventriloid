/*
 * Copyright 2010 Justin Driggers <jtxdriggers@gmail.com>
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

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class VentriloidListAdapter extends SimpleExpandableListAdapter {
	 private List<? extends List<? extends Map<String, ?>>> mChildData;
	 private String[] mChildFrom;
	 private int[] mChildTo;

	 public VentriloidListAdapter(Context context, List<? extends Map<String, ?>> groupData,
			int groupLayout, String[] groupFrom, int[] groupTo,
			List<? extends List<? extends Map<String, ?>>> childData,
			int childLayout, String[] childFrom, int[] childTo) {
		 super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);

	     mChildData = childData;
	     mChildFrom = childFrom;
	     mChildTo = childTo;
	 }

	 public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		 View v;
		 
	     if (convertView == null) {
	    	 v = newChildView(isLastChild, parent);
	     } else {
	         v = convertView;
	     }
	     
	     bindView(v, mChildData.get(groupPosition).get(childPosition), mChildFrom, mChildTo, groupPosition, childPosition);
	     return v;
	 }

	 private void bindView(View view, Map<String, ?> data, String[] from, int[] to, int groupPosition, int childPosition) {
		 int len = to.length - 1;

		 for (int i = 0; i < len; i++) {
	    	 if (i != 1) {
	    		 TextView v = (TextView) view.findViewById(to[i]);
	    		 if (v != null) {
	    			 v.setText(data.get(from[i]).toString());
	    		 }
	    	 } else {
	    	     ImageView imgV = (ImageView) view.findViewById(to[1]);
	    	     if (imgV != null) {
	    	    	 int indicator = R.drawable.user_status_inactive;
	    	    	 if (data.get(from[1]).equals(Integer.toString(VentriloidListItem.PLAYER_ON)))
	    	    		 indicator = R.drawable.user_status_active;
	    	    	 else if (data.get(from[1]).equals(Integer.toString(VentriloidListItem.PLAYER_INIT)))
	    	    		 indicator = R.drawable.user_status_other;
	    	    	 imgV.setImageResource(indicator);
	    	     } 
	    	 }
	     }
	 }

}
