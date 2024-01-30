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
import androidx.annotation.RequiresApi;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace;
import androidx.core.util.Supplier;

import java.util.Objects;

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
    private final DynamicRange mDynamicRange;
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
     * @param dynamicRange     The dynamic range of input frames.
     * @param expectedFrameRateRange The expected source frame rate range. This should act as an
     *                               envelope for any frame rate calculated from {@code videoSpec}
     *                               and {@code videoProfile} since the source should not
     *                               produce frames at a frame rate outside this range. If
     *                               equal to {@link SurfaceRequest#FRAME_RATE_RANGE_UNSPECIFIED},
     *                               then no information about the source frame rate is available
     *                               and it does not need to be used in calculations.
     */
    public VideoEncoderConfigVideoProfileResolver(@NonNull String mimeType,
            @NonNull Timebase inputTimebase,
            @NonNull VideoSpec videoSpec,
            @NonNull Size surfaceSize,
            @NonNull VideoProfileProxy videoProfile,
            @NonNull DynamicRange dynamicRange,
            @NonNull Range<Integer> expectedFrameRateRange) {
        mMimeType = mimeType;
        mInputTimebase = inputTimebase;
        mVideoSpec = videoSpec;
        mSurfaceSize = surfaceSize;
        mVideoProfile = videoProfile;
        mDynamicRange = dynamicRange;
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
                mDynamicRange.getBitDepth(), mVideoProfile.getBitDepth(),
                resolvedFrameRate, mVideoProfile.getFrameRate(),
                mSurfaceSize.getWidth(), mVideoProfile.getWidth(),
                mSurfaceSize.getHeight(), mVideoProfile.getHeight(),
                videoSpecBitrateRange);

        int resolvedProfile = mVideoProfile.getProfile();
        VideoEncoderDataSpace dataSpace =
                VideoConfigUtil.mimeAndProfileToEncoderDataSpace(mMimeType, resolvedProfile);

        return VideoEncoderConfig.builder()
                .setMimeType(mMimeType)
                .setInputTimebase(mInputTimebase)
                .setResolution(mSurfaceSize)
                .setBitrate(resolvedBitrate)
                .setFrameRate(resolvedFrameRate)
                .setProfile(resolvedProfile)
                .setDataSpace(dataSpace)
                .build();
    }

    private int resolveFrameRate() {
        int videoProfileFrameRate = mVideoProfile.getFrameRate();
        int resolvedFrameRate;
        if (!Objects.equals(mExpectedFrameRateRange, SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED)) {
            resolvedFrameRate = mExpectedFrameRateRange.clamp(videoProfileFrameRate);
        } else {
            resolvedFrameRate = videoProfileFrameRate;
        }

        Logger.d(TAG,
                String.format("Resolved frame rate %dfps [Video profile frame rate: %dfps, "
                                + "Expected operating range: %s]", resolvedFrameRate,
                        videoProfileFrameRate, Objects.equals(mExpectedFrameRateRange,
                                SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED)
                                ? mExpectedFrameRateRange : "<UNSPECIFIED>"));

        return resolvedFrameRate;
    }
}
