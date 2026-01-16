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

import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.DynamicRanges;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.video.Quality.QualitySource;
import androidx.camera.video.internal.DynamicRangeMatchedEncoderProfilesProvider;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * RecorderVideoCapabilities is used to query video recording capabilities related to Recorder.
 *
 * <p>The {@link EncoderProfilesProxy} queried from RecorderVideoCapabilities will contain
 * {@link VideoProfileProxy}s matches with the target {@link DynamicRange}.
 *
 * @see Recorder#getVideoCapabilities(CameraInfo)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class RecorderVideoCapabilities implements VideoCapabilities {
    private final EncoderProfilesProvider mProfilesProvider;
    private final boolean mIsStabilizationSupported;
    private final @QualitySource int mQualitySource;

    // Mappings of DynamicRange to recording capability information. The mappings are divided
    // into two collections based on the key's (DynamicRange) category, one for specified
    // DynamicRange and one for others. Specified DynamicRange means that its bit depth and
    // format are specified values, not some wildcards, such as: ENCODING_UNSPECIFIED,
    // ENCODING_HDR_UNSPECIFIED or BIT_DEPTH_UNSPECIFIED.
    private final Map<DynamicRange, CapabilitiesByQuality>
            mCapabilitiesMapForFullySpecifiedDynamicRange = new HashMap<>();
    private final Map<DynamicRange, CapabilitiesByQuality>
            mCapabilitiesMapForNonFullySpecifiedDynamicRange = new HashMap<>();

    /**
     * Creates a RecorderVideoCapabilities.
     *
     * @param profilesProvider        the encoder profiles provider.
     * @param cameraInfo              the cameraInfo.
     * @param qualitySource           the quality source.
     * @throws IllegalArgumentException if unable to get the capability information from the
     *                                  CameraInfo or the videoCapabilitiesSource is not supported.
     */
    RecorderVideoCapabilities(
            @NonNull EncoderProfilesProvider profilesProvider,
            @Quality.QualitySource int qualitySource,
            @NonNull CameraInfoInternal cameraInfo
    ) {
        mQualitySource = qualitySource;
        mProfilesProvider = profilesProvider;

        // Group by dynamic range.
        for (DynamicRange dynamicRange : cameraInfo.getSupportedDynamicRanges()) {
            // Filter video profiles to include only the profiles match with the target dynamic
            // range.
            EncoderProfilesProvider constrainedProvider =
                    new DynamicRangeMatchedEncoderProfilesProvider(mProfilesProvider, dynamicRange);
            CapabilitiesByQuality capabilities = new CapabilitiesByQuality(constrainedProvider,
                    mQualitySource);

            if (!capabilities.getSupportedQualities().isEmpty()) {
                mCapabilitiesMapForFullySpecifiedDynamicRange.put(dynamicRange, capabilities);
            }
        }

        // Video stabilization
        mIsStabilizationSupported = cameraInfo.isVideoStabilizationSupported();
    }

    @Override
    public @NonNull Set<DynamicRange> getSupportedDynamicRanges() {
        return mCapabilitiesMapForFullySpecifiedDynamicRange.keySet();
    }

    @Override
    public @NonNull List<Quality> getSupportedQualities(@NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? new ArrayList<>() : capabilities.getSupportedQualities();
    }

    @Override
    public boolean isQualitySupported(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities != null && capabilities.isQualitySupported(quality);
    }

    @Override
    public boolean isStabilizationSupported() {
        return mIsStabilizationSupported;
    }

    @Override
    public @Nullable Size getResolution(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? null : capabilities.getResolution(quality);
    }

    @Override
    public @Nullable VideoValidatedEncoderProfilesProxy getProfiles(@NonNull Quality quality,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? null : capabilities.getProfiles(quality);
    }

    @Override
    public @Nullable VideoValidatedEncoderProfilesProxy
            findNearestHigherSupportedEncoderProfilesFor(
                    @NonNull Size size, @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? null
                : capabilities.findNearestHigherSupportedEncoderProfilesFor(size);
    }

    @Override
    public @NonNull Quality findNearestHigherSupportedQualityFor(@NonNull Size size,
            @NonNull DynamicRange dynamicRange) {
        CapabilitiesByQuality capabilities = getCapabilities(dynamicRange);
        return capabilities == null ? Quality.NONE
                : capabilities.findNearestHigherSupportedQualityFor(size);
    }

    private @Nullable CapabilitiesByQuality getCapabilities(@NonNull DynamicRange dynamicRange) {
        if (dynamicRange.isFullySpecified()) {
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

    private @Nullable CapabilitiesByQuality generateCapabilitiesForNonFullySpecifiedDynamicRange(
            @NonNull DynamicRange dynamicRange) {
        if (!DynamicRanges.canResolve(dynamicRange, getSupportedDynamicRanges())) {
            return null;
        }

        // Filter video profiles to include only the profiles match with the target dynamic
        // range.
        EncoderProfilesProvider constrainedProvider =
                new DynamicRangeMatchedEncoderProfilesProvider(mProfilesProvider, dynamicRange);
        return new CapabilitiesByQuality(constrainedProvider, mQualitySource);
    }
}
