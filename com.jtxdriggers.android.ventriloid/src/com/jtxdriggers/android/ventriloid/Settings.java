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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.view.Gravity;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Settings extends PreferenceActivity {
	
	private Preference ptt;
	private KeyCharacterMap keyMap;
	private int key;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		key = prefs.getInt("ptt_key", KeyEvent.KEYCODE_CAMERA);
		keyMap = KeyCharacterMap.load(KeyCharacterMap.BUILT_IN_KEYBOARD);
		
		ptt = findPreference("ptt");
		
		if (prefs.getBoolean("voice_activation", false)) {
			ptt.setEnabled(false);
		}
		
		Preference voiceActivation = findPreference("voice_activation");
		voiceActivation.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				if ((Boolean) newValue)
					ptt.setEnabled(false);
				else
					ptt.setEnabled(true);
				return true;
			}
		});
		
		final Preference customKey = findPreference("ptt_key");
		customKey.setSummary("Current: " + getKeyName(key));
		customKey.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				Builder builder = new AlertDialog.Builder(Settings.this);
				builder.setTitle("Set PTT Key");
				TextView message = new TextView(Settings.this);
				message.setText("Press the desired PTT button,\nthen press OK");
				message.setGravity(Gravity.CENTER_HORIZONTAL);
				final TextView currentKey = new TextView(Settings.this);
				currentKey.setPadding(0, 10, 0, 10);
				currentKey.setText("Current Key: " + getKeyName(key));
				currentKey.setGravity(Gravity.CENTER_HORIZONTAL);
				LinearLayout layout = new LinearLayout(Settings.this);
				layout.setOrientation(LinearLayout.VERTICAL);
				layout.addView(message);
				layout.addView(currentKey);
				builder.setView(layout);
				builder.setOnKeyListener(new OnKeyListener() {
					public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
						if (event.getAction() == KeyEvent.ACTION_DOWN) {
							key = keyCode;
							currentKey.setText("Current Key: " + getKeyName(keyCode));
						}
						return true;
					}
				});
				builder.setPositiveButton("OK", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						PreferenceManager.getDefaultSharedPreferences(Settings.this).edit().putInt("ptt_key", key).commit();
						customKey.setSummary("Current: " + getKeyName(key));
					}
				});
				builder.setNegativeButton("Cancel", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				builder.show();
				return true;
			}
		});
		
		Preference donate = super.findPreference("donate");
		donate.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference arg0) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=N95UGEQ6FAKPN")));
		    	return true;
			}
		});
	}
	
	private String getKeyName(int keyCode) {
		if (keyMap.isPrintingKey(keyCode))
			return Character.toString(keyMap.getDisplayLabel(keyCode));
		else {
			switch (keyCode) {
			case KeyEvent.KEYCODE_CAMERA:
				return "Camera";
			case KeyEvent.KEYCODE_VOLUME_UP:
				return "Volume Up";
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				return "Volume Down";
			case KeyEvent.KEYCODE_BACK:
				return "Back";
			case KeyEvent.KEYCODE_HOME:
				return "Home";
			case KeyEvent.KEYCODE_MENU:
				return "Menu";
			case KeyEvent.KEYCODE_SEARCH:
				return "Search";
			case KeyEvent.KEYCODE_CALL:
				return "Call";
			case KeyEvent.KEYCODE_ENDCALL:
				return "End Call";
			case KeyEvent.KEYCODE_DPAD_CENTER:
				return "Select";
			case KeyEvent.KEYCODE_DPAD_UP:
				return "Up";
			case KeyEvent.KEYCODE_DPAD_DOWN:
				return "Down";
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				return "Right";
			case KeyEvent.KEYCODE_DPAD_LEFT:
				return "Left";
			case KeyEvent.KEYCODE_SPACE:
				return "Space";
			case KeyEvent.KEYCODE_ALT_LEFT:
				return "Left Alt";
			case KeyEvent.KEYCODE_ALT_RIGHT:
				return "Right Alt";
			case KeyEvent.KEYCODE_DEL:
				return "Delete";
			case KeyEvent.KEYCODE_ENTER:
				return "Enter";
			case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
				return "Fast Forward";
			case KeyEvent.KEYCODE_MEDIA_NEXT:
				return "Next";
			case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
				return "Play/Pause";
			case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
				return "Previous";
			case KeyEvent.KEYCODE_MEDIA_REWIND:
				return "Rewind";
			case KeyEvent.KEYCODE_MEDIA_STOP:
				return "Stop";
			case KeyEvent.KEYCODE_MUTE:
				return "Mute";
				
			default:
				return "Key Code " + Integer.toString(keyCode);
			}
		}
	}
}