/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.video;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.DynamicRange;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * VideoCapabilities is used to query video recording capabilities on the device.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface VideoCapabilities {

    /**
     * Gets all dynamic ranges supported by both the camera and video output.
     *
     * <p>Only {@link DynamicRange}s with specified values both in {@link DynamicRange.BitDepth}
     * and {@link DynamicRange.DynamicRangeFormat} will be present in the returned set.
     * {@link DynamicRange}s such as {@link DynamicRange#HDR_UNSPECIFIED_10_BIT} will not be
     * included, but they can be used in other methods, such as checking for quality support with
     * {@link #isQualitySupported(Quality, DynamicRange)}.
     */
    @NonNull
    Set<DynamicRange> getSupportedDynamicRanges();

    /**
     * Gets all supported qualities for the input dynamic range.
     *
     * <p>The returned list is sorted by quality size from large to small.
     *
     * <p>Note: Constants {@link Quality#HIGHEST} and {@link Quality#LOWEST} are not included.
     */
    @NonNull
    List<Quality> getSupportedQualities(@NonNull DynamicRange dynamicRange);

    /**
     * Checks if the quality is supported for the input dynamic range.
     *
     * @param quality one of the quality constants. Possible values include
     *                {@link Quality#LOWEST}, {@link Quality#HIGHEST}, {@link Quality#SD},
     *                {@link Quality#HD}, {@link Quality#FHD}, or {@link Quality#UHD}.
     * @param dynamicRange the target dynamicRange.
     * @return {@code true} if the quality is supported; {@code false} otherwise.
     */
    boolean isQualitySupported(@NonNull Quality quality, @NonNull DynamicRange dynamicRange);

    /**
     * Gets the corresponding {@link VideoValidatedEncoderProfilesProxy} of the input quality and
     * dynamic range.
     *
     * @param quality one of the quality constants. Possible values include
     *                {@link Quality#LOWEST}, {@link Quality#HIGHEST}, {@link Quality#SD},
     *                {@link Quality#HD}, {@link Quality#FHD}, or {@link Quality#UHD}.
     * @param dynamicRange target dynamicRange.
     * @return the corresponding VideoValidatedEncoderProfilesProxy, or {@code null} if the
     * quality is not supported on the device.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    default VideoValidatedEncoderProfilesProxy getProfiles(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        return null;
    }

    /**
     * Finds the supported EncoderProfilesProxy with the resolution nearest to the given
     * {@link Size}.
     *
     * <p>The supported EncoderProfilesProxy means the corresponding {@link Quality} is also
     * supported. If the size aligns exactly with the pixel count of an EncoderProfilesProxy,
     * that EncoderProfilesProxy will be selected. If the size falls between two
     * EncoderProfilesProxy, the higher resolution will always be selected. Otherwise, the
     * nearest EncoderProfilesProxy will be selected, whether that EncoderProfilesProxy's
     * resolution is above or below the given size.
     *
     * @see #findHighestSupportedQualityFor(Size, DynamicRange)
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    default VideoValidatedEncoderProfilesProxy findHighestSupportedEncoderProfilesFor(
            @NonNull Size size, @NonNull DynamicRange dynamicRange) {
        return null;
    }

    /**
     * Finds the nearest quality by number of pixels to the given {@link Size}.
     *
     * <p>If the size aligns exactly with the pixel count of a supported quality, that quality
     * will be selected. If the size falls between two qualities, the higher quality will always
     * be selected. Otherwise, the nearest single quality will be selected, whether that
     * quality's size is above or below the given size.
     *
     * @param size the size representing the number of pixels for comparison. Pixels are assumed
     *             to be square.
     * @param dynamicRange target dynamicRange.
     * @return the quality constant defined in {@link Quality}. If no qualities are supported,
     * then {@link Quality#NONE} is returned.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    default Quality findHighestSupportedQualityFor(@NonNull Size size,
            @NonNull DynamicRange dynamicRange) {
        return Quality.NONE;
    }

    /** An empty implementation. */
    VideoCapabilities EMPTY = new VideoCapabilities() {
        @NonNull
        @Override
        public Set<DynamicRange> getSupportedDynamicRanges() {
            return new HashSet<>();
        }

        @NonNull
        @Override
        public List<Quality> getSupportedQualities(@NonNull DynamicRange dynamicRange) {
            return new ArrayList<>();
        }

        @Override
        public boolean isQualitySupported(@NonNull Quality quality,
                @NonNull DynamicRange dynamicRange) {
            return false;
        }
    };
}
