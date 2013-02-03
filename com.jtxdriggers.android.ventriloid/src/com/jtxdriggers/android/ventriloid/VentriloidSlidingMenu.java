package com.jtxdriggers.android.ventriloid;

import org.holoeverywhere.widget.ExpandableListView;

import android.content.Context;

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
	
	public void notifyDataSetChanged() {
		adapter.notifyDataSetChanged();
	}

}
