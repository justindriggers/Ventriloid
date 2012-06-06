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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

public class Recorder {

	private static VentriloidService s;
	private static Thread thread = null; // limited to only one recording thread
	private static boolean stop = false; // stop flag
	private static int rate = 0; // current or overridden rate for current channel
	private static int buflen;
	private static double thresh;
	private static boolean force_8khz;

	private static class RecordThread implements Runnable {
		public void run() {
			AudioRecord audiorecord = null;
			byte[] buf = null; // send buffer
			
			Log.e("recorder", "lv3 says " + VentriloInterface.pcmlengthforrate(rate) + " for rate " + rate);
			if (rate != 48000 && buflen < VentriloInterface.pcmlengthforrate(rate)) {
				Log.e("debug", "setting buffer length to " + VentriloInterface.pcmlengthforrate(rate));
				buflen = VentriloInterface.pcmlengthforrate(rate);
			}
			Log.e("recorder", "buflen is " + buflen);
			
			// Find out if the minimum buffer length is smaller
			// than the amount of data we need to send.  If so,
			// adjust buflen (set from buffer()) accordingly

			Log.e("mangler", "starting audio record");
			audiorecord = new AudioRecord(
					MediaRecorder.AudioSource.MIC,
					rate,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					buflen);
			try {
				audiorecord.startRecording();
			}
			catch (IllegalStateException e) {
				VentriloInterface.stopaudio();
				audiorecord.release();
				thread = null;
				return;
			}
			buf = new byte[buflen];
			int bitsProcessed = -1;
			while (true) {
		        for (int offset = 0, read = 0; offset < buflen; offset += read) {
		        	if (stop) {
		        		VentriloInterface.stopaudio();
		        		audiorecord.stop();
		        		audiorecord.release();
		        		thread = null;
		        		return;
		        	}
		        	if (!stop && (read = audiorecord.read(buf, offset, buflen - offset)) < 0) {
		        		throw new RuntimeException("AudioRecord read failed: " + Integer.toString(read));
		        	}
		        }
		        if (!stop) {
		        	if (bitsProcessed < 0) {
		        		if (calcDb(buf, 0, buf.length) > -thresh) {
							VentriloInterface.startaudio((short)0);
							s.setXmit(true);
							VentriloInterface.sendaudio(buf, buflen, rate);
							bitsProcessed += buflen;
		        		}
			        } else if (bitsProcessed == 0) {
			        	if (calcDb(buf, 0, buf.length) > -thresh) {
			        		VentriloInterface.sendaudio(buf, buflen, rate);
			        		bitsProcessed += buflen;
			        	} else {
			        		VentriloInterface.stopaudio();
			        		s.setXmit(false);
			        		bitsProcessed = -1;
			        	}
			        	// Figure out this equation instead of using a number!
			        } else if (bitsProcessed < buflen * 8) {
			        	VentriloInterface.sendaudio(buf, buflen, rate);
			        	bitsProcessed += buflen;
			        } else {
			        	VentriloInterface.sendaudio(buf, buflen, rate);
			        	bitsProcessed = 0;
			        }
		        }
			}
		}
	}

	private static int buffer() {
		// all rates used by the protocol
		Log.e("mangler", "checking available buffer sizes");
		if (isForce_8khz()) {
			rate(8000);
			return AudioRecord.getMinBufferSize(
					8000,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
		}
		if (rate == 48000) {
			return AudioRecord.getMinBufferSize(
					48000,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT);
		}
		final int[] rates = { 8000, 11025, 16000, 22050, 32000, 44100 };
		for (int cur = 0; cur < rates.length; cur++) {
			// find the current rate in the rates array
			if (rates[cur] != rate) {
				int buffer = 0;
				// try current and higher rates
				for (int ctr = cur; ctr < rates.length; ctr++) {
					Log.d("mangler", "checking rate: " + rates[ctr]);
					buffer = AudioRecord.getMinBufferSize(
							rates[ctr],
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					Log.d("mangler", "getMinBufferSize returned: " + buffer);
					if (buffer > 0 && buffer <= VentriloInterface.pcmlengthforrate(rates[ctr])) {
						// found a supported rate
						// override if it is not the channel rate and use the resampler
						if (rates[ctr] != rate) {
							Log.e("mangler", "" + rates[ctr] + " -- buffer: " + buffer + " - pcmlen: " + VentriloInterface.pcmlengthforrate(rates[ctr]));
							rate(rates[ctr]);
						}
						return buffer;
					}
				}
				// else try lower rates than current
				for (int ctr = cur - 1; ctr >= 0; ctr--) {
					Log.d("mangler", "checking rate: " + rates[ctr]);
					buffer = AudioRecord.getMinBufferSize(
							rates[ctr],
							AudioFormat.CHANNEL_CONFIGURATION_MONO,
							AudioFormat.ENCODING_PCM_16BIT);
					if (buffer > 0 && buffer <= VentriloInterface.pcmlengthforrate(rates[ctr])) {
						if (rates[ctr] != rate) {
							Log.e("mangler", "" + rates[ctr] + " -- buffer: " + buffer + " - pcmlen: " + VentriloInterface.pcmlengthforrate(rates[ctr]));
							rate(rates[ctr]);
						}
						return buffer;
					}
				}
				// else break and return 0
				break;
			}
		}
		return 0;
	}

	public static void rate(final int _rate) {
		rate = Build.PRODUCT.equals("sdk") ? 8000 : _rate;
	}

	public static int rate() {
		return rate;
	}
	
	public static int buflen() {
		return buflen;
	}

	public static boolean recording() {
		// if a recording thread is running, we can't instantiate another one
		return thread != null;
	}

	public static boolean start(VentriloidService service, double threshold) {
		s = service;
		thresh = threshold;
		if (recording() || rate <= 0) {
			return true;
		}
		// find a supported rate
		if ((buflen = buffer()) <= 0) {
			return false;
		}
		stop = false;
		(thread = new Thread(new RecordThread())).start();
		return true;
	}

	public static void stop() {
		stop = true;
	}

	public static void setForce_8khz(boolean force_8khz) {
		Log.e("recorder", "forcing 8khz: " + force_8khz);
		Recorder.force_8khz = force_8khz;
	}

	public static boolean isForce_8khz() {
		return force_8khz;
	}
	
	public static double calcDb(byte[] data, int off, int samples) {
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