/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.testing;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

/** Utility for audio related functions. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class AudioUtil {
    private static final String TAG = "AudioUtil";

    // Guarantee supported settings.
    private static final int COMMON_SAMPLE_RATE = 44100;
    private static final int COMMON_CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int COMMON_AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioUtil() {
    }

    /**
     * Test if an AudioRecord with an audio source id can be started.
     *
     * <p>Use the most common settings to start AudioRecord. The failed attempt indicates that
     * the audio source might be occupied by another app or be in a error state.
     *
     * @see AudioRecord
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public static boolean canStartAudioRecord(int audioSource) {
        int minBufferSize = AudioRecord.getMinBufferSize(COMMON_SAMPLE_RATE, COMMON_CHANNEL_CONFIG,
                COMMON_AUDIO_FORMAT);
        if (minBufferSize <= 0) {
            Log.w(TAG, "Unable to get the minimum buffer size of AudioRecord.");
            return false;
        }

        AudioRecord audioRecord = null;
        try {
            try {
                audioRecord = new AudioRecord(audioSource, COMMON_SAMPLE_RATE,
                        COMMON_CHANNEL_CONFIG, COMMON_AUDIO_FORMAT, minBufferSize * 2);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "AudioRecord can't be created.", e);
                return false;
            }

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "AudioRecord can't be initialized.");
                return false;
            }

            audioRecord.startRecording();
            if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                Log.w(TAG, "AudioRecord can't be started.");
                return false;
            }
        } finally {
            if (audioRecord != null) {
                audioRecord.release();
            }
        }
        return true;
    }
}
