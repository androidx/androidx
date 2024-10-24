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
import static androidx.car.app.utils.LogTags.TAG;

import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.annotation.RestrictTo;
import androidx.car.app.CarContext;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.KeepFields;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link CarAudioRecord} for projection.
 *
 */
@RestrictTo(LIBRARY_GROUP)
@KeepFields
@CarProtocol
public class ProjectedCarAudioRecord extends CarAudioRecord {
    private @Nullable InputStream mInputStream;

    @RequiresPermission(RECORD_AUDIO)
    public ProjectedCarAudioRecord(
            @NonNull CarContext carContext) {
        super(carContext);
    }

    @Override
    protected void startRecordingInternal(@NonNull OpenMicrophoneResponse openMicrophoneResponse) {
        mInputStream = openMicrophoneResponse.getCarMicrophoneInputStream();
    }

    @Override
    protected void stopRecordingInternal() {
        try {
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Exception closing microphone pipe", e);
        }
    }

    @Override
    protected int readInternal(byte @NonNull [] audioData, int offsetInBytes, int sizeInBytes) {
        InputStream inputStream = mInputStream;

        if (inputStream != null) {
            try {
                return inputStream.read(audioData, offsetInBytes, sizeInBytes);
            } catch (IOException e) {
                // stream is closed
                stopRecording();
            }
        }
        return -1;
    }
}
