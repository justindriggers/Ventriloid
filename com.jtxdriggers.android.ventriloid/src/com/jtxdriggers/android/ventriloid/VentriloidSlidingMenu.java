package com.jtxdriggers.android.ventriloid;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.ExpandableListView;
import org.holoeverywhere.widget.TextView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

import com.slidingmenu.lib.SlidingMenu;

public class VentriloidSlidingMenu extends SlidingMenu {
	
	public static final int MENU_SWITCH_VIEW = 0;
	public static final int MENU_USER_OPTIONS = 1;
	public static final int MENU_CLOSE = 2;
	
	public static final int MENU_SERVER_VIEW = 0;
	public static final int MENU_CHANNEL_VIEW = 1;
	
	public static final int MENU_SET_COMMENT = 0;
	public static final int MENU_SET_URL = 1;
	public static final int MENU_JOIN_CHAT = 2;
	
	public static final int MENU_MINIMIZE = 0;
	public static final int MENU_DISCONNECT = 1;
	
	private ExpandableListView mList;
	private SlidingMenuAdapter adapter;
	private String[] sections = {"Switch View", "User Options", "Close"};
	private String[][] options = { {"Server", "Channel"}, {"Set Comment", "Set URL", "Join Chat"}, {"Minimize", "Disconnect"} };
	private int activeView = MENU_SERVER_VIEW;

	public VentriloidSlidingMenu(Context context) {
		super(context);
		setMenu(R.layout.sliding_menu);
        
        setMode(SlidingMenu.LEFT);
        setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		setBehindOffset((int)(getResources().getDisplayMetrics().widthPixels / 3));
        setFadeDegree(0.35f);
		
        adapter = new SlidingMenuAdapter(context, sections, options);

        mList = (ExpandableListView) findViewById(android.R.id.list);
        mList.setGroupIndicator(null);
        mList.setAdapter(adapter);
        
        for (int i = 0; i < adapter.getGroupCount(); i++) {
        	mList.expandGroup(i);
        }
	}
	
	public ExpandableListView getListView() {
		return mList;
	}
	
	public void notifyDataSetChanged() {
		adapter.notifyDataSetChanged();
	}
	
	public void setActiveView(int activeView) {
		this.activeView = activeView;
	}
	
	public int getDrawableResource(int groupPosition, int childPosition) {
		switch (groupPosition) {
		case MENU_SWITCH_VIEW:
			switch (childPosition) {
			case MENU_SERVER_VIEW:
				return R.drawable.server;
			case MENU_CHANNEL_VIEW:
				return R.drawable.view;
			}
			break;
		case MENU_USER_OPTIONS:
			switch (childPosition) {
			case MENU_SET_COMMENT:
				return R.drawable.comment;
			case MENU_SET_URL:
				return R.drawable.url;
			case MENU_JOIN_CHAT:
				return R.drawable.comment;
			}
			break;
		case MENU_CLOSE:
			switch (childPosition) {
			case MENU_MINIMIZE:
				return R.drawable.minimize;
			case MENU_DISCONNECT:
				return R.drawable.remove;
			}
			break;
		}
		return -1;
	}
	
	public class SlidingMenuAdapter extends BaseExpandableListAdapter {
		
		private Context mContext;
		private String[] groups;
		private String[][] children;
		
		public SlidingMenuAdapter(Context context, String[] groups, String[][] children) {
			mContext = context;
			this.groups = groups;
			this.children = children;
		}
		
		@Override
		public String getChild(int groupPosition, int childPosition) {
			return children[groupPosition][childPosition];
		}
		
		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}
		
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = LayoutInflater.inflate(mContext, R.layout.menu_item);
			
			View selected = convertView.findViewById(R.id.selected);
			selected.setBackgroundResource(0);
			if (groupPosition == MENU_SWITCH_VIEW && childPosition == activeView)
				selected.setBackgroundColor(getResources().getColor(R.color.holo_blue));
			
			int imageResource = getDrawableResource(groupPosition, childPosition);
			if (imageResource != -1) {
				ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
				imageView.setImageResource(imageResource);
			}
			
			TextView textView = (TextView) convertView.findViewById(R.id.title);
			textView.setText(children[groupPosition][childPosition]);
			textView.setTextColor(getResources().getColor(R.color.primary_text_holo_dark));
            return convertView;
		}
		
		@Override
		public int getChildrenCount(int groupPosition) {
			return children[groupPosition].length;
		}
		
		@Override
		public String getGroup(int groupPosition) {
			return groups[groupPosition];
		}
		
		@Override
		public int getGroupCount() {
			return groups.length;
		}
		
		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}
		
		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = LayoutInflater.inflate(mContext, R.layout.preference_category_holo);
			
			TextView textView = (TextView) convertView.findViewById(R.id.title);
			textView.setText(groups[groupPosition]);
			textView.setTextColor(getResources().getColor(R.color.primary_text_holo_dark));
			convertView.setClickable(true);
            return convertView;
		}
		
		@Override
		public boolean hasStableIds() {
			return true;
		}
		
		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

}
