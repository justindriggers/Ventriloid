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

import java.util.HashMap;

public class Item {
	
	public short id, parent;
	public String name, phonetic, comment;
	public String status = "";
	public String indent = "";
	public boolean hasPhantom = false;
	
	public class Channel extends Item {
		public boolean reqPassword, isAdmin, allowPhantoms, allowPaging;
		
		public Channel() {
			this.id = 0;
			this.parent = 0;
			this.name = "Lobby";
			this.phonetic = "Lobby";
			this.comment = "";
			this.reqPassword = false;
			this.isAdmin = false;
		}
		
		public Channel(String name, String phonetic, String comment) {
			this.id = 0;
			this.parent = 0;
			this.name = name;
			this.phonetic = phonetic;
			this.comment = comment;
			this.reqPassword = false;
		}
		
		public Channel(short id, short parent, String name, String phonetic, String comment, boolean reqPassword, boolean isAdmin, boolean allowPhantoms, boolean allowPaging) {
			this.id = id;
			this.parent = parent;
			this.name = name;
			this.phonetic = phonetic;
			this.comment = comment;
			this.reqPassword = reqPassword;
			this.isAdmin = isAdmin;
			this.allowPhantoms = allowPhantoms;
			this.allowPaging = allowPaging;
			if (isAdmin)
				status = "\"A\" ";
		}
		
		public HashMap<String, Object> toHashMap() {
			HashMap<String, Object> channel = new HashMap<String, Object>();
			channel.put("name", name);
			channel.put("status", status);
			channel.put("comment", comment);
			channel.put("indent", indent);
			
			return channel;
		}
		
		public void changeStatus(boolean admin) {
			status = "";
			if (admin)
				status = "\"A\" ";
		}
	}
	
	public class User extends Item {
		
		public static final int XMIT_OFF = R.drawable.user_status_inactive;
		public static final int XMIT_INIT = R.drawable.user_status_other;
		public static final int XMIT_ON = R.drawable.user_status_active;
		
		public static final int MUTE = 0;
		public static final int CHANNEL_MUTE = 1;
		public static final int GLOBAL_MUTE = 2;
		
		public String rank, url, integration;
		public int xmit = XMIT_OFF, volume = 74;
		public short realId;
		public boolean muted = false, globalMute = false, channelMute = false, inChat = false;
		
		public User() { }
		
		public User(short id, short parent, short realId, String name, String phonetic, String rank, String comment, String url, String integration) {
			this.id = id;
			this.parent = parent;
			this.realId = realId;
			this.name = name;
			this.phonetic = phonetic;
			this.rank = rank;
			this.comment = comment;
			this.url = url;
			this.integration = integration;
		}
		
		private String formatRank(String rank) {
			if (rank.length() > 0)
				return "[" + rank + "] ";
			else
				return "";
		}
		
		private String formatComment(String url, String comment) {
			boolean hasUrl = url.length() > 0;
			boolean hasComment = comment.length() > 0;
			
			if (hasUrl && hasComment)
				return " (U: " + comment + ")";
			else if (hasUrl)
				return " (U:)";
			else if (hasComment)
				return " (" + comment + ")";
			
			return "";
		}
		
		private String formatIntegration(String integration) {
			if (integration.length() > 0)
				return " \"" + integration + "\"";
			else
				return "";
		}
		
		public HashMap<String, Object> toHashMap() {
			HashMap<String, Object> user = new HashMap<String, Object>();
			user.put("name", name);
			user.put("rank", formatRank(rank));
			user.put("comment", formatComment(url, comment));
			user.put("integration", formatIntegration(integration));
			user.put("indent", indent);
			user.put("status", status);
			user.put("xmit", xmit);
			
			return user;
		}
		
		public void updateStatus() {
			status = "";
			if (channelMute)
				status += "N";
			if (globalMute)
				status += "G";
			if (realId != 0)
				status += "P";
			if (muted)
				status += "M";
			if (inChat)
				status += "C";
			if (volume != 74)
				status += "S";
			
			if (status.length() > 0) {
				status = "\"" + status + "\" ";
			}
		}
	}

}
