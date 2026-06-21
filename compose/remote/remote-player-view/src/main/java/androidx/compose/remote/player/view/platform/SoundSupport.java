/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.remote.player.view.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.operations.utilities.ToneSynthesizer;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides low-latency sound-effect playback for RemoteCompose using {@link AudioTrack}.
 *
 * <p>Uses {@link AudioTrack#MODE_STATIC} with {@code PERFORMANCE_MODE_LOW_LATENCY} to route
 * audio through Android's fast mixer path. Call {@link #init(Context)} once during view
 * construction so that PCM is synthesized (or resampled) to the device's native sample rate,
 * which is required for fast mixer eligibility.
 */
@RestrictTo(LIBRARY_GROUP)
public class SoundSupport {

    private static final String TAG = "SoundSupport";

    /** SC-format header magic bytes. */
    private static final byte SC_MAGIC_0 = 0x53; // 'S'
    private static final byte SC_MAGIC_1 = 0x43; // 'C'
    private static final int SC_HEADER_SIZE = 11;

    /** WAV header size for standard 16-bit PCM. */
    private static final int WAV_HEADER_SIZE = 44;

    /** Native output sample rate; set in {@link #init(Context)} via AudioManager. */
    private int mNativeSampleRate = ToneSynthesizer.SAMPLE_RATE;

    private final AudioAttributes mAudioAttrs = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

    /** Maps sound ID → pre-loaded AudioTrack. */
    private final Map<Integer, AudioTrack> mTracks = new HashMap<>();

    /**
     * Read the device's native output sample rate from {@link AudioManager}. Must be called
     * during view construction (e.g. from {@code RemoteComposePlayer.init()}) so that any
     * subsequent sound loads use the correct rate for fast-mixer eligibility.
     */
    public void init(@NonNull Context context) {
        AudioManager am = context.getSystemService(AudioManager.class);
        if (am != null) {
            String rateStr = am.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            if (rateStr != null) {
                try {
                    mNativeSampleRate = Integer.parseInt(rateStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    /**
     * Build a {@link CoreDocument.SoundEngine} that delegates to this instance.
     */
    public CoreDocument.@NonNull SoundEngine buildEngine() {
        return new CoreDocument.SoundEngine() {
            @Override
            public void loadSound(int soundId, byte @NonNull [] data) {
                SoundSupport.this.loadSound(soundId, data);
            }

            @Override
            public void playSound(int soundId) {
                SoundSupport.this.playSound(soundId);
            }
        };
    }

    /**
     * Decode sound data and pre-load it into an {@link AudioTrack}. Accepts WAV bytes (from
     * {@link ToneSynthesizer}) or SC-format bytes (from
     * {@link androidx.compose.remote.core.operations.SoundData}).
     * PCM is resampled to the device's native rate if it differs.
     */
    public void loadSound(int soundId, byte @NonNull [] data) {
        byte[] pcm;
        int sampleRate;
        int bitDepth;
        int channels;

        if (data.length >= 4
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F') {
            if (data.length <= WAV_HEADER_SIZE) {
                Log.w(TAG, "loadSound: WAV too short for id=" + soundId);
                return;
            }
            // Parse WAV fmt chunk fields.
            channels = (data[22] & 0xFF) | ((data[23] & 0xFF) << 8);
            sampleRate = (data[24] & 0xFF) | ((data[25] & 0xFF) << 8)
                    | ((data[26] & 0xFF) << 16) | ((data[27] & 0xFF) << 24);
            bitDepth = (data[34] & 0xFF) | ((data[35] & 0xFF) << 8);
            pcm = new byte[data.length - WAV_HEADER_SIZE];
            System.arraycopy(data, WAV_HEADER_SIZE, pcm, 0, pcm.length);
        } else if (data.length >= SC_HEADER_SIZE
                && data[0] == SC_MAGIC_0 && data[1] == SC_MAGIC_1) {
            bitDepth = data[3] & 0xFF;
            channels = data[4] & 0xFF;
            sampleRate = ((data[5] & 0xFF) << 8) | (data[6] & 0xFF);
            int sampleCount = ((data[7] & 0xFF) << 24) | ((data[8] & 0xFF) << 16)
                    | ((data[9] & 0xFF) << 8) | (data[10] & 0xFF);
            int sampleBytes = sampleCount * (bitDepth / 8) * channels;
            if (data.length < SC_HEADER_SIZE + sampleBytes) {
                Log.w(TAG, "loadSound: truncated SC data for id=" + soundId);
                return;
            }
            pcm = new byte[sampleBytes];
            System.arraycopy(data, SC_HEADER_SIZE, pcm, 0, sampleBytes);
        } else {
            Log.w(TAG, "loadSound: unrecognized format for id=" + soundId);
            return;
        }

        // Resample 16-bit mono PCM to the device's native rate for fast-mixer eligibility.
        if (sampleRate != mNativeSampleRate && bitDepth == 16 && channels == 1) {
            pcm = resample16BitMono(pcm, sampleRate, mNativeSampleRate);
            sampleRate = mNativeSampleRate;
        }

        loadPcm(soundId, pcm, sampleRate, bitDepth, channels);
    }

    /**
     * Trigger immediate playback of the sound pre-loaded under {@code soundId}.
     */
    public void playSound(int soundId) {
        AudioTrack track = mTracks.get(soundId);
        if (track == null) {
            Log.w(TAG, "playSound: no sound loaded for id=" + soundId);
            return;
        }
        if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            track.stop();
            if (track.reloadStaticData() != AudioTrack.SUCCESS) {
                Log.w(TAG, "playSound: reloadStaticData failed for id=" + soundId);
                return;
            }
        }
        track.play();
    }

    /** Release all AudioTrack resources. */
    public void release() {
        for (AudioTrack track : mTracks.values()) {
            track.release();
        }
        mTracks.clear();
    }

    // ---- internal helpers -------------------------------------------------------

    private void loadPcm(int id, byte @NonNull [] pcm, int sampleRate, int bitDepth, int channels) {
        int encoding = bitDepth == 8
                ? AudioFormat.ENCODING_PCM_8BIT
                : AudioFormat.ENCODING_PCM_16BIT;
        int channelMask = channels == 2
                ? AudioFormat.CHANNEL_OUT_STEREO
                : AudioFormat.CHANNEL_OUT_MONO;

        int minBuf = AudioTrack.getMinBufferSize(sampleRate, channelMask, encoding);
        int bufSize = Math.max(minBuf, pcm.length);

        AudioTrack.Builder builder = new AudioTrack.Builder()
                .setAudioAttributes(mAudioAttrs)
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(encoding)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelMask)
                        .build())
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STATIC);

        builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);

        AudioTrack track = builder.build();
        int written = track.write(pcm, 0, pcm.length);
        if (written != pcm.length) {
            Log.w(TAG, "loadPcm: wrote " + written + "/" + pcm.length + " for id=" + id);
            track.release();
            return;
        }

        AudioTrack old = mTracks.put(id, track);
        if (old != null) old.release();
    }

    /**
     * Linear-interpolation resample for 16-bit mono PCM.
     */
    private static byte @NonNull [] resample16BitMono(
            byte @NonNull [] src, int fromRate, int toRate) {
        int srcSamples = src.length / 2;
        int dstSamples = (int) ((long) srcSamples * toRate / fromRate);
        byte[] dst = new byte[dstSamples * 2];
        for (int i = 0; i < dstSamples; i++) {
            double pos = (double) i * fromRate / toRate;
            int idx = (int) pos;
            double frac = pos - idx;
            int s0 = (idx < srcSamples)
                    ? (short) ((src[idx * 2] & 0xFF) | (src[idx * 2 + 1] << 8)) : 0;
            int s1 = (idx + 1 < srcSamples)
                    ? (short) ((src[(idx + 1) * 2] & 0xFF) | (src[(idx + 1) * 2 + 1] << 8)) : 0;
            short out = (short) (s0 + frac * (s1 - s0));
            dst[i * 2] = (byte) (out & 0xFF);
            dst[i * 2 + 1] = (byte) ((out >> 8) & 0xFF);
        }
        return dst;
    }
}
