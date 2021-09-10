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

package androidx.camera.video.internal.compat;

import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.concurrent.Executor;

/**
 * Helper class to avoid verification errors for methods introduced in Android 10.0 (API 29).
 */
@RequiresApi(29)
public final class Api29Impl {

    private Api29Impl() {
    }

    /**
     * Registers a {@link android.media.AudioManager.AudioRecordingCallback} to a
     * {@link AudioRecord}.
     */
    @DoNotInline
    public static void registerAudioRecordingCallback(@NonNull AudioRecord audioRecord,
            @NonNull Executor executor,
            @NonNull AudioManager.AudioRecordingCallback callback) {
        audioRecord.registerAudioRecordingCallback(executor, callback);
    }

    /**
     * Unregisters a {@link android.media.AudioManager.AudioRecordingCallback} previously
     * registered from a {@link AudioRecord}.
     */
    @DoNotInline
    public static void unregisterAudioRecordingCallback(@NonNull AudioRecord audioRecord,
            @NonNull AudioManager.AudioRecordingCallback callback) {
        audioRecord.unregisterAudioRecordingCallback(callback);
    }

    /**
     * Checks whether a {@link AudioRecordingConfiguration} shows that the client is silenced.
     */
    @DoNotInline
    public static boolean isClientSilenced(@NonNull AudioRecordingConfiguration configuration) {
        return configuration.isClientSilenced();
    }
}
