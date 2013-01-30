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

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ServerAdapter extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "ventriloiddata";
	public static final int DATABASE_VERSION = 3;
	
	public static final String TABLE_SERVERS = "Servers";
	
	public static final String KEY_ID = "ID";
	public static final String KEY_USERNAME = "Username";
	public static final String KEY_PHONETIC = "Phonetic";
	public static final String KEY_SERVERNAME = "Servername";
	public static final String KEY_HOSTNAME = "Hostname";
	public static final String KEY_PORT = "Port";
	public static final String KEY_PASSWORD = "Password";
	
    public static final String CREATE_SERVERS_TABLE = "CREATE TABLE " + TABLE_SERVERS + "("
	+ KEY_ID + " INTEGER PRIMARY KEY, " + KEY_USERNAME + " TEXT NOT NULL, "
	+ KEY_PHONETIC + " TEXT NOT NULL, " + KEY_SERVERNAME + " TEXT NOT NULL, "
	+ KEY_HOSTNAME + " TEXT NOT NULL, " + KEY_PORT + " INTEGER NOT NULL, "
	+ KEY_PASSWORD + " TEXT NOT NULL);";
	
	public ServerAdapter(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_SERVERS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 3) {
			db.execSQL("ALTER TABLE " + TABLE_SERVERS + " RENAME TO ServersTemp;");
			db.execSQL(CREATE_SERVERS_TABLE);
			db.execSQL("INSERT INTO " + TABLE_SERVERS + "("
					+ KEY_ID + ", " + KEY_USERNAME + ", "
					+ KEY_PHONETIC + ", " + KEY_SERVERNAME + ", "
					+ KEY_HOSTNAME + ", " + KEY_PORT + ", "
					+ KEY_PASSWORD + ") "
					+ "SELECT _id, username, phonetic, "
					+ "servername, hostname, portnumber, password "
					+ "FROM ServersTemp;");
			db.execSQL("DROP TABLE IF EXISTS ServersTemp;");
		}
	}
	
	public void addServer(Server server) {
	    SQLiteDatabase db = getWritableDatabase();
	 
	    ContentValues values = new ContentValues();
	    values.put(KEY_USERNAME, server.getUsername());
	    values.put(KEY_PHONETIC, server.getPhonetic());
	    values.put(KEY_SERVERNAME, server.getServername());
	    values.put(KEY_HOSTNAME, server.getHostname());
	    values.put(KEY_PORT, server.getPort());
	    values.put(KEY_PASSWORD, server.getPassword());
	    
	    db.insert(TABLE_SERVERS, null, values);
	    db.close();
	}
	
	public Server getServer(int id) {
	    SQLiteDatabase db = getReadableDatabase();
	 
	    Cursor cursor = db.query(true, TABLE_SERVERS, new String[] { KEY_ID, KEY_USERNAME,
	            KEY_PHONETIC, KEY_SERVERNAME, KEY_HOSTNAME, KEY_PORT, KEY_PASSWORD }, KEY_ID + "=" + id,
	            null, null, null, null, null);
	    if (cursor != null)
	        cursor.moveToFirst();
	 
	    Server server = new Server(Integer.parseInt(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3),
	    		cursor.getString(4), Integer.parseInt(cursor.getString(5)), cursor.getString(6));
	    cursor.close();
	    db.close();
	    
	    return server;
	}
	
	public ArrayList<Server> getAllServers() {
	    ArrayList<Server> serverList = new ArrayList<Server>();
	    String selectQuery = "SELECT * FROM " + TABLE_SERVERS;
	 
	    SQLiteDatabase db = getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    if (cursor.moveToFirst()) {
	        do {
	            Server server = new Server();
	            server.setId(Integer.parseInt(cursor.getString(0)));
	            server.setUsername(cursor.getString(1));
	            server.setPhonetic(cursor.getString(2));
	            server.setServername(cursor.getString(3));
	            server.setHostname(cursor.getString(4));
	            server.setPort(Integer.parseInt(cursor.getString(5)));
	            server.setPassword(cursor.getString(6));
	            
	            serverList.add(server);
	        } while (cursor.moveToNext());
	    }
	    cursor.close();
	    db.close();
	 
	    return serverList;
	}
	
	public ArrayList<String> getAllServersAsStrings() {
	    ArrayList<String> serverList = new ArrayList<String>();
	    String selectQuery = "SELECT * FROM " + TABLE_SERVERS;
	 
	    SQLiteDatabase db = getWritableDatabase();
	    Cursor cursor = db.rawQuery(selectQuery, null);
	 
	    if (cursor.moveToFirst()) {
	        do {
	            Server server = new Server();
	            server.setUsername(cursor.getString(1));
	            server.setServername(cursor.getString(3));
	            server.setHostname(cursor.getString(4));
	            server.setPort(Integer.parseInt(cursor.getString(5)));
	            
	            serverList.add(server.getUsername() + "@" + server.getServername() + ": " + server.getHostname() + ":" + server.getPort());
	        } while (cursor.moveToNext());
	    }
	    cursor.close();
	    db.close();
	 
	    return serverList;
	}
	
	public int getServersCount() {
        String countQuery = "SELECT  * FROM " + TABLE_SERVERS;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
 
        int count = cursor.getCount();

        cursor.close();
	    db.close();
	    return count;
    }
	
	public void updateServer(Server server) {
	    SQLiteDatabase db = getWritableDatabase();
	 
	    ContentValues values = new ContentValues();
	    values.put(KEY_USERNAME, server.getUsername());
	    values.put(KEY_PHONETIC, server.getPhonetic());
	    values.put(KEY_SERVERNAME, server.getServername());
	    values.put(KEY_HOSTNAME, server.getHostname());
	    values.put(KEY_PORT, server.getPort());
	    values.put(KEY_PASSWORD, server.getPassword());
	    values.put(KEY_ID, server.getId());
	 
	    db.update(TABLE_SERVERS, values, KEY_ID + " = " + server.getId(), null);

	    db.close();
	}
	
	public void deleteServer(Server server) {
	    SQLiteDatabase db = getWritableDatabase();
	    db.delete(TABLE_SERVERS, KEY_ID + " = ?",
	            new String[] { String.valueOf(server.getId()) });
	    db.close();
	}
	
	public void clearServers() {
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_SERVERS, null, null);
		db.close();
	}
	
}
