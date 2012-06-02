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
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
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
				mTabHost.setCurrentTabByTag("Tab" + position);
			}
		});
		mViewPager.setOffscreenPageLimit(3);
    }
    
	private void initialiseTabHost(Bundle args) {
		mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        
        ImageView divider1 = new ImageView(this);
        divider1.setImageResource(R.drawable.tab_separator);
        ImageView divider2 = new ImageView(this);
        divider2.setImageResource(R.drawable.tab_separator);
        
        TabInfo tabInfo = null;
        final View[] tabs = new View[3];
        for (int i = 0; i < tabs.length; i++) {
        	tabs[i] = getLayoutInflater().inflate(R.layout.tab_indicator, null);
        }
        final float scale = getResources().getDisplayMetrics().density;
        int pixels = (int) (scale + 0.5f);
        
        ((TextView)tabs[0].findViewById(android.R.id.text1)).setText("Server");
        ViewPagerActivity.AddTab(this, mTabHost, mTabHost.newTabSpec("Tab0").setIndicator(tabs[0]), ( tabInfo = new TabInfo("Tab0", ServerView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);
        mTabHost.getTabWidget().addView(divider1, pixels, LayoutParams.FILL_PARENT);
        ((TextView)tabs[1].findViewById(android.R.id.text1)).setText("Channel");
        ViewPagerActivity.AddTab(this, mTabHost, mTabHost.newTabSpec("Tab1").setIndicator(tabs[1]), ( tabInfo = new TabInfo("Tab1", ChannelView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);
        mTabHost.getTabWidget().addView(divider2, pixels, LayoutParams.FILL_PARENT);
        ((TextView)tabs[2].findViewById(android.R.id.text1)).setText("Chat");
        ViewPagerActivity.AddTab(this, mTabHost, mTabHost.newTabSpec("Tab2").setIndicator(tabs[2]), ( tabInfo = new TabInfo("Tab2", ChatView.class, args)));
        mapTabInfo.put(tabInfo.tag, tabInfo);

        mTabHost.setOnTabChangedListener(new OnTabChangeListener() {
			public void onTabChanged(String tabId) {
				for (int i = 0; i < 3; i++) {
					if (tabId.equals("Tab" + i)) {
						tabs[i].findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.blue));
						((TextView) tabs[i].findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.white));
					} else {
						tabs[i].findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.black));
						((TextView) tabs[i].findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.gray));
					}
				}
				int pos = mTabHost.getCurrentTab();
				mViewPager.setCurrentItem(pos);
			}
        });
        
		mTabHost.getCurrentTabView().findViewById(R.id.selected).setBackgroundColor(getResources().getColor(R.color.blue));
		((TextView) mTabHost.getCurrentTabView().findViewById(android.R.id.text1)).setTextColor(getResources().getColor(R.color.white));
	}

	private static void AddTab(ViewPagerActivity activity, final TabHost tabHost, TabHost.TabSpec tabSpec, final TabInfo tabInfo) {
		tabSpec.setContent(activity.new TabFactory(activity));
        tabHost.addTab(tabSpec);
        tabHost.getTabWidget().getChildTabViewAt(tabHost.getTabWidget().getTabCount() - 1).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				tabHost.setCurrentTabByTag(tabInfo.tag);
			}
        });
	}
}
