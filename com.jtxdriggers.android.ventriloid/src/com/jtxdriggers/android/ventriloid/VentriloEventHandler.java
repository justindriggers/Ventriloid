/*
 * Copyright 2010 Daniel Sloof <daniel@danslo.org>
 *
 * This file is part of Mangler.
 *
 * $LastChangedDate$
 * $Revision$
 * $LastChangedBy$
 * $URL$
 *
 * Mangler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mangler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mangler.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jtxdriggers.android.ventriloid;

import android.util.Log;

public class VentriloEventHandler {

	public String StringFromBytes(byte[] bytes) {
		return new String(bytes, 0, (new String(bytes).indexOf(0)));
	}
	
	public void process() {
		
		VentriloEventData data;
		
		while ((data = VentriloidService.getNext()) != null) {
			Log.d("ventriloid", "EventHandler: processing event type " + data.type);
			switch (data.type) {
			case VentriloEvents.V3_EVENT_STATUS:
				Log.d("ventriloid", StringFromBytes(data.status.message));
				break;
				
				case VentriloEvents.V3_EVENT_ERROR_MSG:
					Log.d("ventriloid", StringFromBytes(data.error.message));
					break;
					
				case VentriloEvents.V3_EVENT_DISCONNECT:
					VentriloidService.clearEvents();
					break;
					
				default:
					Log.d("ventriloid", "Unhandled event type: " + Integer.toString(data.type));
					break;
			}
		}
	}
}