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

import static androidx.camera.core.DynamicRange.BIT_DEPTH_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.FORMAT_HDR_UNSPECIFIED;
import static androidx.camera.core.DynamicRange.FORMAT_HLG;
import static androidx.camera.core.DynamicRange.FORMAT_SDR;
import static androidx.camera.core.DynamicRange.FORMAT_UNSPECIFIED;
import static androidx.camera.video.internal.BackupHdrProfileEncoderProfilesProvider.DEFAULT_VALIDATOR;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.Quirks;
import androidx.camera.core.impl.ResolutionValidatedEncoderProfilesProvider;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.video.internal.BackupHdrProfileEncoderProfilesProvider;
import androidx.camera.video.internal.DynamicRangeMatchedEncoderProfilesProvider;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.compat.quirk.DeviceQuirks;
import androidx.camera.video.internal.workaround.QualityValidatedEncoderProfilesProvider;
import androidx.core.util.Preconditions;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * RecorderVideoCapabilities is used to query video recording capabilities related to Recorder.
 *
 * <p>The {@link EncoderProfilesProxy} queried from RecorderVideoCapabilities will contain
 * {@link VideoProfileProxy}s matches with the target {@link DynamicRange}. When HDR is
 * supported, RecorderVideoCapabilities will try best to provide additional backup HDR
 * {@link VideoProfileProxy}s in case the information is lacked in the device.
 *
 * @see Recorder#getVideoCapabilities(CameraInfo)
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RecorderVideoCapabilities implements VideoCapabilities {

    private static final String TAG = "RecorderVideoCapabilities";

    private final EncoderProfilesProvider mProfilesProvider;

    // Mappings of DynamicRange to recording capability information. The mappings are divided
    // into two collections based on the key's (DynamicRange) category, one for specified
    // DynamicRange and one for others. Specified DynamicRange means that its bit depth and
    // format are specified values, not some wildcards, such as: FORMAT_UNSPECIFIED,
    // FORMAT_HDR_UNSPECIFIED or BIT_DEPTH_UNSPECIFIED.
    private final Map<DynamicRange, CapabilitiesByQuality>
            mCapabilitiesMapForFullySpecifiedDynamicRange = new HashMap<>();
    private final Map<DynamicRange, CapabilitiesByQuality>
            mCapabilitiesMapForNonFullySpecifiedDynamicRange = new HashMap<>();

    /**
     * Creates a RecorderVideoCapabilities.
     *
     * @param cameraInfoInternal the cameraInfo.
     * @param backupVideoProfileValidator the validator used to check if the derived backup HDR
     *                                    {@link VideoProfileProxy} is valid.
     * @throws IllegalArgumentException if unable to get the capability information from the
     *                                  CameraInfo.
     */
    @VisibleForTesting
    RecorderVideoCapabilities(@NonNull CameraInfoInternal cameraInfoInternal,
            @NonNull Function<VideoProfileProxy, VideoProfileProxy> backupVideoProfileValidator) {
        EncoderProfilesProvider encoderProfilesProvider =
                cameraInfoInternal.getEncoderProfilesProvider();

        // Add backup HDR video information. In the initial version, only HLG10 profile is added.
        if (isHlg10SupportedByCamera(cameraInfoInternal)) {
            encoderProfilesProvider = new BackupHdrProfileEncoderProfilesProvider(
                    encoderProfilesProvider, backupVideoProfileValidator);
        }

        // Workaround resolution quirk.
        Quirks cameraQuirks = cameraInfoInternal.getCameraQuirks();
        encoderProfilesProvider = new ResolutionValidatedEncoderProfilesProvider(
                encoderProfilesProvider, cameraQuirks);

        // Workaround quality quirk.
        Quirks deviceQuirks = DeviceQuirks.getAll();
        encoderProfilesProvider = new QualityValidatedEncoderProfilesProvider(
                encoderProfilesProvider, cameraInfoInternal, deviceQuirks);
        mProfilesProvider = encoderProfilesProvider;

        // Group by dynamic range.
        for (DynamicRange dynamicRange : cameraInfoInternal.getSupportedDynamicRanges()) {
            // Filter video profiles to include only the profiles match with the target dynamic
            // range.
            EncoderProfilesProvider constrainedProvider =
                    new DynamicRangeMatchedEncoderProfilesProvider(mProfilesProvider, dynamicRange);
            CapabilitiesByQuality capabilities = new CapabilitiesByQuality(constrainedProvider);

            if (!capabilities.getSupportedQualities().isEmpty()) {
                mCapabilitiesMapForFullySpecifiedDynamicRange.put(dynamicRange, capabilities);
            }
        }
    }

    /**
     * Gets RecorderVideoCapabilities by the {@link CameraInfo}.
     *
     * <p>Should not be called directly, use {@link Recorder#getVideoCapabilities(CameraInfo)}
     * instead.
     *
     * <p>The {@link BackupHdrProfileEncoderProfilesProvider#DEFAULT_VALIDATOR} is used for
     * validating the derived backup HDR {@link VideoProfileProxy}.
     */
    @NonNull
    static RecorderVideoCapabilities from(@NonNull CameraInfo cameraInfo) {
        return new RecorderVideoCapabilities((CameraInfoInternal) cameraInfo, DEFAULT_VALIDATOR);
    }

    @NonNull
    @Override
    public Set<DynamicRange> getSupportedDynamicRanges() {
        return mCapabilitiesMapForFullySpecifiedDynamicRange.keySet();
    }

    @NonNull
    @Override
    public List<Quality> getSupportedQualities(@NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? new ArrayList<>() : capabilities.getSupportedQualities();
    }

    @Override
    public boolean isQualitySupported(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities != null && capabilities.isQualitySupported(quality);
    }

    @Nullable
    @Override
    public VideoValidatedEncoderProfilesProxy getProfiles(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? null : capabilities.getProfiles(quality);
    }

    @Nullable
    @Override
    public VideoValidatedEncoderProfilesProxy findHighestSupportedEncoderProfilesFor(
            @NonNull Size size, @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? null : capabilities.findHighestSupportedEncoderProfilesFor(
                size);
    }

    @NonNull
    @Override
    public Quality findHighestSupportedQualityFor(@NonNull Size size,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? Quality.NONE : capabilities.findHighestSupportedQualityFor(
                size);
    }

    @Nullable
    private CapabilitiesByQuality getCapabilities(@NonNull DynamicRange dynamicRange) {
        if (isFullySpecified(dynamicRange)) {
            return mCapabilitiesMapForFullySpecifiedDynamicRange.get(dynamicRange);
        }

        // Handle dynamic range that is not fully specified.
        if (mCapabilitiesMapForNonFullySpecifiedDynamicRange.containsKey(dynamicRange)) {
            return mCapabilitiesMapForNonFullySpecifiedDynamicRange.get(dynamicRange);
        } else {
            CapabilitiesByQuality capabilities =
                    generateCapabilitiesForNonFullySpecifiedDynamicRange(dynamicRange);
            mCapabilitiesMapForNonFullySpecifiedDynamicRange.put(dynamicRange, capabilities);
            return capabilities;
        }
    }

    private static boolean isHlg10SupportedByCamera(
            @NonNull CameraInfoInternal cameraInfoInternal) {
        Set<DynamicRange> dynamicRanges = cameraInfoInternal.getSupportedDynamicRanges();
        for (DynamicRange dynamicRange : dynamicRanges) {
            Integer format = dynamicRange.getFormat();
            int bitDepth = dynamicRange.getBitDepth();
            if (format.equals(FORMAT_HLG) && bitDepth == DynamicRange.BIT_DEPTH_10_BIT) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    private CapabilitiesByQuality generateCapabilitiesForNonFullySpecifiedDynamicRange(
            @NonNull DynamicRange dynamicRange) {
        if (!canResolve(dynamicRange, getSupportedDynamicRanges())) {
            return null;
        }

        // Filter video profiles to include only the profiles match with the target dynamic
        // range.
        EncoderProfilesProvider constrainedProvider =
                new DynamicRangeMatchedEncoderProfilesProvider(mProfilesProvider, dynamicRange);
        return new CapabilitiesByQuality(constrainedProvider);
    }

    /**
     * Returns {@code true} if the test dynamic range can resolve to the fully specified dynamic
     * range set.
     *
     * <p>A range can resolve if test fields are unspecified and appropriately match the fields
     * of the fully specified dynamic range, or the test fields exactly match the fields of
     * the fully specified dynamic range.
     */
    private static boolean canResolve(@NonNull DynamicRange dynamicRangeToTest,
            @NonNull Set<DynamicRange> fullySpecifiedDynamicRanges) {
        if (isFullySpecified(dynamicRangeToTest)) {
            return fullySpecifiedDynamicRanges.contains(dynamicRangeToTest);
        } else {
            for (DynamicRange fullySpecifiedDynamicRange : fullySpecifiedDynamicRanges) {
                if (canMatchBitDepth(dynamicRangeToTest, fullySpecifiedDynamicRange)
                        && canMatchFormat(dynamicRangeToTest, fullySpecifiedDynamicRange)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static boolean canMatchBitDepth(@NonNull DynamicRange dynamicRangeToTest,
            @NonNull DynamicRange fullySpecifiedDynamicRange) {
        Preconditions.checkState(isFullySpecified(fullySpecifiedDynamicRange), "Fully specified "
                + "range is not actually fully specified.");
        if (dynamicRangeToTest.getBitDepth() == BIT_DEPTH_UNSPECIFIED) {
            return true;
        }

        return dynamicRangeToTest.getBitDepth() == fullySpecifiedDynamicRange.getBitDepth();
    }

    private static boolean canMatchFormat(@NonNull DynamicRange dynamicRangeToTest,
            @NonNull DynamicRange fullySpecifiedDynamicRange) {
        Preconditions.checkState(isFullySpecified(fullySpecifiedDynamicRange), "Fully specified "
                + "range is not actually fully specified.");
        int formatToTest = dynamicRangeToTest.getFormat();
        if (formatToTest == FORMAT_UNSPECIFIED) {
            return true;
        }

        int fullySpecifiedFormat = fullySpecifiedDynamicRange.getFormat();
        if (formatToTest == FORMAT_HDR_UNSPECIFIED && fullySpecifiedFormat != FORMAT_SDR) {
            return true;
        }

        return formatToTest == fullySpecifiedFormat;
    }

    private static boolean isFullySpecified(@NonNull DynamicRange dynamicRange) {
        return dynamicRange.getFormat() != FORMAT_UNSPECIFIED
                && dynamicRange.getFormat() != FORMAT_HDR_UNSPECIFIED
                && dynamicRange.getBitDepth() != BIT_DEPTH_UNSPECIFIED;
    }

    /**
     * This class implements the video capabilities query logic related to quality and resolution.
     */
    @VisibleForTesting
    static class CapabilitiesByQuality {

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

        CapabilitiesByQuality(@NonNull EncoderProfilesProvider provider) {
            // Construct supported profile map.
            for (Quality quality : Quality.getSortedQualities()) {
                EncoderProfilesProxy profiles = getEncoderProfiles(quality, provider);
                if (profiles == null) {
                    continue;
                }

                // Validate that EncoderProfiles contain video information.
                Logger.d(TAG, "profiles = " + profiles);
                VideoValidatedEncoderProfilesProxy validatedProfiles = toValidatedProfiles(
                        profiles);
                if (validatedProfiles == null) {
                    Logger.w(TAG, "EncoderProfiles of quality " + quality + " has no video "
                            + "validated profiles.");
                    continue;
                }

                EncoderProfilesProxy.VideoProfileProxy videoProfile =
                        validatedProfiles.getDefaultVideoProfile();
                Size size = new Size(videoProfile.getWidth(), videoProfile.getHeight());
                mAreaSortedSizeToQualityMap.put(size, quality);

                // SortedQualities is from size large to small.
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

        @NonNull
        public List<Quality> getSupportedQualities() {
            return new ArrayList<>(mSupportedProfilesMap.keySet());
        }

        public boolean isQualitySupported(@NonNull Quality quality) {
            checkQualityConstantsOrThrow(quality);
            return getProfiles(quality) != null;
        }

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

        @NonNull
        public Quality findHighestSupportedQualityFor(@NonNull Size size) {
            Map.Entry<Size, Quality> ceilEntry = mAreaSortedSizeToQualityMap.ceilingEntry(size);

            if (ceilEntry != null) {
                // The ceiling entry will either be equivalent or higher in size, so always
                // return it.
                return ceilEntry.getValue();
            } else {
                // If a ceiling entry doesn't exist and a floor entry exists, it is the closest
                // we have, so return it.
                Map.Entry<Size, Quality> floorEntry = mAreaSortedSizeToQualityMap.floorEntry(size);
                if (floorEntry != null) {
                    return floorEntry.getValue();
                }
            }

            // No supported qualities.
            return Quality.NONE;
        }

        @Nullable
        private EncoderProfilesProxy getEncoderProfiles(@NonNull Quality quality,
                @NonNull EncoderProfilesProvider provider) {
            Preconditions.checkState(quality instanceof Quality.ConstantQuality,
                    "Currently only support ConstantQuality");
            int qualityValue = ((Quality.ConstantQuality) quality).getValue();

            return provider.getAll(qualityValue);
        }

        @Nullable
        private VideoValidatedEncoderProfilesProxy toValidatedProfiles(
                @NonNull EncoderProfilesProxy profiles) {
            // According to the document, the first profile is the default video profile.
            List<EncoderProfilesProxy.VideoProfileProxy> videoProfiles =
                    profiles.getVideoProfiles();
            if (videoProfiles.isEmpty()) {
                return null;
            }

            return VideoValidatedEncoderProfilesProxy.from(profiles);
        }

        private static void checkQualityConstantsOrThrow(@NonNull Quality quality) {
            Preconditions.checkArgument(Quality.containsQuality(quality),
                    "Unknown quality: " + quality);
        }
    }
}
