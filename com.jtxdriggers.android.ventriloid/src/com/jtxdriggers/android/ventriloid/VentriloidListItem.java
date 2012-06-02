package com.jtxdriggers.android.ventriloid;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class VentriloidListItem {
	static final int USER = 1;
	static final int CHANNEL = 2;
	
	static final int PLAYER_OFF = 0;
	static final int PLAYER_INIT = 1;
	static final int PLAYER_ON = 2;
	
	public short id = -1;
	public String name = "";
	public String comment = "";
	public String url = "";
	public String commenttext = "";
	public String integration = "";
	public String indent = "";
	public String rank = "";
	public String status = "";
	public int type = -1;
	public int passwordProtected = 0;
	public int userinchat = 0;
	public short parentid = 0;
	public int xmitStatus = PLAYER_OFF;
	
	public VentriloidListItem(int type, short id) {
		VentriloEventData data = new VentriloEventData();
		//VentriloEventData rankdata = new VentriloEventData();
		switch (type) {
			case USER:
				VentriloInterface.getuser(data, id);
				this.type = type;
				this.id = id;
				name = stringFromBytes(data.text.name);
				comment = stringFromBytes(data.text.comment);
				url = stringFromBytes(data.text.url);
				integration = stringFromBytes(data.text.integration_text);
				parentid = VentriloInterface.getuserchannel(id);
				//VentriloInterface.getrank(rankdata, data.data.rank.id);
				//rank = stringFromBytes(rankdata.text.name);
				if (!url.equals("")) {
					if (!comment.equals(""))
						commenttext = " (U: " + comment + ")";
					else
						commenttext = " (U:)";
				} else if (!comment.equals(""))
					commenttext = " (" + comment + ")";
				if (!integration.equals(""))
					integration = " \"" + integration + "\"";
				break;
			case CHANNEL:
				VentriloInterface.getchannel(data, id);
				this.type = type;
				this.id = id;
				name = stringFromBytes(data.text.name);
				comment = stringFromBytes(data.text.comment);
				parentid = data.data.channel.parent;
				if (!comment.equals(""))
					commenttext = " (" + comment + ")";
				//if (VentriloInterface.channelrequirespassword(id))
					//passwordProtected = 1;
				break;
		}
	}
	
	public VentriloidListItem(HashMap<String, String> item) {
		type = Integer.parseInt(item.get("type"));
		id = Short.parseShort(item.get("id"));
		parentid = Short.parseShort(item.get("parentid"));
		name = item.get("name");
		comment = item.get("comment");
		url = item.get("url");
		integration = item.get("integration");
		indent = item.get("indent");
		rank = item.get("rank");
		status = item.get("status");
		passwordProtected = Integer.parseInt(item.get("passwordProtected"));
		userinchat = Integer.parseInt(item.get("userinchat"));
		xmitStatus = Integer.parseInt(item.get("xmitStatus"));
		if (!url.equals("")) {
			if (!comment.equals(""))
				commenttext = " (U: " + comment + ")";
			else
				commenttext = " (U:)";
		} else if (!comment.equals(""))
			commenttext = " (" + comment + ")";
	}
	
	public VentriloidListItem() { }
	
	public static String stringFromBytes(byte[] bytes) {
		try {
			return new String(bytes, 0, (new String(bytes).indexOf(0)), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new String(bytes, 0, (new String(bytes).indexOf(0)));
		}
	}
	
	public HashMap<String, String> toHashMap() {
		HashMap<String, String> item = new HashMap<String, String>();
		item.put("type", Integer.toString(type));
		item.put("id", Short.toString(id));
		item.put("status", status);
		item.put("name", name);
		item.put("comment", comment);
		item.put("url", url);
		item.put("commenttext", commenttext);
		item.put("integration", integration);
		item.put("rank", rank);
		item.put("indent", indent);
		item.put("passwordProtected", Integer.toString(passwordProtected));
		item.put("userinchat", Integer.toString(userinchat));
		item.put("parentid", Short.toString(parentid));
		item.put("xmitStatus", Integer.toString(xmitStatus));
		return item;
	}

}
