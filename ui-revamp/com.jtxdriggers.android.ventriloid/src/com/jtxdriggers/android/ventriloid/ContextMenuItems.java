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

public class ContextMenuItems {
	
	public static final int SERVER_VIEW = -2;
	public static final int CHANNEL_VIEW = -1;

	public static final int DISCONNECT		= 0;
	public static final int USER_OPTIONS	= 1;
	public static final int MOVE_TO_CHANNEL	= 2;
	public static final int CLEAR_PASSWORD	= 3;
	public static final int ADD_PHANTOM		= 4;
	public static final int REMOVE_PHANTOM	= 5;
	public static final int	SEND_PAGE		= 6;
	public static final int PRIVATE_CHAT	= 7;
	public static final int MUTE			= 8;
	public static final int SET_VOLUME		= 9;
	public static final int SET_COMMENT		= 10;
	public static final int VIEW_COMMENT	= 11;
	public static final int SET_URL			= 12;
	public static final int VIEW_URL		= 13;
	public static final int ADMIN_LOGIN		= 14;
	public static final int ADMIN_LOGOUT	= 15;
	public static final int KICK_USER		= 16;
	public static final int BAN_USER		= 17;
	public static final int GLOBALLY_MUTE	= 18;
	public static final int CHANNEL_KICK	= 19;
	public static final int CHANNEL_BAN		= 20;
	public static final int CHANNEL_MUTE	= 21;
	public static final int SERVER_ADMIN	= 22;
	public static final int CHANNEL_ADMIN	= 23;
	public static final int MOVE_USER		= 24;
	
	// MOVE_USER_TO must be the highest int value for the options to work properly.
	public static final int MOVE_USER_TO	= 50;
	
}
