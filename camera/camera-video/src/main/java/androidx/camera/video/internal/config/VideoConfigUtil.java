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
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.Timebase;
import androidx.camera.video.MediaSpec;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.core.util.Supplier;

import java.util.Objects;

/**
 * A collection of utilities used for resolving and debugging video configurations.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class VideoConfigUtil {
    private static final String TAG = "VideoConfigUtil";

    // Should not be instantiated.
    private VideoConfigUtil() {
    }

    /**
     * Resolves the video mime information into a {@link MimeInfo}.
     *
     * @param mediaSpec        the media spec to resolve the mime info.
     * @param encoderProfiles  the encoder profiles to resolve the mime info. It can be null if
     *                         there is no relevant encoder profiles.
     * @return the video MimeInfo.
     */
    @NonNull
    public static MimeInfo resolveVideoMimeInfo(@NonNull MediaSpec mediaSpec,
            @Nullable VideoValidatedEncoderProfilesProxy encoderProfiles) {
        String mediaSpecVideoMime = MediaSpec.outputFormatToVideoMime(mediaSpec.getOutputFormat());
        String resolvedVideoMime = mediaSpecVideoMime;
        boolean encoderProfilesIsCompatible = false;
        if (encoderProfiles != null) {
            VideoProfileProxy videoProfile = encoderProfiles.getDefaultVideoProfile();
            String encoderProfilesVideoMime = videoProfile.getMediaType();
            // Use EncoderProfiles settings if the media spec's output format is set to auto or
            // happens to match the EncoderProfiles' output format.
            if (Objects.equals(encoderProfilesVideoMime, VideoProfileProxy.MEDIA_TYPE_NONE)) {
                Logger.d(TAG, "EncoderProfiles contains undefined VIDEO mime type so cannot be "
                        + "used. May rely on fallback defaults to derive settings [chosen mime "
                        + "type: " + resolvedVideoMime + "]");
            } else if (mediaSpec.getOutputFormat() == MediaSpec.OUTPUT_FORMAT_AUTO) {
                encoderProfilesIsCompatible = true;
                resolvedVideoMime = encoderProfilesVideoMime;
                Logger.d(TAG, "MediaSpec contains OUTPUT_FORMAT_AUTO. Using EncoderProfiles "
                        + "to derive VIDEO settings [mime type: " + resolvedVideoMime + "]");
            } else if (Objects.equals(mediaSpecVideoMime, encoderProfilesVideoMime)) {
                encoderProfilesIsCompatible = true;
                resolvedVideoMime = encoderProfilesVideoMime;
                Logger.d(TAG, "MediaSpec video mime matches EncoderProfiles. Using "
                        + "EncoderProfiles to derive VIDEO settings [mime type: "
                        + resolvedVideoMime + "]");
            } else {
                Logger.d(TAG, "MediaSpec video mime does not match EncoderProfiles, so "
                        + "EncoderProfiles settings cannot be used. May rely on fallback "
                        + "defaults to derive VIDEO settings [EncoderProfiles mime type: "
                        + encoderProfilesVideoMime + ", chosen mime type: "
                        + resolvedVideoMime + "]");
            }
        } else {
            Logger.d(TAG, "No EncoderProfiles present. May rely on fallback defaults to derive "
                    + "VIDEO settings [chosen mime type: " + resolvedVideoMime + "]");
        }

        MimeInfo.Builder mimeInfoBuilder = MimeInfo.builder(resolvedVideoMime);
        if (encoderProfilesIsCompatible) {
            mimeInfoBuilder.setCompatibleEncoderProfiles(encoderProfiles);
        }

        return mimeInfoBuilder.build();
    }

    /**
     * Resolves video related information into a {@link VideoEncoderConfig}.
     *
     * @param videoMimeInfo          the video mime info.
     * @param videoSpec              the video spec.
     * @param inputTimebase          the timebase of the input frame.
     * @param surfaceSize            the surface size.
     * @param expectedFrameRateRange the expected frame rate range. It could be null.
     * @return a VideoEncoderConfig.
     */
    @NonNull
    public static VideoEncoderConfig resolveVideoEncoderConfig(@NonNull MimeInfo videoMimeInfo,
            @NonNull Timebase inputTimebase, @NonNull VideoSpec videoSpec,
            @NonNull Size surfaceSize, @Nullable Range<Integer> expectedFrameRateRange) {
        Supplier<VideoEncoderConfig> configSupplier;
        VideoValidatedEncoderProfilesProxy profiles = videoMimeInfo.getCompatibleEncoderProfiles();
        if (profiles != null) {
            configSupplier = new VideoEncoderConfigVideoProfileResolver(
                    videoMimeInfo.getMimeType(), inputTimebase, videoSpec, surfaceSize,
                    profiles.getDefaultVideoProfile(), expectedFrameRateRange);
        } else {
            configSupplier = new VideoEncoderConfigDefaultResolver(videoMimeInfo.getMimeType(),
                    inputTimebase, videoSpec, surfaceSize, expectedFrameRateRange);
        }

        return configSupplier.get();
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
