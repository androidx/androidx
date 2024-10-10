/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.media;

import static android.Manifest.permission.RECORD_AUDIO;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;

import org.jspecify.annotations.NonNull;

/**
 * A {@link CarAudioRecord} for automotive OS.
 *
 */
@RestrictTo(LIBRARY_GROUP)
@KeepFields
@CarProtocol
public class AutomotiveCarAudioRecord extends CarAudioRecord {
    /**
     * Only used for Automotive, as the car microphone is the device microphone.
     */
    private final @NonNull AudioRecord mAudioRecord;

    @RequiresPermission(RECORD_AUDIO)
    public AutomotiveCarAudioRecord(
            @NonNull CarContext carContext) {
        super(carContext);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_CONTENT_SAMPLING_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_DEFAULT, AUDIO_CONTENT_BUFFER_SIZE);
    }

    @Override
    protected void startRecordingInternal(
            @NonNull OpenMicrophoneResponse openMicrophoneResponse) {
        mAudioRecord.startRecording();
    }

    @Override
    protected void stopRecordingInternal() {
        mAudioRecord.stop();
    }

    @Override
    protected int readInternal(byte @NonNull [] audioData, int offsetInBytes, int sizeInBytes) {
        return mAudioRecord.read(audioData, offsetInBytes, sizeInBytes);
    }
}
