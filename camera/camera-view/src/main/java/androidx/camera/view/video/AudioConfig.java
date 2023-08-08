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

package androidx.camera.view.video;

import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

/**
 * A class providing configuration for audio settings in the video recording.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class AudioConfig {

    /**
     * The audio configuration with audio disabled.
     */
    @NonNull
    public static final AudioConfig AUDIO_DISABLED = new AudioConfig(false);

    private final boolean mIsAudioEnabled;

    AudioConfig(boolean audioEnabled) {
        mIsAudioEnabled = audioEnabled;
    }

    /**
     * Creates a default {@link AudioConfig} with the given audio enabled state.
     *
     * <p> The {@link android.Manifest.permission#RECORD_AUDIO} permission is required to
     * enable audio in video recording; for the use cases where audio is always disabled, please
     * use {@link AudioConfig#AUDIO_DISABLED} instead, which has no permission requirements.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @NonNull
    public static AudioConfig create(boolean enableAudio) {
        return new AudioConfig(enableAudio);
    }

    /**
     * Get the audio enabled state.
     */
    public boolean getAudioEnabled() {
        return mIsAudioEnabled;
    }
}
