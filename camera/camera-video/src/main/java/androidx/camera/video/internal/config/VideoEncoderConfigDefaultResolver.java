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
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace;
import androidx.camera.video.internal.utils.DynamicRangeUtil;
import androidx.core.util.Supplier;

import java.util.Objects;

/**
 * A {@link VideoEncoderConfig} supplier that resolves requested encoder settings from a
 * {@link VideoSpec} for the given surface {@link Size} using pre-defined default values.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoEncoderConfigDefaultResolver implements Supplier<VideoEncoderConfig> {

    private static final String TAG = "VidEncCfgDefaultRslvr";

    // Base config based on generic 720p H264 quality will be scaled by actual source settings.
    // TODO: These should vary based on quality/codec and be derived from actual devices
    private static final int VIDEO_BITRATE_BASE = 14000000;
    private static final Size VIDEO_SIZE_BASE = new Size(1280, 720);
    private static final int VIDEO_FRAME_RATE_BASE = 30;
    static final int VIDEO_FRAME_RATE_FIXED_DEFAULT = 30;
    private static final int VIDEO_BIT_DEPTH_BASE = 8;
    private static final Range<Integer> VALID_FRAME_RATE_RANGE = new Range<>(1, 60);

    private final String mMimeType;

    private final Timebase mInputTimebase;
    private final VideoSpec mVideoSpec;
    private final Size mSurfaceSize;
    private final DynamicRange mDynamicRange;
    private final Range<Integer> mExpectedFrameRateRange;

    /**
     * Constructor for a VideoEncoderConfigDefaultResolver.
     *
     * @param mimeType               The mime type for the video encoder
     * @param inputTimebase          The time base of the input frame.
     * @param videoSpec              The {@link VideoSpec} which defines the settings that should
     *                               be used with the video encoder.
     * @param surfaceSize            The size of the surface required by the camera for the video
     *                               encoder.
     * @param dynamicRange           The dynamic range of input frames.
     * @param expectedFrameRateRange The expected source frame rate range. This should act as an
     *                               envelope for any frame rate calculated from {@code videoSpec
     *                               } and {@code videoProfile} since the source should not
     *                               produce frames at a frame rate outside this range. If
     *                               equal to {@link SurfaceRequest#FRAME_RATE_RANGE_UNSPECIFIED},
     *                               then no information about the source frame rate is available
     *                               and it does not need to be used in calculations.
     */
    public VideoEncoderConfigDefaultResolver(@NonNull String mimeType,
            @NonNull Timebase inputTimebase, @NonNull VideoSpec videoSpec,
            @NonNull Size surfaceSize, @NonNull DynamicRange dynamicRange,
            @NonNull Range<Integer> expectedFrameRateRange) {
        mMimeType = mimeType;
        mInputTimebase = inputTimebase;
        mVideoSpec = videoSpec;
        mSurfaceSize = surfaceSize;
        mDynamicRange = dynamicRange;
        mExpectedFrameRateRange = expectedFrameRateRange;
    }

    @Override
    @NonNull
    public VideoEncoderConfig get() {
        int resolvedFrameRate = resolveFrameRate();
        Logger.d(TAG, "Resolved VIDEO frame rate: " + resolvedFrameRate + "fps");

        Range<Integer> videoSpecBitrateRange = mVideoSpec.getBitrate();
        Logger.d(TAG, "Using fallback VIDEO bitrate");
        // We have no other information to go off of. Scale based on fallback defaults.
        int resolvedBitrate = VideoConfigUtil.scaleAndClampBitrate(
                VIDEO_BITRATE_BASE,
                mDynamicRange.getBitDepth(), VIDEO_BIT_DEPTH_BASE,
                resolvedFrameRate, VIDEO_FRAME_RATE_BASE,
                mSurfaceSize.getWidth(), VIDEO_SIZE_BASE.getWidth(),
                mSurfaceSize.getHeight(), VIDEO_SIZE_BASE.getHeight(),
                videoSpecBitrateRange);

        int resolvedProfile = DynamicRangeUtil.dynamicRangeToCodecProfileLevelForMime(
                mMimeType, mDynamicRange);
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
        // If the operating frame rate range isn't unspecified, we'll use the upper frame rate from
        // as our default in an attempt to maximize the quality of the video. Clamp the value to
        // ensure it's a valid frame rate.
        int resolvedFrameRate;
        if (!Objects.equals(mExpectedFrameRateRange, SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED)) {
            resolvedFrameRate = VALID_FRAME_RATE_RANGE.clamp(mExpectedFrameRateRange.getUpper());
        } else {
            // If the frame rate range is unspecified, return a hard coded common default.
            resolvedFrameRate = VIDEO_FRAME_RATE_FIXED_DEFAULT;
        }

        Logger.d(TAG,
                String.format("Default resolved frame rate: %dfps. [Expected operating range: %s]",
                        resolvedFrameRate, Objects.equals(mExpectedFrameRateRange,
                                SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED)
                                ? mExpectedFrameRateRange : "<UNSPECIFIED>"));

        return resolvedFrameRate;
    }
}
