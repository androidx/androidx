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
import androidx.camera.core.Logger;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.core.util.Supplier;

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

    private final String mMimeType;
    private final VideoSpec mVideoSpec;
    private final Size mSurfaceSize;

    /**
     * Constructor for a VideoEncoderConfigDefaultResolver.
     *
     * @param mimeType    The mime type for the video encoder
     * @param videoSpec   The {@link VideoSpec} which defines the settings that should be used with
     *                    the video encoder.
     * @param surfaceSize The size of the surface required by the camera for the video encoder.
     */
    public VideoEncoderConfigDefaultResolver(@NonNull String mimeType,
            @NonNull VideoSpec videoSpec, @NonNull Size surfaceSize) {
        mMimeType = mimeType;
        mVideoSpec = videoSpec;
        mSurfaceSize = surfaceSize;
    }

    @Override
    @NonNull
    public VideoEncoderConfig get() {
        int resolvedFrameRate = VideoConfigUtil.resolveFrameRate(mVideoSpec);

        Range<Integer> videoSpecBitrateRange = mVideoSpec.getBitrate();
        Logger.d(TAG, "Using fallback VIDEO bitrate");
        // We have no other information to go off of. Scale based on fallback defaults.
        int resolvedBitrate = VideoConfigUtil.scaleAndClampBitrate(
                VIDEO_BITRATE_BASE,
                resolvedFrameRate, VIDEO_FRAME_RATE_BASE,
                mSurfaceSize.getWidth(), VIDEO_SIZE_BASE.getWidth(),
                mSurfaceSize.getHeight(), VIDEO_SIZE_BASE.getHeight(),
                videoSpecBitrateRange);

        return VideoEncoderConfig.builder()
                .setMimeType(mMimeType)
                .setResolution(mSurfaceSize)
                .setBitrate(resolvedBitrate)
                .setFrameRate(resolvedFrameRate)
                .build();
    }
}
