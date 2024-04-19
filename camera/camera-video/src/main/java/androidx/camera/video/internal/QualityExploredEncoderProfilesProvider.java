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

package androidx.camera.video.internal;

import static androidx.camera.core.internal.utils.SizeUtil.findNearestHigherFor;
import static androidx.camera.video.internal.config.VideoConfigUtil.toVideoEncoderConfig;
import static androidx.camera.video.internal.utils.DynamicRangeUtil.isHdrSettingsMatched;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.deriveVideoProfile;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.video.CapabilitiesByQuality;
import androidx.camera.video.Quality;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * An implementation that provides the {@link EncoderProfilesProxy} with additional quality added.
 *
 * <p>The basic EncoderProfilesProvider references to {@link android.media.CamcorderProfile}.
 * This class explores more camera and codec supported qualities in addition to CamcorderProfile.
 * When a quality is explored, the corresponding profile will be derived from a nearest higher
 * supported profile.
 */
@RequiresApi(21)
public class QualityExploredEncoderProfilesProvider implements EncoderProfilesProvider {
    private final EncoderProfilesProvider mBaseEncoderProfilesProvider;
    private final Set<Quality> mTargetQualities;
    private final Set<Size> mCameraSupportedResolutions;
    private final Set<DynamicRange> mTargetDynamicRanges;
    private final Function<VideoEncoderConfig, VideoEncoderInfo> mVideoEncoderInfoFinder;
    private final Map<Integer, EncoderProfilesProxy> mEncoderProfilesCache = new HashMap<>();
    private final Map<DynamicRange, CapabilitiesByQuality> mDynamicRangeToCapabilitiesMap =
            new HashMap<>();

    /**
     * Creates a QualityExploredEncoderProfilesProvider.
     *
     * @param baseProvider               the base EncoderProfilesProvider.
     * @param targetQualities            the target qualities to be explored.
     * @param targetDynamicRanges        the target dynamic range to explore with. Must be fully
     *                                   specified dynamic ranges.
     * @param cameraSupportedResolutions the camera supported resolutions.
     * @param videoEncoderInfoFinder     the VideEncoderInfo finder.
     */
    public QualityExploredEncoderProfilesProvider(
            @NonNull EncoderProfilesProvider baseProvider,
            @NonNull Collection<Quality> targetQualities,
            @NonNull Collection<DynamicRange> targetDynamicRanges,
            @NonNull Collection<Size> cameraSupportedResolutions,
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder) {
        checkFullySpecifiedOrThrow(targetDynamicRanges);
        mBaseEncoderProfilesProvider = baseProvider;
        mTargetQualities = new HashSet<>(targetQualities);
        mTargetDynamicRanges = new HashSet<>(targetDynamicRanges);
        mCameraSupportedResolutions = new HashSet<>(cameraSupportedResolutions);
        mVideoEncoderInfoFinder = videoEncoderInfoFinder;
    }

    @Override
    public boolean hasProfile(int quality) {
        return getProfilesInternal(quality) != null;
    }

    @Nullable
    @Override
    public EncoderProfilesProxy getAll(int quality) {
        return getProfilesInternal(quality);
    }

    @Nullable
    private EncoderProfilesProxy getProfilesInternal(int qualityValue) {
        if (mEncoderProfilesCache.containsKey(qualityValue)) {
            return mEncoderProfilesCache.get(qualityValue);
        }
        EncoderProfilesProxy profiles = mBaseEncoderProfilesProvider.getAll(qualityValue);
        Quality.ConstantQuality quality = findQualityInTargetQualities(qualityValue);
        if (quality != null && !hasMatchedVideoProfileForAllTargetDynamicRanges(profiles)) {
            profiles = mergeEncoderProfiles(profiles, exploreProfiles(quality));
        }
        mEncoderProfilesCache.put(qualityValue, profiles);
        return profiles;
    }

    private boolean hasMatchedVideoProfileForAllTargetDynamicRanges(
            @Nullable EncoderProfilesProxy encoderProfiles) {
        if (encoderProfiles == null) {
            return false;
        }
        // Return true only if the encoderProfiles contains all target DynamicRange.
        for (DynamicRange dynamicRange : mTargetDynamicRanges) {
            if (!hasMatchedVideoProfileForDynamicRange(encoderProfiles, dynamicRange)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private EncoderProfilesProxy exploreProfiles(@NonNull Quality.ConstantQuality quality) {
        checkArgument(mTargetQualities.contains(quality));
        EncoderProfilesProxy qualityMappedProfiles =
                mBaseEncoderProfilesProvider.getAll(quality.getValue());
        for (Size size : quality.getTypicalSizes()) {
            if (!mCameraSupportedResolutions.contains(size)) {
                continue;
            }
            TreeMap<Size, EncoderProfilesProxy> areaSortedSizeToEncoderProfilesMap = new TreeMap<>(
                    new CompareSizesByArea());
            List<VideoProfileProxy> generatedVideoProfiles = new ArrayList<>();
            for (DynamicRange dynamicRange : mTargetDynamicRanges) {
                if (hasMatchedVideoProfileForDynamicRange(qualityMappedProfiles, dynamicRange)) {
                    continue;
                }
                // Find a nearest higher EncoderProfiles by the target dynamic range.
                VideoValidatedEncoderProfilesProxy encoderProfiles =
                        getCapabilitiesByQualityFor(dynamicRange)
                                .findNearestHigherSupportedEncoderProfilesFor(size);
                if (encoderProfiles == null) {
                    continue;
                }
                VideoProfileProxy baseVideoProfile = encoderProfiles.getDefaultVideoProfile();
                // Find VideoEncoderInfo from VideoProfile.
                VideoEncoderConfig encoderConfig = toVideoEncoderConfig(baseVideoProfile);
                VideoEncoderInfo encoderInfo = mVideoEncoderInfoFinder.apply(encoderConfig);
                // Check if size is valid for the Encoder.
                if (encoderInfo == null || !encoderInfo.isSizeSupportedAllowSwapping(
                        size.getWidth(), size.getHeight())) {
                    continue;
                }
                // Add the encoderProfiles to the candidates of base EncoderProfiles.
                areaSortedSizeToEncoderProfilesMap.put(
                        new Size(baseVideoProfile.getWidth(), baseVideoProfile.getHeight()),
                        encoderProfiles);
                // Generate VideoProfile from base VideoProfile and new size.
                generatedVideoProfiles.add(
                        deriveVideoProfile(baseVideoProfile, size,
                                encoderInfo.getSupportedBitrateRange()));
            }
            if (!generatedVideoProfiles.isEmpty()) {
                // Use the nearest higher EncoderProfiles as base EncoderProfiles.
                EncoderProfilesProxy baseProfiles = requireNonNull(
                        findNearestHigherFor(size, areaSortedSizeToEncoderProfilesMap));
                return ImmutableEncoderProfilesProxy.create(
                        baseProfiles.getDefaultDurationSeconds(),
                        baseProfiles.getRecommendedFileFormat(),
                        baseProfiles.getAudioProfiles(),
                        generatedVideoProfiles);
            }
        }
        return null;
    }

    @Nullable
    private Quality.ConstantQuality findQualityInTargetQualities(int qualityValue) {
        for (Quality quality : mTargetQualities) {
            Quality.ConstantQuality constantQuality = (Quality.ConstantQuality) quality;
            if (constantQuality.getValue() == qualityValue) {
                return constantQuality;
            }
        }
        return null;
    }

    @NonNull
    private CapabilitiesByQuality getCapabilitiesByQualityFor(@NonNull DynamicRange dynamicRange) {
        if (mDynamicRangeToCapabilitiesMap.containsKey(dynamicRange)) {
            return requireNonNull(mDynamicRangeToCapabilitiesMap.get(dynamicRange));
        }
        EncoderProfilesProvider constrainedProvider =
                new DynamicRangeMatchedEncoderProfilesProvider(mBaseEncoderProfilesProvider,
                        dynamicRange);
        CapabilitiesByQuality capabilities = new CapabilitiesByQuality(constrainedProvider);
        mDynamicRangeToCapabilitiesMap.put(dynamicRange, capabilities);
        return capabilities;
    }

    @Nullable
    private static EncoderProfilesProxy mergeEncoderProfiles(
            @Nullable EncoderProfilesProxy baseProfiles,
            @Nullable EncoderProfilesProxy profilesToMerge) {
        if (baseProfiles == null && profilesToMerge == null) {
            return null;
        }
        int duration = baseProfiles != null ? baseProfiles.getDefaultDurationSeconds() :
                profilesToMerge.getDefaultDurationSeconds();
        int format = baseProfiles != null ? baseProfiles.getRecommendedFileFormat() :
                profilesToMerge.getRecommendedFileFormat();
        List<EncoderProfilesProxy.AudioProfileProxy> audioProfiles = baseProfiles != null
                ? baseProfiles.getAudioProfiles() : profilesToMerge.getAudioProfiles();
        List<EncoderProfilesProxy.VideoProfileProxy> videoProfiles = new ArrayList<>();
        if (baseProfiles != null) {
            videoProfiles.addAll(baseProfiles.getVideoProfiles());
        }
        if (profilesToMerge != null) {
            videoProfiles.addAll(profilesToMerge.getVideoProfiles());
        }
        return EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                duration,
                format,
                audioProfiles,
                videoProfiles
        );
    }

    private static boolean hasMatchedVideoProfileForDynamicRange(
            @Nullable EncoderProfilesProxy encoderProfiles,
            @NonNull DynamicRange dynamicRange) {
        if (encoderProfiles == null) {
            return false;
        }
        for (VideoProfileProxy videoProfile : encoderProfiles.getVideoProfiles()) {
            if (isHdrSettingsMatched(videoProfile, dynamicRange)) {
                return true;
            }
        }
        return false;
    }

    private static void checkFullySpecifiedOrThrow(
            @NonNull Collection<DynamicRange> dynamicRanges) {
        for (DynamicRange dynamicRange : dynamicRanges) {
            if (!dynamicRange.isFullySpecified()) {
                throw new IllegalArgumentException(
                        "Contains non-fully specified DynamicRange: " + dynamicRange);
            }
        }
    }
}
