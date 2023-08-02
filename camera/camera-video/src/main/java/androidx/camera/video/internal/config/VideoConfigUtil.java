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
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.video.VideoSpec;

/**
 * A collection of utilities used for resolving and debugging video configurations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoConfigUtil {
    private static final String TAG = "VideoConfigUtil";

    // Should not be instantiated.
    private VideoConfigUtil() {
    }

    static int resolveFrameRate(@NonNull Range<Integer> preferredRange,
            int exactFrameRateHint, @Nullable Range<Integer> strictOperatingFpsRange) {
        Range<Integer> refinedRange;
        if (strictOperatingFpsRange != null) {
            // We have a strict operating range. Our frame rate should always be in this
            // range. Since we can only choose a single frame rate (which acts as a target for
            // VBR), we can only fine tune our preferences within that range.
            try {
                // First, let's try to intersect with the preferred frame rate range since this
                // could contain intent from the user.
                refinedRange = strictOperatingFpsRange.intersect(preferredRange);
            } catch (IllegalArgumentException ex) {
                // Ranges are disjoint. Choose the closest extreme as our frame rate.
                if (preferredRange.getUpper() < strictOperatingFpsRange.getLower()) {
                    // Preferred range is below operating range.
                    return strictOperatingFpsRange.getLower();
                } else {
                    // Preferred range is above operating range.
                    return strictOperatingFpsRange.getUpper();
                }
            }
        } else {
            // We only have the preferred range as a hint since the operating range is null.
            refinedRange = preferredRange;
        }

        // Finally, try to apply the exact frame rate hint to the refined range since
        // other settings may expect this number.
        return refinedRange.clamp(exactFrameRateHint);
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
