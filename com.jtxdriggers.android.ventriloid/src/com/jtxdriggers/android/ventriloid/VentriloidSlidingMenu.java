/*
 * Copyright 2013 Justin Driggers <jtxdriggers@gmail.com>
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

import org.holoeverywhere.widget.ExpandableListView;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.slidingmenu.lib.SlidingMenu;

public class VentriloidSlidingMenu extends SlidingMenu {
	
	public static final int MENU_SWITCH_VIEW = 0, MENU_USER_OPTIONS = 1, MENU_CLOSE = 2;
	
	public static final int MENU_SERVER_VIEW = 0, MENU_CHANNEL_VIEW = 1;
	
	public static final int MENU_ADMIN = 0, MENU_SET_TRANSMIT = 1, MENU_SET_COMMENT = 2, MENU_SET_URL = 3, MENU_CHAT = 4;
	
	public static final int MENU_MINIMIZE = 0, MENU_DISCONNECT = 1;
	
	private ExpandableListView mList;
	private SlidingMenuAdapter adapter;

	public VentriloidSlidingMenu(Context context) {
		super(context);
		setMenu(R.layout.sliding_menu);
        
        setMode(SlidingMenu.LEFT);
        setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		setBehindOffset((int)(getResources().getDisplayMetrics().widthPixels / 3));
        setFadeDegree(0.35f);

        mList = (ExpandableListView) findViewById(android.R.id.list);
        mList.setGroupIndicator(null);
        mList.setTranscriptMode(ExpandableListView.TRANSCRIPT_MODE_NORMAL);
	}
	
	public ExpandableListView getListView() {
		return mList;
	}
	
	public SlidingMenuAdapter getAdapter() {
		return adapter;
	}
	
	public void setAdapter(SlidingMenuAdapter adapter) {
		this.adapter = adapter;
		mList.setAdapter(adapter);
        
        for (int i = 0; i < adapter.getGroupCount(); i++) {
        	mList.expandGroup(i);
        }
	}
	
	public void makeViewPersistent(Activity activity, int viewId) {
		ViewGroup contentParent = (ViewGroup) getContent();
		View v = contentParent.findViewById(viewId);
		contentParent.removeView(v);
		
		ViewGroup parent = (ViewGroup) activity.findViewById(android.R.id.content);
		View parentLayout = parent.getChildAt(0);
		parent.removeView(parentLayout);
		
		RelativeLayout layout = new RelativeLayout(activity.getApplicationContext());
		layout.addView(parentLayout);
		
		RelativeLayout.LayoutParams parentParams = (LayoutParams) parentLayout.getLayoutParams();
		parentParams.addRule(RelativeLayout.ABOVE, viewId);
		parentLayout.setLayoutParams(parentParams);
		
		RelativeLayout.LayoutParams params = (LayoutParams) v.getLayoutParams();
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		v.setLayoutParams(params);
		
		layout.addView(v);
		
		parent.addView(layout);
	}

}
