package com.jtxdriggers.android.ventriloid;

public class Server {

	private String username, phonetic, servername, hostname, password;
	private int id, port;
	
	public Server(Integer id, String username, String phonetic, String servername, String hostname, int port, String password) {
		setId(id);
		setUsername(username);
		setPhonetic(phonetic);
		setServername(servername);
		setHostname(hostname);
		setPort(port);
		setPassword(password);
	}

	public Server(String username, String phonetic, String servername, String hostname, int port, String password) {
		setUsername(username);
		setPhonetic(phonetic);
		setServername(servername);
		setHostname(hostname);
		setPort(port);
		setPassword(password);
	}
	
	public Server() {
		
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPhonetic() {
		return phonetic;
	}

	public void setPhonetic(String phonetic) {
		this.phonetic = phonetic;
	}

	public String getServername() {
		return servername;
	}

	public void setServername(String servername) {
		this.servername = servername;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
