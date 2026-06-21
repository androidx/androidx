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
package androidx.compose.remote.core.operations.utilities;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility for synthesizing audio tones as WAV bytes, used by
 * {@link androidx.compose.remote.core.operations.SoundExpression} to produce loadable audio
 * without a platform-specific synthesis implementation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class ToneSynthesizer {

    /** Sample rate used for all synthesized tones. */
    public static final int SAMPLE_RATE = 22050;

    private ToneSynthesizer() {}

    /**
     * Synthesize a tone and return it as a WAV-formatted byte array (44-byte header + 16-bit
     * mono PCM).
     *
     * @param frequency   frequency in Hz
     * @param durationSec duration in seconds
     * @param waveform    waveform kind: 0=sine, 1=square, 2=sawtooth, 3=triangle
     * @return WAV bytes ready for loading into a SoundPool or audio decoder
     */
    public static byte @NonNull [] synthesizeWav(
            float frequency, float durationSec, float waveform) {
        byte[] pcm = synthesizePcm(frequency, durationSec, waveform, SAMPLE_RATE);
        return buildWav(pcm, SAMPLE_RATE, 16, 1);
    }

    /**
     * Synthesize a tone as raw 16-bit mono PCM bytes.
     *
     * @param frequency   frequency in Hz
     * @param durationSec duration in seconds
     * @param waveform    waveform kind: 0=sine, 1=square, 2=sawtooth, 3=triangle
     * @param sampleRate  sample rate in Hz
     * @return raw 16-bit little-endian PCM bytes
     */
    public static byte @NonNull [] synthesizePcm(
            float frequency, float durationSec, float waveform, int sampleRate) {
        int sampleCount = Math.max(1, (int) (sampleRate * durationSec));
        byte[] pcm = new byte[sampleCount * 2]; // 16-bit = 2 bytes/sample
        int waveKind = (int) waveform;
        for (int i = 0; i < sampleCount; i++) {
            double t = (double) i / sampleRate;
            double phase = 2.0 * Math.PI * frequency * t;
            double sample;
            switch (waveKind) {
                case 1: // square
                    sample = Math.sin(phase) >= 0 ? 1.0 : -1.0;
                    break;
                case 2: // sawtooth
                    sample = 2.0 * ((frequency * t) - Math.floor(frequency * t + 0.5));
                    break;
                case 3: // triangle
                    sample = 2.0 * Math.abs(
                            2.0 * ((frequency * t) - Math.floor(frequency * t + 0.5))) - 1.0;
                    break;
                default: // sine
                    sample = Math.sin(phase);
                    break;
            }
            // Short fade at start and end to avoid click artifacts
            double envelope = i < 100 ? i / 100.0
                    : i > sampleCount - 100 ? (sampleCount - i) / 100.0
                    : 1.0;
            short s = (short) (sample * envelope * Short.MAX_VALUE);
            pcm[i * 2] = (byte) (s & 0xFF);
            pcm[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return pcm;
    }

    /**
     * Wrap raw PCM samples in a standard WAV container.
     *
     * @param pcm        raw PCM bytes
     * @param sampleRate sample rate in Hz
     * @param bitDepth   bits per sample (8 or 16)
     * @param channels   number of channels (1=mono, 2=stereo)
     * @return WAV-formatted bytes
     */
    public static byte @NonNull [] buildWav(
            byte @NonNull [] pcm, int sampleRate, int bitDepth, int channels) {
        int byteRate = sampleRate * channels * (bitDepth / 8);
        int blockAlign = channels * (bitDepth / 8);
        int dataSize = pcm.length;
        ByteBuffer buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(new byte[]{'R', 'I', 'F', 'F'});
        buf.putInt(36 + dataSize);
        buf.put(new byte[]{'W', 'A', 'V', 'E'});
        buf.put(new byte[]{'f', 'm', 't', ' '});
        buf.putInt(16);
        buf.putShort((short) 1);           // PCM
        buf.putShort((short) channels);
        buf.putInt(sampleRate);
        buf.putInt(byteRate);
        buf.putShort((short) blockAlign);
        buf.putShort((short) bitDepth);
        buf.put(new byte[]{'d', 'a', 't', 'a'});
        buf.putInt(dataSize);
        buf.put(pcm);
        return buf.array();
    }
}
