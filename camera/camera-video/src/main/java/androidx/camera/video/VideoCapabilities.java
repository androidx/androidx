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

import android.hardware.camera2.CaptureRequest;
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
 *
 * <p>Take {@link Recorder} as an example, the supported {@link DynamicRange}s can be queried with
 * the following code:
 * <pre>{@code
 *   VideoCapabilities videoCapabilities = Recorder.getVideoCapabilities(cameraInfo);
 *   Set<DynamicRange> supportedDynamicRanges = videoCapabilities.getSupportedDynamicRanges();
 * }</pre>
 * <p>The query result can be used to check if high dynamic range (HDR) recording is
 * supported, and to get the supported qualities of the target {@link DynamicRange}:
 * <pre>{@code
 *   List<Quality> supportedQualities = videoCapabilities.getSupportedQualities(dynamicRange);
 * }</pre>
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface VideoCapabilities {

    /**
     * Gets all dynamic ranges supported by both the camera and video output.
     *
     * <p>Only {@link DynamicRange}s with specified values both in {@link DynamicRange.BitDepth}
     * and {@link DynamicRange.DynamicRangeEncoding} will be present in the returned set.
     * {@link DynamicRange}s such as {@link DynamicRange#HDR_UNSPECIFIED_10_BIT} will not be
     * included, but they can be used in other methods, such as checking for quality support with
     * {@link #isQualitySupported(Quality, DynamicRange)}.
     *
     * @return a set of supported dynamic ranges.
     */
    @NonNull
    Set<DynamicRange> getSupportedDynamicRanges();

    /**
     * Gets all supported qualities for the input dynamic range.
     *
     * <p>The returned list is sorted by quality size from largest to smallest. For the qualities in
     * the returned list, with the same input dynamicRange,
     * {@link #isQualitySupported(Quality, DynamicRange)} will return {@code true}.
     *
     * <p>When the {@code dynamicRange} is not fully specified, e.g.
     * {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}, the returned list is the union of the
     * qualities supported by the matching fully specified dynamic ranges. This does not mean
     * that all returned qualities are available for every matching dynamic range. Therefore, it
     * is not recommended to rely on any one particular quality to work if mixing use cases with
     * other dynamic ranges.
     *
     * <p>Note: Constants {@link Quality#HIGHEST} and {@link Quality#LOWEST} are not included in
     * the returned list, but their corresponding qualities are included. For example: when the
     * returned list consists of {@link Quality#UHD}, {@link Quality#FHD} and {@link Quality#HD},
     * {@link Quality#HIGHEST} corresponds to {@link Quality#UHD}, which is the highest quality,
     * and {@link Quality#LOWEST} corresponds to {@link Quality#HD}.
     *
     * @param dynamicRange the dynamicRange.
     * @return a list of supported qualities sorted by size from large to small.
     */
    @NonNull
    List<Quality> getSupportedQualities(@NonNull DynamicRange dynamicRange);

    /**
     * Checks if the quality is supported for the input dynamic range.
     *
     * <p>Calling this method with one of the qualities contained in the returned list of
     * {@link #getSupportedQualities(DynamicRange)} will return {@code true}.
     *
     * <p>Possible values for {@code quality} include {@link Quality#LOWEST},
     * {@link Quality#HIGHEST}, {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD}
     * and {@link Quality#UHD}.
     *
     * <p>If this method is called with {@link Quality#LOWEST} or {@link Quality#HIGHEST}, it
     * will return {@code true} except the case that none of the qualities can be supported.
     *
     * <p>When the {@code dynamicRange} is not fully specified, e.g.
     * {@link DynamicRange#HDR_UNSPECIFIED_10_BIT}, {@code true} will be returned if there is any
     * matching fully specified dynamic range supporting the {@code quality}, otherwise {@code
     * false} will be returned.
     *
     * @param quality one of the quality constants.
     * @param dynamicRange the dynamicRange.
     * @return {@code true} if the quality is supported; {@code false} otherwise.
     * @see #getSupportedQualities(DynamicRange)
     */
    boolean isQualitySupported(@NonNull Quality quality, @NonNull DynamicRange dynamicRange);

    /**
     * Returns if video stabilization is supported on the device. Video stabilization can be
     * turned on via {@link VideoCapture.Builder#setVideoStabilizationEnabled(boolean)}.
     *
     * <p>Not all recording sizes or frame rates may be supported for
     * stabilization by a device that reports stabilization support. It is guaranteed
     * that an output targeting a MediaRecorder or MediaCodec will be stabilized if
     * the recording resolution is less than or equal to 1920 x 1080 (width less than
     * or equal to 1920, height less than or equal to 1080), and the recording
     * frame rate is less than or equal to 30fps. At other sizes, the video stabilization will
     * not take effect.
     *
     * @return true if {@link CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE_ON} is supported,
     * otherwise false.
     *
     * @see VideoCapture.Builder#setVideoStabilizationEnabled(boolean)
     * @see CaptureRequest#CONTROL_VIDEO_STABILIZATION_MODE
     */
    default boolean isStabilizationSupported() {
        return false;
    }

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
     * @see #findNearestHigherSupportedQualityFor(Size, DynamicRange)
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    default VideoValidatedEncoderProfilesProxy findNearestHigherSupportedEncoderProfilesFor(
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
    default Quality findNearestHigherSupportedQualityFor(@NonNull Size size,
            @NonNull DynamicRange dynamicRange) {
        return Quality.NONE;
    }

    /** An empty implementation. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
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

        @Override
        public boolean isStabilizationSupported() {
            return false;
        }
    };
}
