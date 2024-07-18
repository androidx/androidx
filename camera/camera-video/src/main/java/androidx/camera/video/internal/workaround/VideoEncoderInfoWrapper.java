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

package androidx.camera.video.internal.workaround;

import android.media.MediaCodecInfo;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.MediaCodecInfoReportIncorrectInfoQuirk;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;
import androidx.core.util.Preconditions;

import java.util.HashSet;
import java.util.Set;

/**
 * Workaround to wrap the VideoEncoderInfo in order to fix the wrong information provided by
 * {@link MediaCodecInfo}.
 *
 * <p>One use case is VideoCapture resizing the crop to a size valid for the encoder.
 *
 * @see MediaCodecInfoReportIncorrectInfoQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class VideoEncoderInfoWrapper implements VideoEncoderInfo {
    private static final String TAG = "VideoEncoderInfoWrapper";

    // The resolution of CamcorderProfile.QUALITY_4KDCI
    private static final int WIDTH_4KDCI = 4096;
    private static final int HEIGHT_4KDCI = 2160;

    private final VideoEncoderInfo mVideoEncoderInfo;
    private final Range<Integer> mSupportedWidths;
    private final Range<Integer> mSupportedHeights;
    // Extra supported sizes is used to put resolutions that are actually supported on the device
    // but the MediaCodecInfo indicates the resolution is invalid. The most common one is
    // 1920x1080. For resolutions in this set, #isSizeSupported(w, h) should return true.
    private final Set<Size> mExtraSupportedSizes = new HashSet<>();

    /**
     * Check and wrap an input VideoEncoderInfo
     *
     * <p>The input VideoEncoderInfo will be wrapped when
     * <ul>
     * <li>The device is a quirk device determined in
     * {@link MediaCodecInfoReportIncorrectInfoQuirk}.</li>
     * <li>The input {@code validSizeToCheck} is not supported by input VideoEncoderInfo.</li>
     * </ul>
     * Otherwise, the input VideoEncoderInfo will be returned.
     *
     * <p>Exception: if the input videoEncoderInfo is already a wrapper, then it will not be
     * wrapped again and will be returned directly.
     *
     * @param videoEncoderInfo the input VideoEncoderInfo.
     * @param validSizeToCheck a valid size to check or null if no valid size to check.
     * @return a wrapped VideoEncoderInfo or the input VideoEncoderInfo.
     */
    @NonNull
    public static VideoEncoderInfo from(@NonNull VideoEncoderInfo videoEncoderInfo,
            @Nullable Size validSizeToCheck) {
        boolean toWrap;
        if (videoEncoderInfo instanceof VideoEncoderInfoWrapper) {
            toWrap = false;
        } else if (DeviceQuirks.get(MediaCodecInfoReportIncorrectInfoQuirk.class) != null) {
            toWrap = true;
        } else if (validSizeToCheck != null && !videoEncoderInfo.isSizeSupported(
                validSizeToCheck.getWidth(), validSizeToCheck.getHeight())) {
            // If the device does not support a size that should be valid, assume the device
            // reports incorrect information. This is used to detect devices that we haven't
            // discovered incorrect information yet.
            Logger.w(TAG, String.format(
                    "Detected that the device does not support a size %s that should be valid"
                            + " in widths/heights = %s/%s", validSizeToCheck,
                    videoEncoderInfo.getSupportedWidths(),
                    videoEncoderInfo.getSupportedHeights()));
            toWrap = true;
        } else {
            toWrap = false;
        }
        return toWrap ? new VideoEncoderInfoWrapper(videoEncoderInfo, validSizeToCheck)
                : videoEncoderInfo;
    }

    VideoEncoderInfoWrapper(@NonNull VideoEncoderInfo videoEncoderInfo, @Nullable Size validSize) {
        mVideoEncoderInfo = videoEncoderInfo;

        // Ideally we should find out supported widths/heights for each problematic device.
        // As a workaround, simply return a big enough size for video encoding. i.e.
        // CamcorderProfile.QUALITY_4KDCI. The size still need to follow the multiple of alignment.
        int widthAlignment = videoEncoderInfo.getWidthAlignment();
        int maxWidth = (int) Math.ceil((double) WIDTH_4KDCI / widthAlignment) * widthAlignment;
        mSupportedWidths = Range.create(widthAlignment, maxWidth);
        int heightAlignment = videoEncoderInfo.getHeightAlignment();
        int maxHeight = (int) Math.ceil((double) HEIGHT_4KDCI / heightAlignment) * heightAlignment;
        mSupportedHeights = Range.create(heightAlignment, maxHeight);

        if (validSize != null) {
            mExtraSupportedSizes.add(validSize);
        }
        mExtraSupportedSizes.addAll(
                MediaCodecInfoReportIncorrectInfoQuirk.getExtraSupportedSizes());
    }

    @NonNull
    @Override
    public String getName() {
        return mVideoEncoderInfo.getName();
    }

    @Override
    public boolean isSizeSupported(int width, int height) {
        if (!mExtraSupportedSizes.isEmpty() && mExtraSupportedSizes.contains(
                new Size(width, height))) {
            return true;
        }
        return mSupportedWidths.contains(width)
                && mSupportedHeights.contains(height)
                && width % mVideoEncoderInfo.getWidthAlignment() == 0
                && height % mVideoEncoderInfo.getHeightAlignment() == 0;
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedWidths() {
        return mSupportedWidths;
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedHeights() {
        return mSupportedHeights;
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedWidthsFor(int height) {
        Preconditions.checkArgument(mSupportedHeights.contains(height)
                        && height % mVideoEncoderInfo.getHeightAlignment() == 0,
                "Not supported height: " + height + " which is not in " + mSupportedHeights
                        + " or can not be divided by alignment "
                        + mVideoEncoderInfo.getHeightAlignment());
        return mSupportedWidths;
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedHeightsFor(int width) {
        Preconditions.checkArgument(mSupportedWidths.contains(width)
                        && width % mVideoEncoderInfo.getWidthAlignment() == 0,
                "Not supported width: " + width + " which is not in " + mSupportedWidths
                        + " or can not be divided by alignment "
                        + mVideoEncoderInfo.getWidthAlignment());
        return mSupportedHeights;
    }

    @Override
    public int getWidthAlignment() {
        return mVideoEncoderInfo.getWidthAlignment();
    }

    @Override
    public int getHeightAlignment() {
        return mVideoEncoderInfo.getHeightAlignment();
    }

    @NonNull
    @Override
    public Range<Integer> getSupportedBitrateRange() {
        return mVideoEncoderInfo.getSupportedBitrateRange();
    }
}
