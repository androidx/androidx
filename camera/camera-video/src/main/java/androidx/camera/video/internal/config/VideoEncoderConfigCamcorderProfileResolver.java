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
import androidx.camera.core.impl.CamcorderProfileProxy;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.core.util.Supplier;

/**
 * A {@link VideoEncoderConfig} supplier that resolves requested encoder settings from a
 * {@link VideoSpec} for the given surface {@link Size} using the provided
 * {@link CamcorderProfileProxy}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoEncoderConfigCamcorderProfileResolver implements Supplier<VideoEncoderConfig> {

    private static final String TAG = "VidEncCmcrdrPrflRslvr";

    private final String mMimeType;
    private final VideoSpec mVideoSpec;
    private final Size mSurfaceSize;
    private final CamcorderProfileProxy mCamcorderProfile;

    /**
     * Constructor for a VideoEncoderConfigCamcorderProfileResolver.
     *
     * @param mimeType         The mime type for the video encoder
     * @param videoSpec        The {@link VideoSpec} which defines the settings that should be
     *                         used with the video encoder.
     * @param surfaceSize      The size of the surface required by the camera for the video encoder.
     * @param camcorderProfile The {@link CamcorderProfileProxy} used to resolve automatic and
     *                         range settings.
     */
    public VideoEncoderConfigCamcorderProfileResolver(@NonNull String mimeType,
            @NonNull VideoSpec videoSpec,
            @NonNull Size surfaceSize,
            @NonNull CamcorderProfileProxy camcorderProfile) {
        mMimeType = mimeType;
        mVideoSpec = videoSpec;
        mSurfaceSize = surfaceSize;
        mCamcorderProfile = camcorderProfile;
    }

    @Override
    @NonNull
    public VideoEncoderConfig get() {
        int resolvedFrameRate = VideoConfigUtil.resolveFrameRate(mVideoSpec);

        Range<Integer> videoSpecBitrateRange = mVideoSpec.getBitrate();
        Logger.d(TAG, "Using resolved VIDEO bitrate from CamcorderProfile");
        int resolvedBitrate = VideoConfigUtil.scaleAndClampBitrate(
                mCamcorderProfile.getVideoBitRate(),
                resolvedFrameRate, mCamcorderProfile.getVideoFrameRate(),
                mSurfaceSize.getWidth(), mCamcorderProfile.getVideoFrameWidth(),
                mSurfaceSize.getHeight(), mCamcorderProfile.getVideoFrameHeight(),
                videoSpecBitrateRange);

        return VideoEncoderConfig.builder()
                .setMimeType(mMimeType)
                .setResolution(mSurfaceSize)
                .setBitrate(resolvedBitrate)
                .setFrameRate(resolvedFrameRate)
                .build();
    }
}
