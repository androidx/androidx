/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video.internal.audio;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/** Factory class to create {@link AudioStream}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
interface AudioStreamFactory {

    /**
     * Factory method to create the AudioStream.
     *
     * @param audioSettings      the audio settings.
     * @param attributionContext the attribution context.
     * @return AudioStream
     * @throws AudioStream.AudioStreamException if it fails to create the AudioStream.
     */
    @NonNull
    AudioStream create(@NonNull AudioSettings audioSettings, @Nullable Context attributionContext)
            throws AudioStream.AudioStreamException;
}
