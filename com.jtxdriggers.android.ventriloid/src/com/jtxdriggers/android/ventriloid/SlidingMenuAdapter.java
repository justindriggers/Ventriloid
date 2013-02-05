package com.jtxdriggers.android.ventriloid;

import java.util.ArrayList;

import org.holoeverywhere.LayoutInflater;
import org.holoeverywhere.widget.TextView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;

public class SlidingMenuAdapter extends BaseExpandableListAdapter {
	
	private Context mContext;
	private ArrayList<String> groups;
	private ArrayList<ArrayList<String>> children;
	private int activeView = VentriloidSlidingMenu.MENU_SERVER_VIEW;
	
	public SlidingMenuAdapter(Context context, ItemData items) {
		mContext = context;
		
		groups = new ArrayList<String>();
		groups.add("Switch View");
		groups.add("User Options");
		groups.add("Close");
		
		setMenuItems(items);
	}
	
	public void setMenuItems(ItemData items) {
		children = items.getMenuItems();
		activeView = items.getActiveView();
		notifyDataSetChanged();
	}
	
	public int getDrawableResource(int groupPosition, int childPosition) {
		switch (groupPosition) {
		case VentriloidSlidingMenu.MENU_SWITCH_VIEW:
			switch (childPosition) {
			case VentriloidSlidingMenu.MENU_SERVER_VIEW:
				return R.drawable.server;
			case VentriloidSlidingMenu.MENU_CHANNEL_VIEW:
				return R.drawable.view;
			default:
				return R.drawable.comment;
			}
		case VentriloidSlidingMenu.MENU_USER_OPTIONS:
			switch (childPosition) {
			case VentriloidSlidingMenu.MENU_ADMIN:
				return R.drawable.admin;
			case VentriloidSlidingMenu.MENU_SET_TRANSMIT:
				return R.drawable.transmit;
			case VentriloidSlidingMenu.MENU_SET_COMMENT:
				return R.drawable.edit;
			case VentriloidSlidingMenu.MENU_SET_URL:
				return R.drawable.url;
			case VentriloidSlidingMenu.MENU_CHAT:
				return R.drawable.comment;
			}
			break;
		case VentriloidSlidingMenu.MENU_CLOSE:
			switch (childPosition) {
			case VentriloidSlidingMenu.MENU_MINIMIZE:
				return R.drawable.minimize;
			case VentriloidSlidingMenu.MENU_DISCONNECT:
				return R.drawable.remove;
			}
			break;
		}
		return -1;
	}
	
	@Override
	public String getChild(int groupPosition, int childPosition) {
		return children.get(groupPosition).get(childPosition);
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
		if (groupPosition == VentriloidSlidingMenu.MENU_SWITCH_VIEW && childPosition == activeView)
			selected.setBackgroundColor(mContext.getResources().getColor(R.color.holo_blue));
		
		int imageResource = getDrawableResource(groupPosition, childPosition);
		if (imageResource != -1) {
			ImageView imageView = (ImageView) convertView.findViewById(R.id.icon);
			imageView.setImageResource(imageResource);
		}
		
		TextView textView = (TextView) convertView.findViewById(R.id.title);
		textView.setText(children.get(groupPosition).get(childPosition));
		textView.setTextColor(mContext.getResources().getColor(R.color.primary_text_holo_dark));
        return convertView;
	}
	
	@Override
	public int getChildrenCount(int groupPosition) {
		return children.get(groupPosition).size();
	}
	
	@Override
	public String getGroup(int groupPosition) {
		return groups.get(groupPosition);
	}
	
	@Override
	public int getGroupCount() {
		return groups.size();
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
		textView.setText(groups.get(groupPosition));
		textView.setTextColor(mContext.getResources().getColor(R.color.primary_text_holo_dark));
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