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

package androidx.camera.video.internal.encoder;

import android.media.MediaCodecInfo;
import android.util.Range;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.camera.core.Logger;

import java.util.Objects;

/**
 * VideoEncoderInfoImpl provides video encoder related information and capabilities.
 *
 * <p>The implementation wraps and queries {@link MediaCodecInfo} relevant capability classes
 * such as {@link MediaCodecInfo.CodecCapabilities}, {@link MediaCodecInfo.EncoderCapabilities}
 * and {@link MediaCodecInfo.VideoCapabilities}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoEncoderInfoImpl extends EncoderInfoImpl implements VideoEncoderInfo {
    private static final String TAG = "VideoEncoderInfoImpl";

    /**
     * A default implementation of the VideoEncoderInfoImpl finder.
     *
     * <p>The function will return {@code null} if it can't find a VideoEncoderInfoImpl.
     */
    @NonNull
    public static final Function<VideoEncoderConfig, VideoEncoderInfo> FINDER =
            videoEncoderConfig -> {
                try {
                    return from(videoEncoderConfig);
                } catch (InvalidConfigException e) {
                    Logger.w(TAG, "Unable to find a VideoEncoderInfoImpl", e);
                    return null;
                }
            };

    private final MediaCodecInfo.VideoCapabilities mVideoCapabilities;
    /**
     * Returns a VideoEncoderInfoImpl from a VideoEncoderConfig.
     *
     * <p>The input VideoEncoderConfig is used to find the corresponding encoder.
     *
     * @throws InvalidConfigException if the encoder is not found.
     */
    @NonNull
    public static VideoEncoderInfoImpl from(@NonNull VideoEncoderConfig encoderConfig)
            throws InvalidConfigException {
        return new VideoEncoderInfoImpl(findCodecAndGetCodecInfo(encoderConfig),
                encoderConfig.getMimeType());
    }

    VideoEncoderInfoImpl(@NonNull MediaCodecInfo codecInfo, @NonNull String mime)
            throws InvalidConfigException {
        super(codecInfo, mime);
        mVideoCapabilities = Objects.requireNonNull(mCodecCapabilities.getVideoCapabilities());
    }

    @Override
    public boolean isSizeSupported(int width, int height) {
        return mVideoCapabilities.isSizeSupported(width, height);
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedWidths() {
        return mVideoCapabilities.getSupportedWidths();
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedHeights() {
        return mVideoCapabilities.getSupportedHeights();
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedWidthsFor(int height) {
        try {
            return mVideoCapabilities.getSupportedWidthsFor(height);
        } catch (Throwable t) {
            throw toIllegalArgumentException(t);
        }
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedHeightsFor(int width) {
        try {
            return mVideoCapabilities.getSupportedHeightsFor(width);
        } catch (Throwable t) {
            throw toIllegalArgumentException(t);
        }
    }

    @Override
    public int getWidthAlignment() {
        return mVideoCapabilities.getWidthAlignment();
    }

    @Override
    public int getHeightAlignment() {
        return mVideoCapabilities.getHeightAlignment();
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedBitrateRange() {
        return mVideoCapabilities.getBitrateRange();
    }

    @NonNull
    private static IllegalArgumentException toIllegalArgumentException(@NonNull Throwable t) {
        if (t instanceof IllegalArgumentException) {
            return (IllegalArgumentException) t;
        } else {
            return new IllegalArgumentException(t);
        }
    }
}
