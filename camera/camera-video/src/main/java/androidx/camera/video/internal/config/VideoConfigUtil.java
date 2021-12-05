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
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.video.VideoSpec;

/**
 * A collection of utilities used for resolving and debugging video configurations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoConfigUtil {
    private static final String TAG = "VideoConfigUtil";

    private static final int VIDEO_FRAME_RATE_FIXED_DEFAULT = 30;

    // Should not be instantiated.
    private VideoConfigUtil() {
    }

    static int resolveFrameRate(@NonNull VideoSpec videoSpec) {
        // TODO(b/177918193): We currently cannot communicate the frame rate to the camera,
        //  so we only support 30fps. This should come from MediaSpec or use
        //  CamcorderProfile.videoFrameRate if set to AUTO framerate.
        Range<Integer> videoSpecFrameRateRange = videoSpec.getFrameRate();
        int resolvedFrameRate = VIDEO_FRAME_RATE_FIXED_DEFAULT;
        if (VideoSpec.FRAME_RATE_RANGE_AUTO.equals(videoSpecFrameRateRange)
                || videoSpecFrameRateRange.contains(VIDEO_FRAME_RATE_FIXED_DEFAULT)) {
            Logger.d(TAG, "Using single supported VIDEO frame rate: " + resolvedFrameRate);
        } else {
            Logger.w(TAG,
                    "Requested frame rate range does not include single supported frame rate. "
                            + "Ignoring range. [range: " + videoSpecFrameRateRange + " supported "
                            + "frame rate: " + resolvedFrameRate + "]");
        }

        return resolvedFrameRate;
    }

    static int scaleAndClampBitrate(
            int baseBitrate,
            int actualFrameRate, int baseFrameRate,
            int actualWidth, int baseWidth,
            int actualHeight, int baseHeight,
            @NonNull Range<Integer> clampedRange) {
        // Scale bitrate to match current frame rate
        Rational frameRateRatio = new Rational(actualFrameRate, baseFrameRate);
        // Scale bitrate depending on number of actual pixels relative to profile's
        // number of pixels.
        // TODO(b/191678894): This should come from the eventual crop rectangle rather
        //  than the full surface size.
        Rational widthRatio = new Rational(actualWidth, baseWidth);
        Rational heightRatio = new Rational(actualHeight, baseHeight);
        int resolvedBitrate =
                (int) (baseBitrate * frameRateRatio.doubleValue() * widthRatio.doubleValue()
                        * heightRatio.doubleValue());

        String debugString = "";
        if (Logger.isDebugEnabled(TAG)) {
            debugString = String.format("Base Bitrate(%dbps) * Frame Rate Ratio(%d / %d) * Width "
                            + "Ratio(%d / %d) * Height Ratio(%d / %d) = %d", baseBitrate,
                    actualFrameRate,
                    baseFrameRate, actualWidth, baseWidth, actualHeight, baseHeight,
                    resolvedBitrate);
        }

        if (!VideoSpec.BITRATE_RANGE_AUTO.equals(clampedRange)) {
            // Clamp the resolved bitrate
            resolvedBitrate = clampedRange.clamp(resolvedBitrate);
            if (Logger.isDebugEnabled(TAG)) {
                debugString += String.format("\nClamped to range %s -> %dbps", clampedRange,
                        resolvedBitrate);
            }
        }
        Logger.d(TAG, debugString);
        return resolvedBitrate;
    }
}
