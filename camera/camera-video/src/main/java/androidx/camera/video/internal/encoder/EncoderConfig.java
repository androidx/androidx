/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.encoder;

import android.media.MediaFormat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * The configuration represents the required parameters to configure an encoder.
 *
 * <p>An {@code EncoderConfig} is used to configure an {@link Encoder}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface EncoderConfig {
    /**
     * The mime type of the encoder.
     *
     * <p>For example, "video/avc" for a video encoder and "audio/mp4a-latm" for an audio encoder.
     *
     * @see {@link MediaFormat}
     */
    @NonNull
    String getMimeType();

    /**
     * Transfers the config to a {@link MediaFormat}.
     *
     * @return the result {@link MediaFormat}
     */
    @NonNull
    MediaFormat toMediaFormat() throws InvalidConfigException;
}
