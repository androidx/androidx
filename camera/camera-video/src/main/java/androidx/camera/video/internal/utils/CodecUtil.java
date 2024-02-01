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
import android.util.LruCache;

import androidx.annotation.GuardedBy;
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

    private static final int MAX_CODEC_INFO_CACHE_COUNT = 10;
    // A cache from mimeType to MediaCodecInfo.
    // This cache is created because MediaCodec.createEncoderByType() take relatively long time and
    // findCodecAndGetCodecInfo() is being called frequently in camera-video.
    @GuardedBy("sCodecInfoCache")
    private static final LruCache<String, MediaCodecInfo> sCodecInfoCache = new LruCache<>(
            MAX_CODEC_INFO_CACHE_COUNT);

    /**
     * Creates a codec instance suitable for the encoder config.
     *
     * @throws InvalidConfigException if it fails create the codec.
     */
    @NonNull
    public static MediaCodec createCodec(@NonNull EncoderConfig encoderConfig)
            throws InvalidConfigException {
        return createCodec(encoderConfig.getMimeType());
    }

    /**
     * Finds and creates a codec info instance suitable for the encoder config.
     *
     * @throws InvalidConfigException if it fails to find or create the codec info.
     */
    @NonNull
    public static MediaCodecInfo findCodecAndGetCodecInfo(@NonNull EncoderConfig encoderConfig)
            throws InvalidConfigException {
        String mimeType = encoderConfig.getMimeType();
        MediaCodecInfo codecInfo;
        synchronized (sCodecInfoCache) {
            codecInfo = sCodecInfoCache.get(mimeType);
        }
        if (codecInfo != null) {
            return codecInfo;
        }
        MediaCodec codec = null;
        try {
            codec = createCodec(mimeType);
            codecInfo = codec.getCodecInfo();
            synchronized (sCodecInfoCache) {
                sCodecInfoCache.put(mimeType, codecInfo);
            }
            return codecInfo;
        } finally {
            if (codec != null) {
                codec.release();
            }
        }
    }

    @NonNull
    private static MediaCodec createCodec(@NonNull String mimeType) throws InvalidConfigException {
        try {
            return MediaCodec.createEncoderByType(mimeType);
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidConfigException(e);
        }
    }
}
