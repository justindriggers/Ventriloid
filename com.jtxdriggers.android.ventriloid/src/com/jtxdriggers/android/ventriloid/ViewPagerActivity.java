package com.jtxdriggers.android.ventriloid;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabContentFactory;
import android.widget.TextView;

public class ViewPagerActivity extends FragmentActivity {

	private TabHost mTabHost;
	private ViewPager mViewPager;
	private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, ViewPagerActivity.TabInfo>();
	private PagerAdapter mPagerAdapter;
	private int ping = 0;

    @SuppressWarnings("unused")
	private class TabInfo {
		 private String tag;
         private Class<?> clss;
		private Bundle args;
         private Fragment fragment;
         TabInfo(String tag, Class<?> clss, Bundle args) {
        	 this.tag = tag;
        	 this.clss = clss;
        	 this.args = args;
         }

	}

	class TabFactory implements TabContentFactory {

		private final Context mContext;

	    public TabFactory(Context context) {
	        mContext = context;
	    }

	    public View createTabContent(String tag) {
	        View v = new View(mContext);
	        v.setMinimumWidth(0);
	        v.setMinimumHeight(0);
	        return v;
	    }

	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.viewpager);
		
		initialiseTabHost(savedInstanceState);
		if (savedInstanceState != null)
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
		
		intialiseViewPager();

		if (ping < 65535 && ping > 0)
			setTitle("Ping: " + ping + "ms");
		else
			setTitle("Checking latency...");
	}

    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("tab", mTabHost.getCurrentTabTag());
        super.onSaveInstanceState(outState);
    }

    private void intialiseViewPager() {
		List<Fragment> fragments = new Vector<Fragment>();
		fragments.add(Fragment.instantiate(this, ServerView.class.getName()));
		fragments.add(Fragment.instantiate(this, ChannelView.class.getName()));
		fragments.add(Fragment.instantiate(this, ChatView.class.getName()));
		mPagerAdapter  = new PagerAdapter(super.getSupportFragmentManager(), fragments);
		
		mViewPager = (ViewPager)super.findViewById(R.id.viewpager);
		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener() {
			public void onPageScrollStateChanged(int state) { }
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
			public void onPageSelected(int position) {
				mTabHost.setCurrentTab(position);
			}
		});
		mViewPager.setOffscreenPageLimit(3);
    }
    
	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        TabInfo tabInfo = null;
        final View[] tabs = new View[3];
        for (int i = 0; i < tabs.length; i++) {
        	tabs[i] = getLayoutInflater().inflate(R.layout.tab_indicator, null);
        }
        ((TextView)tabs[0].findViewById(android.R.id.text1)).setText("Server");
        ViewPagerActivity.AddTab(this, mTabHost, mTabHost.newTabSpec("ServerView").setIndicator(tabs[0]), ( tabInfo = new TabInfo("ServerView", ServerView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);
        ((TextView)tabs[1].findViewById(android.R.id.text1)).setText("Channel");
        ViewPagerActivity.AddTab(this, mTabHost, mTabHost.newTabSpec("ChannelView").setIndicator(tabs[1]), ( tabInfo = new TabInfo("ChannelView", ChannelView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);
        ((TextView)tabs[2].findViewById(android.R.id.text1)).setText("Chat");
        ViewPagerActivity.AddTab(this, mTabHost, mTabHost.newTabSpec("ChatView").setIndicator(tabs[2]), ( tabInfo = new TabInfo("ChatView", ChatView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);

        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				for (int i = 0; i < tabs.length; i++) {
					tabs[i].findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.black));
					((TextView) tabs[i].findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.gray));
				}
				mTabHost.getCurrentTabView().findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.blue));
				((TextView) mTabHost.getCurrentTabView().findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.white));
			}
        });
        
		mTabHost.getCurrentTabView().findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.blue));
		((TextView) mTabHost.getCurrentTabView().findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.white));
	}

	private static void AddTab(ViewPagerActivity activity, TabHost tabHost, TabHost.TabSpec tabSpec, TabInfo tabInfo) {
		tabSpec.setContent(activity.new TabFactory(activity));
        tabHost.addTab(tabSpec);
	}
}
