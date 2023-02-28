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

package androidx.camera.video.internal.config;

import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.core.util.Supplier;

/**
 * A {@link VideoEncoderConfig} supplier that resolves requested encoder settings from a
 * {@link VideoSpec} for the given surface {@link Size} using the provided
 * {@link VideoProfileProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoEncoderConfigVideoProfileResolver implements Supplier<VideoEncoderConfig> {

    private static final String TAG = "VidEncVdPrflRslvr";

    private final String mMimeType;
    private final Timebase mInputTimebase;
    private final VideoSpec mVideoSpec;
    private final Size mSurfaceSize;
    private final VideoProfileProxy mVideoProfile;
    @Nullable
    private final Range<Integer> mExpectedFrameRateRange;

    /**
     * Constructor for a VideoEncoderConfigVideoProfileResolver.
     *
     * @param mimeType         The mime type for the video encoder
     * @param inputTimebase    The timebase of the input frame
     * @param videoSpec        The {@link VideoSpec} which defines the settings that should be
     *                         used with the video encoder.
     * @param surfaceSize      The size of the surface required by the camera for the video encoder.
     * @param videoProfile     The {@link VideoProfileProxy} used to resolve automatic and range
     *                         settings.
     * @param expectedFrameRateRange The expected source frame rate range. This should act as an
     *                               envelope for any frame rate calculated from {@code videoSpec
     *                               } and {@code videoProfile} since the source should not
     *                               produce frames at a frame rate outside this range. If {@code
     *                               null}, then no information about the source frame rate is
     *                               available and it does not need to be used in calculations.
     */
    public VideoEncoderConfigVideoProfileResolver(@NonNull String mimeType,
            @NonNull Timebase inputTimebase,
            @NonNull VideoSpec videoSpec,
            @NonNull Size surfaceSize,
            @NonNull VideoProfileProxy videoProfile,
            @Nullable Range<Integer> expectedFrameRateRange) {
        mMimeType = mimeType;
        mInputTimebase = inputTimebase;
        mVideoSpec = videoSpec;
        mSurfaceSize = surfaceSize;
        mVideoProfile = videoProfile;
        mExpectedFrameRateRange = expectedFrameRateRange;
    }

    @Override
    @NonNull
    public VideoEncoderConfig get() {
        int resolvedFrameRate = resolveFrameRate();
        Logger.d(TAG, "Resolved VIDEO frame rate: " + resolvedFrameRate + "fps");

        Range<Integer> videoSpecBitrateRange = mVideoSpec.getBitrate();
        Logger.d(TAG, "Using resolved VIDEO bitrate from EncoderProfiles");
        int resolvedBitrate = VideoConfigUtil.scaleAndClampBitrate(
                mVideoProfile.getBitrate(),
                resolvedFrameRate, mVideoProfile.getFrameRate(),
                mSurfaceSize.getWidth(), mVideoProfile.getWidth(),
                mSurfaceSize.getHeight(), mVideoProfile.getHeight(),
                videoSpecBitrateRange);

        return VideoEncoderConfig.builder()
                .setMimeType(mMimeType)
                .setInputTimebase(mInputTimebase)
                .setResolution(mSurfaceSize)
                .setBitrate(resolvedBitrate)
                .setFrameRate(resolvedFrameRate)
                .build();
    }

    private int resolveFrameRate() {
        Range<Integer> videoSpecFrameRateRange = mVideoSpec.getFrameRate();
        int videoProfileFrameRate = mVideoProfile.getFrameRate();
        Logger.d(TAG,
                String.format("Frame rate from video profile: %dfps. [Requested range: %s, "
                        + "Expected operating range: %s]", videoProfileFrameRate,
                        videoSpecFrameRateRange, mExpectedFrameRateRange));

        return VideoConfigUtil.resolveFrameRate(
                /*preferredRange=*/ videoSpecFrameRateRange,
                /*exactFrameRateHint=*/ videoProfileFrameRate,
                /*strictOperatingFpsRange=*/mExpectedFrameRateRange);
    }
}
