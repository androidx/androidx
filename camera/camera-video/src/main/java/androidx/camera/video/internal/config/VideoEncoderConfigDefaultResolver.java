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

import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace;
import androidx.camera.video.internal.utils.DynamicRangeUtil;
import androidx.core.util.Supplier;

import org.jspecify.annotations.NonNull;

/**
 * A {@link VideoEncoderConfig} supplier that resolves requested encoder settings from a
 * {@link VideoSpec} for the given surface {@link Size} using pre-defined default values.
 */
public class VideoEncoderConfigDefaultResolver implements Supplier<VideoEncoderConfig> {

    private static final String TAG = "VidEncCfgDefaultRslvr";

    // Base config based on generic 720p H264 quality will be scaled by actual source settings.
    // TODO: These should vary based on quality/codec and be derived from actual devices
    private static final int VIDEO_BITRATE_BASE = 14000000;
    private static final Size VIDEO_SIZE_BASE = new Size(1280, 720);
    private static final int VIDEO_FRAME_RATE_BASE = 30;
    private static final int VIDEO_BIT_DEPTH_BASE = 8;

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
    public @NonNull VideoEncoderConfig get() {
        CaptureEncodeRates resolvedFrameRates = VideoConfigUtil.resolveFrameRates(mVideoSpec,
                mExpectedFrameRateRange);
        Logger.d(TAG, "Resolved VIDEO frame rates: "
                + "Capture frame rate = " + resolvedFrameRates.getCaptureRate() + "fps. "
                + "Encode frame rate = " + resolvedFrameRates.getEncodeRate() + "fps.");

        Range<Integer> videoSpecBitrateRange = mVideoSpec.getBitrate();
        Logger.d(TAG, "Using fallback VIDEO bitrate");
        // We have no other information to go off of. Scale based on fallback defaults.
        int resolvedBitrate = VideoConfigUtil.scaleAndClampBitrate(
                VIDEO_BITRATE_BASE,
                mDynamicRange.getBitDepth(), VIDEO_BIT_DEPTH_BASE,
                resolvedFrameRates.getEncodeRate(), VIDEO_FRAME_RATE_BASE,
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
                .setCaptureFrameRate(resolvedFrameRates.getCaptureRate())
                .setEncodeFrameRate(resolvedFrameRates.getEncodeRate())
                .setProfile(resolvedProfile)
                .setDataSpace(dataSpace)
                .build();
    }
}
