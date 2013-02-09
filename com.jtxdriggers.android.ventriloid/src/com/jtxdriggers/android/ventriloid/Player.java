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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;

public class Player {
	
	private Handler handler = new Handler();
	private Map<Short, AudioTrack> tracks;
	private AudioTrack blankTrack;
	private AudioManager am;
	private boolean running = false, block = false;
	
	public Player(VentriloidService s) {
		tracks = new HashMap<Short, AudioTrack>();
		
		am = (AudioManager) s.getSystemService(Context.AUDIO_SERVICE);

		running = true;
		initBlankTrack();
	}
	
	public void stop() {
		running = false;
		blankTrack.pause();
		blankTrack.flush();
		blankTrack.release();
	}

    public void close(short id) {
        AudioTrack track;
        if ((track = tracks.get(id)) != null) {
        	track.release();
        	tracks.remove(id);
        }
    }

    public void clear() {
        Set<Entry<Short, AudioTrack>> set = tracks.entrySet();
        for (Iterator<Entry<Short, AudioTrack>> iter = set.iterator(); iter.hasNext();) {
            Entry<Short, AudioTrack> entry = iter.next();
            entry.getValue().flush();
            entry.getValue().release();
        }
        tracks.clear();
    }
	
	private AudioTrack open(short id, int rate, int channels, int buffer) {
        AudioTrack track;
        close(id);
    	track = new AudioTrack(am.isBluetoothScoOn() ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC, rate, channels,
    			AudioFormat.ENCODING_PCM_16BIT, buffer, AudioTrack.MODE_STREAM);
        tracks.put(id, track);
        return track;
	}
	
	public void write(short id, int rate, byte channels, byte[] sample, int length) {
		if (block)
			return;
		
		AudioTrack track;
		if ((track = tracks.get(id)) == null) {
			int bufferSize = 0;
			int channelsConfig = AudioFormat.CHANNEL_OUT_MONO;
			if (!am.isBluetoothScoOn() && channels == 2)
				channelsConfig = AudioFormat.CHANNEL_OUT_STEREO;

			if (rate == 48000) {
				bufferSize = AudioTrack.getMinBufferSize(rate, channelsConfig, AudioFormat.ENCODING_PCM_16BIT);
			} else
				bufferSize = VentriloInterface.pcmlengthforrate(rate) * channels * 2;
			
            track = open(id, rate, channelsConfig, bufferSize);
            track.play();
		}
		track.write(sample, 0, length);
	}
	
	public void initBlankTrack() {		
		// This thread tricks the system into letting us control volume no matter what activity is in the foreground.
		if (blankTrack != null) {
			blankTrack.pause();
			blankTrack.flush();
			blankTrack.release();
			blankTrack = null;
		}
		
		blankTrack = new AudioTrack(am.isBluetoothScoOn() ? AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT,
				AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT),
				AudioTrack.MODE_STREAM);
		blankTrack.play();
		
		final byte[] blank = new byte[8000000];
		for (int i = 0; i < blank.length; i++) {
			blank[i] = 0;
		}
		
		new Thread(new Runnable() {
			public void run() {
				if (running) {
					blankTrack.write(blank, 0, blank.length);
				}
				handler.postDelayed(this, 1000);
			}
		}).start();
	}
	
	public void setBlock(boolean block) {
		this.block = block;
	}
	
}