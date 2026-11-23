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

import static androidx.camera.core.DynamicRange.SDR;
import static androidx.camera.core.internal.utils.SizeUtil.findNearestHigherFor;
import static androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR;
import static androidx.camera.video.internal.utils.DynamicRangeUtil.isHdrSettingsMatched;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.createDefaultEncoderProfiles;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.createDefaultVideoProfile;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.deriveHdrVideoProfile;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.deriveVideoProfile;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.validateOrAdapt;
import static androidx.core.util.Preconditions.checkArgument;

import static java.util.Objects.requireNonNull;

import android.util.Size;

import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.video.CapabilitiesByQuality;
import androidx.camera.video.Quality;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * An implementation that provides the {@link EncoderProfilesProxy} with the missing video profiles
 * of the target qualities and dynamic ranges added.
 *
 * <p>For each target quality, a video profile is explored for each target dynamic range that the
 * base provider doesn't provide. The first of the following candidates that is supported by a video
 * encoder is used:
 * <ol>
 *     <li>Derived from the nearest higher profile of the same dynamic range, with the size scaled.
 *     A profile of the same dynamic range usually gives the best reference.
 *     <li>For a 10-bit HDR dynamic range, derived from the SDR profile of the same quality.
 *     <li>Created from default values, see
 *     {@code EncoderProfilesUtil.createDefaultVideoProfile()}.
 * </ol>
 *
 * <p>An explored profile uses the size of the existing profiles of the quality. If the quality has
 * no profile, the first typical size of the quality that is supported by the camera and a video
 * encoder is used. The audio profiles, duration and file format are taken from the existing
 * profiles of the quality, or the profiles used as the reference, or default values in this order.
 *
 * <p>Qualities that are not target qualities are provided by the base provider as is.
 */
public class QualityExploredEncoderProfilesProvider implements EncoderProfilesProvider {
    private final EncoderProfilesProvider mBaseEncoderProfilesProvider;
    private final Set<Quality> mTargetQualities;
    private final Set<Size> mCameraSupportedResolutions;
    private final List<DynamicRange> mTargetDynamicRanges;
    private final VideoEncoderInfo.Finder mVideoEncoderInfoFinder;
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
            VideoEncoderInfo.@NonNull Finder videoEncoderInfoFinder) {
        checkFullySpecifiedOrThrow(targetDynamicRanges);
        mBaseEncoderProfilesProvider = baseProvider;
        mTargetQualities = new HashSet<>(targetQualities);
        mTargetDynamicRanges = new ArrayList<>(targetDynamicRanges);
        // Explore SDR first, so an HDR profile can be derived from an explored SDR profile.
        Collections.sort(mTargetDynamicRanges,
                (dr1, dr2) -> Boolean.compare(dr1.is10BitHdr(), dr2.is10BitHdr()));
        mCameraSupportedResolutions = new HashSet<>(cameraSupportedResolutions);
        mVideoEncoderInfoFinder = videoEncoderInfoFinder;
    }

    @Override
    public boolean hasProfile(int quality) {
        return getProfilesInternal(quality) != null;
    }

    @Override
    public @Nullable EncoderProfilesProxy getAll(int quality) {
        return getProfilesInternal(quality);
    }

    private @Nullable EncoderProfilesProxy getProfilesInternal(int qualityValue) {
        if (mEncoderProfilesCache.containsKey(qualityValue)) {
            return mEncoderProfilesCache.get(qualityValue);
        }
        EncoderProfilesProxy profiles = mBaseEncoderProfilesProvider.getAll(qualityValue);
        Quality.ConstantQuality quality = findQualityInTargetQualities(qualityValue);
        if (quality != null && !hasMatchedVideoProfileForAllTargetDynamicRanges(profiles)) {
            profiles = mergeEncoderProfiles(profiles, exploreProfiles(quality, profiles));
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

    private @Nullable EncoderProfilesProxy exploreProfiles(
            Quality.@NonNull ConstantQuality quality,
            @Nullable EncoderProfilesProxy qualityMappedProfiles) {
        checkArgument(mTargetQualities.contains(quality));
        List<VideoProfileProxy> existingVideoProfiles = qualityMappedProfiles != null
                ? qualityMappedProfiles.getVideoProfiles() : Collections.emptyList();
        for (Size size : getTargetSizes(quality, existingVideoProfiles)) {
            TreeMap<Size, EncoderProfilesProxy> areaSortedSizeToEncoderProfilesMap = new TreeMap<>(
                    new CompareSizesByArea());
            List<VideoProfileProxy> generatedVideoProfiles = new ArrayList<>();
            for (DynamicRange dynamicRange : mTargetDynamicRanges) {
                // Different target dynamic ranges may match the same video profile, e.g. HDR10 and
                // its SMPTE ST 2094-50 variant, so also check the generated video profiles.
                if (hasMatchedVideoProfileForDynamicRange(qualityMappedProfiles, dynamicRange)
                        || hasMatchedVideoProfileForDynamicRange(
                                generatedVideoProfiles, dynamicRange)) {
                    continue;
                }
                VideoProfileProxy videoProfile = null;
                // Find a nearest higher EncoderProfiles by the target dynamic range.
                VideoValidatedEncoderProfilesProxy referenceProfiles =
                        getCapabilitiesByQualityFor(dynamicRange)
                                .findNearestHigherSupportedEncoderProfilesFor(size);
                if (referenceProfiles != null) {
                    videoProfile = deriveFromReference(referenceProfiles, size);
                    if (videoProfile != null) {
                        // Add the reference to the candidates of base EncoderProfiles.
                        areaSortedSizeToEncoderProfilesMap.put(
                                referenceProfiles.getDefaultVideoProfile().getResolution(),
                                referenceProfiles);
                    }
                }
                if (videoProfile == null) {
                    List<VideoProfileProxy> videoProfiles = new ArrayList<>(existingVideoProfiles);
                    videoProfiles.addAll(generatedVideoProfiles);
                    videoProfile = deriveFromSdr(dynamicRange, videoProfiles);
                }
                if (videoProfile == null) {
                    videoProfile = createFromDefaults(dynamicRange, size);
                }
                if (videoProfile != null) {
                    generatedVideoProfiles.add(videoProfile);
                }
            }
            if (!generatedVideoProfiles.isEmpty()) {
                // Use the nearest higher EncoderProfiles as base EncoderProfiles.
                EncoderProfilesProxy baseProfiles =
                        findNearestHigherFor(size, areaSortedSizeToEncoderProfilesMap);
                if (baseProfiles == null) {
                    return createDefaultEncoderProfiles(generatedVideoProfiles);
                }
                return ImmutableEncoderProfilesProxy.create(
                        baseProfiles.getDefaultDurationSeconds(),
                        baseProfiles.getRecommendedFileFormat(),
                        baseProfiles.getAudioProfiles(),
                        generatedVideoProfiles);
            }
        }
        return null;
    }

    private @NonNull List<Size> getTargetSizes(Quality.@NonNull ConstantQuality quality,
            @NonNull List<VideoProfileProxy> existingVideoProfiles) {
        List<Size> sizes = new ArrayList<>();
        // Use the size of the existing profiles, so all profiles of a quality have the same size.
        if (!existingVideoProfiles.isEmpty()) {
            sizes.add(existingVideoProfiles.get(0).getResolution());
            return sizes;
        }
        for (Size size : quality.getTypicalSizes()) {
            if (mCameraSupportedResolutions.contains(size)) {
                sizes.add(size);
            }
        }
        return sizes;
    }

    /**
     * Derives a video profile of the given size from the default video profile of
     * {@code referenceProfiles}.
     */
    private @Nullable VideoProfileProxy deriveFromReference(
            @NonNull VideoValidatedEncoderProfilesProxy referenceProfiles, @NonNull Size size) {
        VideoProfileProxy baseVideoProfile = referenceProfiles.getDefaultVideoProfile();
        VideoEncoderInfo encoderInfo = mVideoEncoderInfoFinder.find(
                baseVideoProfile.getMediaType());
        if (encoderInfo == null) {
            return null;
        }
        // Generate VideoProfile from base VideoProfile and new size, and check if it is valid for
        // the encoder.
        return validateOrAdapt(
                deriveVideoProfile(baseVideoProfile, size, encoderInfo.getSupportedBitrateRange()),
                mVideoEncoderInfoFinder);
    }

    /** Derives a 10-bit HDR video profile from the SDR profile in {@code videoProfiles}. */
    private @Nullable VideoProfileProxy deriveFromSdr(@NonNull DynamicRange dynamicRange,
            @NonNull List<VideoProfileProxy> videoProfiles) {
        if (!dynamicRange.is10BitHdr()) {
            return null;
        }
        VideoProfileProxy sdrProfile = findMatchedVideoProfile(videoProfiles, SDR);
        if (sdrProfile == null) {
            return null;
        }
        return validateOrAdapt(deriveHdrVideoProfile(sdrProfile, dynamicRange),
                mVideoEncoderInfoFinder);
    }

    private @Nullable VideoProfileProxy createFromDefaults(@NonNull DynamicRange dynamicRange,
            @NonNull Size size) {
        return validateOrAdapt(createDefaultVideoProfile(size, dynamicRange),
                mVideoEncoderInfoFinder);
    }

    private Quality.@Nullable ConstantQuality findQualityInTargetQualities(int qualityValue) {
        for (Quality quality : mTargetQualities) {
            Quality.ConstantQuality constantQuality = (Quality.ConstantQuality) quality;
            if (constantQuality.getQualityValue(QUALITY_SOURCE_REGULAR) == qualityValue) {
                return constantQuality;
            }
        }
        return null;
    }

    private @NonNull CapabilitiesByQuality getCapabilitiesByQualityFor(
            @NonNull DynamicRange dynamicRange) {
        if (mDynamicRangeToCapabilitiesMap.containsKey(dynamicRange)) {
            return requireNonNull(mDynamicRangeToCapabilitiesMap.get(dynamicRange));
        }
        EncoderProfilesProvider constrainedProvider =
                new DynamicRangeMatchedEncoderProfilesProvider(mBaseEncoderProfilesProvider,
                        dynamicRange);
        CapabilitiesByQuality capabilities = new CapabilitiesByQuality(
                constrainedProvider, QUALITY_SOURCE_REGULAR);
        mDynamicRangeToCapabilitiesMap.put(dynamicRange, capabilities);
        return capabilities;
    }

    private static @Nullable EncoderProfilesProxy mergeEncoderProfiles(
            @Nullable EncoderProfilesProxy baseProfiles,
            @Nullable EncoderProfilesProxy profilesToMerge) {
        if (profilesToMerge == null) {
            return baseProfiles;
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
        return hasMatchedVideoProfileForDynamicRange(encoderProfiles.getVideoProfiles(),
                dynamicRange);
    }

    private static boolean hasMatchedVideoProfileForDynamicRange(
            @NonNull List<VideoProfileProxy> videoProfiles, @NonNull DynamicRange dynamicRange) {
        return findMatchedVideoProfile(videoProfiles, dynamicRange) != null;
    }

    private static @Nullable VideoProfileProxy findMatchedVideoProfile(
            @NonNull List<VideoProfileProxy> videoProfiles, @NonNull DynamicRange dynamicRange) {
        for (VideoProfileProxy videoProfile : videoProfiles) {
            if (isHdrSettingsMatched(videoProfile, dynamicRange)) {
                return videoProfile;
            }
        }
        return null;
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
