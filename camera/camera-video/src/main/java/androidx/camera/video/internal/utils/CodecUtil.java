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

package androidx.camera.video.internal.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.video.internal.encoder.EncoderConfig;
import androidx.camera.video.internal.encoder.InvalidConfigException;

import java.io.IOException;

/** A codec utility class to deal with codec operations. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class CodecUtil {

    private CodecUtil() {
        // Prevent instantiation.
    }

    /**
     * Creates a codec instance suitable for the encoder config.
     *
     * @throws InvalidConfigException if it fails create the codec.
     */
    @NonNull
    public static MediaCodec createCodec(@NonNull EncoderConfig encoderConfig)
            throws InvalidConfigException {
        try {
            return MediaCodec.createEncoderByType(encoderConfig.getMimeType());
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidConfigException(e);
        }
    }

    /**
     * Finds and creates a codec info instance suitable for the encoder config.
     *
     * @throws InvalidConfigException if it fails to find or create the codec info.
     */
    @NonNull
    public static MediaCodecInfo findCodecAndGetCodecInfo(@NonNull EncoderConfig encoderConfig)
            throws InvalidConfigException {
        MediaCodec codec = null;
        try {
            codec = createCodec(encoderConfig);
            return codec.getCodecInfo();
        } finally {
            if (codec != null) {
                codec.release();
            }
        }
    }
}
