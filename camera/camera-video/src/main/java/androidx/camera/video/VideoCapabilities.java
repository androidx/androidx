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

package androidx.camera.video;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.ResolutionValidatedEncoderProfilesProvider;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.compat.quirk.VideoQualityQuirk;
import androidx.core.util.Preconditions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * VideoCapabilities is used to query video recording capabilities on the device.
 *
 * <p>Calling {@link #from(CameraInfo)} to obtain the VideoCapabilities.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(Scope.LIBRARY)
public final class VideoCapabilities {
    private static final String TAG = "VideoCapabilities";

    /**
     * Maps quality to supported {@link VideoValidatedEncoderProfilesProxy}. The order is from
     * size large to small.
     */
    private final Map<Quality, VideoValidatedEncoderProfilesProxy> mSupportedProfilesMap =
            new LinkedHashMap<>();
    private final TreeMap<Size, Quality> mAreaSortedSizeToQualityMap =
            new TreeMap<>(new CompareSizesByArea());
    private final VideoValidatedEncoderProfilesProxy mHighestProfiles;
    private final VideoValidatedEncoderProfilesProxy mLowestProfiles;
    private final EncoderProfilesProvider mEncoderProfilesProvider;

    /**
     * Creates a VideoCapabilities.
     *
     * @param cameraInfoInternal the cameraInfo
     * @throws IllegalArgumentException if unable to get the capability information from the
     *                                  CameraInfo.
     */
    VideoCapabilities(@NonNull CameraInfoInternal cameraInfoInternal) {
        Quirks cameraQuirks = cameraInfoInternal.getCameraQuirks();
        mEncoderProfilesProvider = new ResolutionValidatedEncoderProfilesProvider(
                cameraInfoInternal.getEncoderProfilesProvider(), cameraQuirks);

        // Construct supported profile map
        for (Quality quality : Quality.getSortedQualities()) {
            EncoderProfilesProxy profiles = getEncoderProfiles(cameraInfoInternal, quality);
            if (profiles == null) {
                continue;
            }

            // Validate that EncoderProfiles contain video information
            Logger.d(TAG, "profiles = " + profiles);
            VideoValidatedEncoderProfilesProxy validatedProfiles = toValidatedProfiles(profiles);
            if (validatedProfiles == null) {
                Logger.w(TAG, "EncoderProfiles of quality " + quality + " has no video "
                        + "validated profiles.");
                continue;
            }

            VideoProfileProxy videoProfile = validatedProfiles.getDefaultVideoProfile();
            Size size = new Size(videoProfile.getWidth(), videoProfile.getHeight());
            mAreaSortedSizeToQualityMap.put(size, quality);

            // SortedQualities is from size large to small
            mSupportedProfilesMap.put(quality, validatedProfiles);
        }
        if (mSupportedProfilesMap.isEmpty()) {
            Logger.e(TAG, "No supported EncoderProfiles");
            mLowestProfiles = null;
            mHighestProfiles = null;
        } else {
            Deque<VideoValidatedEncoderProfilesProxy> profileQueue = new ArrayDeque<>(
                    mSupportedProfilesMap.values());
            mHighestProfiles = profileQueue.peekFirst();
            mLowestProfiles = profileQueue.peekLast();
        }
    }

    /** Gets VideoCapabilities by the {@link CameraInfo} */
    @NonNull
    public static VideoCapabilities from(@NonNull CameraInfo cameraInfo) {
        return new VideoCapabilities((CameraInfoInternal) cameraInfo);
    }

    /**
     * Gets all supported qualities on the device.
     *
     * <p>The returned list is sorted by quality size from large to small. For the qualities in
     * the returned list, calling {@link #getProfiles(Quality)} with these qualities will return a
     * non-null result.
     *
     * <p>Note: Constants {@link Quality#HIGHEST} and {@link Quality#LOWEST} are not included.
     */
    @NonNull
    public List<Quality> getSupportedQualities() {
        return new ArrayList<>(mSupportedProfilesMap.keySet());
    }

    /**
     * Checks if the quality is supported.
     *
     * @param quality one of the quality constants. Possible values include
     *                {@link Quality#LOWEST}, {@link Quality#HIGHEST}, {@link Quality#SD},
     *                {@link Quality#HD}, {@link Quality#FHD}, or {@link Quality#UHD}.
     * @return {@code true} if the quality is supported; {@code false} otherwise.
     * @throws IllegalArgumentException if not a quality constant.
     */
    public boolean isQualitySupported(@NonNull Quality quality) {
        checkQualityConstantsOrThrow(quality);
        return getProfiles(quality) != null;
    }

    /**
     * Gets the corresponding {@link VideoValidatedEncoderProfilesProxy} of the input quality.
     *
     * @param quality one of the quality constants. Possible values include
     *                {@link Quality#LOWEST}, {@link Quality#HIGHEST}, {@link Quality#SD},
     *                {@link Quality#HD}, {@link Quality#FHD}, or {@link Quality#UHD}.
     * @return the VideoValidatedEncoderProfilesProxy
     * @throws IllegalArgumentException if not a quality constant
     */
    @Nullable
    public VideoValidatedEncoderProfilesProxy getProfiles(@NonNull Quality quality) {
        checkQualityConstantsOrThrow(quality);
        if (quality == Quality.HIGHEST) {
            return mHighestProfiles;
        } else if (quality == Quality.LOWEST) {
            return mLowestProfiles;
        }
        return mSupportedProfilesMap.get(quality);
    }

    /**
     * Finds the supported EncoderProfilesProxy with the resolution nearest to the given
     * {@link Size}.
     *
     * <p>The supported EncoderProfilesProxy means the corresponding {@link Quality} is also
     * supported. If the size aligns exactly with the pixel count of an EncoderProfilesProxy, that
     * EncoderProfilesProxy will be selected. If the size falls between two EncoderProfilesProxy,
     * the higher resolution will always be selected. Otherwise, the nearest EncoderProfilesProxy
     * will be selected, whether that EncoderProfilesProxy's resolution is above or below the
     * given size.
     *
     * @see #findHighestSupportedQualityFor(Size)
     */
    @Nullable
    public VideoValidatedEncoderProfilesProxy findHighestSupportedEncoderProfilesFor(
            @NonNull Size size) {
        VideoValidatedEncoderProfilesProxy encoderProfiles = null;
        Quality highestSupportedQuality = findHighestSupportedQualityFor(size);
        Logger.d(TAG,
                "Using supported quality of " + highestSupportedQuality + " for size " + size);
        if (highestSupportedQuality != Quality.NONE) {
            encoderProfiles = getProfiles(highestSupportedQuality);
            if (encoderProfiles == null) {
                throw new AssertionError("Camera advertised available quality but did not "
                        + "produce EncoderProfiles for advertised quality.");
            }
        }
        return encoderProfiles;
    }

    /**
     * Finds the nearest quality by number of pixels to the given {@link Size}.
     *
     * <p>If the size aligns exactly with the pixel count of a supported quality, that quality
     * will be selected. If the size falls between two qualities, the higher quality will always
     * be selected. Otherwise, the nearest single quality will be selected, whether that
     * quality's size is above or below the given size.
     *
     * @param size The size representing the number of pixels for comparison. Pixels are assumed
     *             to be square.
     * @return The quality constant defined in {@link Quality}. If no qualities are supported,
     * then {@link Quality#NONE} is returned.
     */
    @NonNull
    public Quality findHighestSupportedQualityFor(@NonNull Size size) {
        Map.Entry<Size, Quality> ceilEntry = mAreaSortedSizeToQualityMap.ceilingEntry(size);

        if (ceilEntry != null) {
            // The ceiling entry will either be equivalent or higher in size, so always return it.
            return ceilEntry.getValue();
        } else {
            // If a ceiling entry doesn't exist and a floor entry exists, it is the closest we have,
            // so return it.
            Map.Entry<Size, Quality> floorEntry = mAreaSortedSizeToQualityMap.floorEntry(size);
            if (floorEntry != null) {
                return floorEntry.getValue();
            }
        }

        // No supported qualities.
        return Quality.NONE;
    }

    private static void checkQualityConstantsOrThrow(@NonNull Quality quality) {
        Preconditions.checkArgument(Quality.containsQuality(quality),
                "Unknown quality: " + quality);
    }

    private boolean isDeviceValidQuality(@NonNull CameraInfoInternal cameraInfo,
            @NonNull Quality quality) {
        for (VideoQualityQuirk quirk : DeviceQuirks.getAll(VideoQualityQuirk.class)) {
            if (quirk != null && quirk.isProblematicVideoQuality(cameraInfo, quality)
                    && !quirk.workaroundBySurfaceProcessing()) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    private EncoderProfilesProxy getEncoderProfiles(@NonNull CameraInfoInternal cameraInfo,
            @NonNull Quality quality) {
        Preconditions.checkState(quality instanceof Quality.ConstantQuality,
                "Currently only support ConstantQuality");
        int qualityValue = ((Quality.ConstantQuality) quality).getValue();

        if (!mEncoderProfilesProvider.hasProfile(qualityValue) || !isDeviceValidQuality(cameraInfo,
                quality)) {
            return null;
        }

        return mEncoderProfilesProvider.getAll(qualityValue);
    }

    @Nullable
    private VideoValidatedEncoderProfilesProxy toValidatedProfiles(
            @NonNull EncoderProfilesProxy profiles) {
        // According to the document, the first profile is the default video profile.
        List<VideoProfileProxy> videoProfiles = profiles.getVideoProfiles();
        if (videoProfiles.isEmpty()) {
            return null;
        }

        return VideoValidatedEncoderProfilesProxy.from(profiles);
    }
}
