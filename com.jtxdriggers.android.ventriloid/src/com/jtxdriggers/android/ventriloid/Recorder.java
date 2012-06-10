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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class Recorder {

	private VentriloidService s;
	private boolean stop = false;
	private double thresh;
	private int rate;
	private int bufferSize;
	
	public Recorder(VentriloidService s) {
		this.s = s;
	}

	private Runnable r = new Runnable() {
		public void run() {
			AudioRecord audiorecord = null;
			byte[] buffer = null;
			
			if (rate != 48000 && bufferSize < VentriloInterface.pcmlengthforrate(rate)) {
				bufferSize = VentriloInterface.pcmlengthforrate(rate);
			}

			audiorecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC,
					rate,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					bufferSize);
			
			try {
				audiorecord.startRecording();
			} catch (IllegalStateException e) {
				VentriloInterface.stopaudio();
				audiorecord.release();
				return;
			}
			buffer = new byte[bufferSize];
			long timePassed = -1;
			while (true) {
		        for (int offset = 0, read = 0; offset < bufferSize; offset += read) {
		        	if (stop) {
		        		VentriloInterface.stopaudio();
		        		audiorecord.stop();
		        		audiorecord.release();
		        		return;
		        	}
		        	if (!stop && (read = audiorecord.read(buffer, offset, bufferSize - offset)) < 0) {
		        		throw new RuntimeException("AudioRecord read failed: " + Integer.toString(read));
		        	}
		        }
				long start = System.currentTimeMillis();
		        if (!stop) {
		        	if (timePassed < 0) {
		        		if (calcDb(buffer, 0, bufferSize) > -thresh) {
							VentriloInterface.startaudio((short)0);
							s.setXmit(true);
							VentriloInterface.sendaudio(buffer, bufferSize, rate);
							timePassed += (System.currentTimeMillis() - start + 1);
		        		}
			        } else if (timePassed == 0) {
			        	if (calcDb(buffer, 0, bufferSize) > -thresh - .1) {
			        		VentriloInterface.sendaudio(buffer, bufferSize, rate);
			        		timePassed += (System.currentTimeMillis() - start);
			        	} else {
			        		VentriloInterface.stopaudio();
			        		s.setXmit(false);
			        		timePassed = -1;
			        	}
			        // If recording, check to see if the user is done talking every .75 seconds or so.
			        } else if (timePassed < 750) {
			        	VentriloInterface.sendaudio(buffer, bufferSize, rate);
		        		timePassed += (System.currentTimeMillis() - start);
			        } else {
			        	VentriloInterface.sendaudio(buffer, bufferSize, rate);
			        	timePassed = 0;
			        }
		        }
			}
		}
	};

	private int getBufferSize() {
		if (rate == 48000) {
			return AudioRecord.getMinBufferSize(
					48000,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
		}
		final int[] rates = { 8000, 11025, 16000, 22050, 32000, 44100 };
		for (int i = 0; i < rates.length; i++) {
			if (rates[i] != rate) {
				int buffer = 0;
				for (int j = i; j < rates.length; j++) {
					buffer = AudioRecord.getMinBufferSize(
							rates[j],
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					if (buffer > 0 && buffer <= VentriloInterface.pcmlengthforrate(rates[j])) {
						if (rates[j] != rate)
							rate = rates[j];
						return buffer;
					}
				}
				for (int j = i - 1; j >= 0; j--) {
					buffer = AudioRecord.getMinBufferSize(
							rates[j],
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					if (buffer > 0 && buffer <= VentriloInterface.pcmlengthforrate(rates[j])) {
						if (rates[j] != rate) {
							rate = rates[j];
						}
						return buffer;
					}
				}
				break;
			}
		}
		return 0;
	}

	public boolean start(double threshold, int rate) {	
		this.rate = rate;
		thresh = threshold;
		
		if (rate <= 0)
			return true;
		
		if ((bufferSize = getBufferSize()) <= 0)
			return false;
		
		stop = false;
		new Thread(r).start();
		return true;
	}

	public void stop() {
		stop = true;
	}
	
	public double calcDb(byte[] data, int off, int samples) {
        double sum = 0;
        double sqsum = 0;
        for (int i = 0; i < samples; i++) {
            final long v = data[off + i];
            sum += v;
            sqsum += v * v;
        }
        
        double power = (sqsum - sum * sum / samples) / samples;
        power /= 32768 * 32768;

        return Math.log10(power) * 10f + 0.6f;
	}

}