package com.jtxdriggers.android.ventriloid;

import java.util.HashMap;

public class Item {
	
	public short id, parent;
	public String name, phonetic, comment;
	public String status = "";
	public String indent = "";
	
	public class Channel extends Item {
		public short parent;
		public boolean reqPassword;
		
		public Channel() {
			this.id = 0;
			this.parent = 0;
			this.name = "Lobby";
			this.phonetic = "Lobby";
			this.comment = "";
			this.reqPassword = false;
		}
		
		public Channel(String name, String phonetic, String comment) {
			this.id = 0;
			this.parent = 0;
			this.name = name;
			this.phonetic = phonetic;
			this.comment = comment;
			this.reqPassword = false;
		}
		
		public Channel(short id, short parent, String name, String phonetic, String comment, boolean reqPassword) {
			this.id = id;
			this.parent = parent;
			this.name = name;
			this.phonetic = phonetic;
			this.comment = comment;
			this.reqPassword = reqPassword;
		}
		
		public HashMap<String, Object> toHashMap() {
			HashMap<String, Object> channel = new HashMap<String, Object>();
			channel.put("name", name);
			channel.put("comment", comment);
			channel.put("indent", indent);
			
			return channel;
		}
	}
	
	public class User extends Item {
		
		public static final int XMIT_OFF = R.drawable.user_status_inactive;
		public static final int XMIT_INIT = R.drawable.user_status_other;
		public static final int XMIT_ON = R.drawable.user_status_active;
		
		public String rank, url, integration;
		public int xmit = XMIT_OFF;
		
		public User(short id, short parent, String name, String phonetic, String rank, String comment, String url, String integration) {
			this.id = id;
			this.parent = parent;
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
		
		public HashMap<String, Object> toHashMap() {
			HashMap<String, Object> user = new HashMap<String, Object>();
			user.put("name", name);
			user.put("rank", formatRank(rank));
			user.put("comment", formatComment(url, comment));
			user.put("integration", integration);
			user.put("indent", indent);
			user.put("status", status);
			user.put("xmit", xmit);
			
			return user;
		}
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

}
