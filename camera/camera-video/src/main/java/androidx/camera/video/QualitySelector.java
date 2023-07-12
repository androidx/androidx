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

import static androidx.camera.core.DynamicRange.SDR;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * QualitySelector defines a desired quality setting that can be used to configure components
 * with quality setting requirements such as creating a
 * {@link Recorder.Builder#setQualitySelector(QualitySelector) Recorder}.
 *
 * <p>There are pre-defined quality constants that are universally used for video, such as
 * {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD} and {@link Quality#UHD}, but
 * not all of them are supported on every device since each device has its own capabilities.
 * {@link #isQualitySupported(CameraInfo, Quality)} can be used to check whether a quality is
 * supported on the device or not and {@link #getResolution(CameraInfo, Quality)} can be used to get
 * the actual resolution defined in the device. Aside from checking the qualities one by one,
 * QualitySelector provides a more convenient way to select a quality. The typical usage of
 * selecting a single desired quality is:
 * <pre>{@code
 *   QualitySelector qualitySelector = QualitySelector.from(Quality.FHD);
 * }</pre>
 * or the usage of selecting a series of qualities by desired order:
 * <pre>{@code
 *   QualitySelector qualitySelector = QualitySelector.fromOrderedList(
 *           Arrays.asList(Quality.FHD, Quality.HD, Quality.HIGHEST)
 *   );
 * }</pre>
 * The recommended way is giving a guaranteed supported quality such as {@link Quality#LOWEST} or
 * {@link Quality#HIGHEST} in the end of the desired quality list, which ensures the
 * QualitySelector can always choose a supported quality. Another way to ensure a quality will be
 * selected when none of the desired qualities are supported is to use
 * {@link #fromOrderedList(List, FallbackStrategy)} with an open-ended fallback strategy such as
 * a fallback strategy from {@link FallbackStrategy#lowerQualityOrHigherThan(Quality)}:
 * <pre>{@code
 *   QualitySelector qualitySelector = QualitySelector.fromOrderedList(
 *           Arrays.asList(Quality.UHD, Quality.FHD),
 *           FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)
 *   );
 * }</pre>
 * If UHD and FHD are not supported on the device, QualitySelector will select the quality that
 * is closest to and lower than FHD. If no lower quality is supported, the quality that is
 * closest to and higher than FHD will be selected.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public final class QualitySelector {
    private static final String TAG = "QualitySelector";

    /**
     * Gets all supported qualities on the device.
     *
     * <p>The returned list is sorted by quality size from largest to smallest. For the qualities in
     * the returned list, with the same input cameraInfo,
     * {@link #isQualitySupported(CameraInfo, Quality)} will return {@code true} and
     * {@link #getResolution(CameraInfo, Quality)} will return the corresponding resolution.
     *
     * <p>Note: Constants {@link Quality#HIGHEST} and {@link Quality#LOWEST} are not included
     * in the returned list, but their corresponding qualities are included.
     *
     * @param cameraInfo the cameraInfo
     */
    @NonNull
    public static List<Quality> getSupportedQualities(@NonNull CameraInfo cameraInfo) {
        return Recorder.getVideoCapabilities(cameraInfo).getSupportedQualities(SDR);
    }

    /**
     * Checks if the quality is supported.
     *
     * <p>Calling this method with one of the qualities contained in the returned list of
     * {@link #getSupportedQualities} will return {@code true}.
     *
     * <p>Possible values for {@code quality} include {@link Quality#LOWEST},
     * {@link Quality#HIGHEST}, {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD}
     * and {@link Quality#UHD}.
     *
     * <p>If this method is called with {@link Quality#LOWEST} or {@link Quality#HIGHEST}, it
     * will return {@code true} except the case that none of the qualities can be supported.
     *
     * @param cameraInfo the cameraInfo for checking the quality.
     * @param quality one of the quality constants.
     * @return {@code true} if the quality is supported; {@code false} otherwise.
     * @see #getSupportedQualities(CameraInfo)
     */
    public static boolean isQualitySupported(@NonNull CameraInfo cameraInfo,
            @NonNull Quality quality) {
        return Recorder.getVideoCapabilities(cameraInfo).isQualitySupported(quality, SDR);
    }

    /**
     * Gets the corresponding resolution from the input quality.
     *
     * <p>Possible values for {@code quality} include {@link Quality#LOWEST},
     * {@link Quality#HIGHEST}, {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD}
     * and {@link Quality#UHD}.
     *
     * @param cameraInfo the cameraInfo for checking the quality.
     * @param quality one of the quality constants.
     * @return the corresponding resolution from the input quality, or {@code null} if the
     * quality is not supported on the device. {@link #isQualitySupported(CameraInfo, Quality)} can
     * be used to check if the input quality is supported.
     * @throws IllegalArgumentException if quality is not one of the possible values.
     * @see #isQualitySupported
     */
    @Nullable
    public static Size getResolution(@NonNull CameraInfo cameraInfo, @NonNull Quality quality) {
        checkQualityConstantsOrThrow(quality);
        VideoCapabilities videoCapabilities = Recorder.getVideoCapabilities(cameraInfo);
        VideoValidatedEncoderProfilesProxy profiles = videoCapabilities.getProfiles(quality, SDR);
        return profiles != null ? getProfileVideoSize(profiles) : null;
    }

    /**
     * Gets a map from all supported qualities to mapped resolutions.
     *
     * @param videoCapabilities the videoCapabilities to query the supported qualities.
     * @param dynamicRange the dynamicRange to query the supported qualities.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public static Map<Quality, Size> getQualityToResolutionMap(
            @NonNull VideoCapabilities videoCapabilities, @NonNull DynamicRange dynamicRange) {
        Map<Quality, Size> map = new HashMap<>();
        for (Quality supportedQuality : videoCapabilities.getSupportedQualities(dynamicRange)) {
            map.put(supportedQuality, getProfileVideoSize(
                    requireNonNull(videoCapabilities.getProfiles(supportedQuality, dynamicRange))));
        }
        return map;
    }

    private final List<Quality> mPreferredQualityList;
    private final FallbackStrategy mFallbackStrategy;

    QualitySelector(@NonNull List<Quality> preferredQualityList,
            @NonNull FallbackStrategy fallbackStrategy) {
        Preconditions.checkArgument(
                !preferredQualityList.isEmpty() || fallbackStrategy != FallbackStrategy.NONE,
                "No preferred quality and fallback strategy.");
        mPreferredQualityList = Collections.unmodifiableList(new ArrayList<>(preferredQualityList));
        mFallbackStrategy = fallbackStrategy;
    }

    /**
     * Gets an instance of QualitySelector with a desired quality.
     *
     * @param quality the quality. Possible values include {@link Quality#LOWEST},
     * {@link Quality#HIGHEST}, {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD},
     * or {@link Quality#UHD}.
     * @return the QualitySelector instance.
     * @throws NullPointerException if {@code quality} is {@code null}.
     * @throws IllegalArgumentException if {@code quality} is not one of the possible values.
     */
    @NonNull
    public static QualitySelector from(@NonNull Quality quality) {
        return from(quality, FallbackStrategy.NONE);
    }

    /**
     * Gets an instance of QualitySelector with a desired quality and a fallback strategy.
     *
     * <p>If the quality is not supported, the fallback strategy will be applied. The fallback
     * strategy can be created by {@link FallbackStrategy} API such as
     * {@link FallbackStrategy#lowerQualityThan(Quality)}.
     *
     * @param quality the quality. Possible values include {@link Quality#LOWEST},
     * {@link Quality#HIGHEST}, {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD},
     * or {@link Quality#UHD}.
     * @param fallbackStrategy the fallback strategy that will be applied when the device does
     *                         not support {@code quality}.
     * @return the QualitySelector instance.
     * @throws NullPointerException if {@code quality} is {@code null} or {@code fallbackStrategy}
     * is {@code null}.
     * @throws IllegalArgumentException if {@code quality} is not one of the possible values.
     */
    @NonNull
    public static QualitySelector from(@NonNull Quality quality,
            @NonNull FallbackStrategy fallbackStrategy) {
        Preconditions.checkNotNull(quality, "quality cannot be null");
        Preconditions.checkNotNull(fallbackStrategy, "fallbackStrategy cannot be null");
        checkQualityConstantsOrThrow(quality);
        return new QualitySelector(singletonList(quality), fallbackStrategy);
    }

    /**
     * Gets an instance of QualitySelector with ordered desired qualities.
     *
     * <p>The final quality will be selected according to the order in the quality list.
     *
     * @param qualities the quality list. Possible values include {@link Quality#LOWEST},
     * {@link Quality#HIGHEST}, {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD},
     * or {@link Quality#UHD}.
     * @return the QualitySelector instance.
     * @throws NullPointerException if {@code qualities} is {@code null}.
     * @throws IllegalArgumentException if {@code qualities} is empty or contains a quality that is
     * not one of the possible values, including a {@code null} value.
     */
    @NonNull
    public static QualitySelector fromOrderedList(@NonNull List<Quality> qualities) {
        return fromOrderedList(qualities, FallbackStrategy.NONE);
    }

    /**
     * Gets an instance of QualitySelector with ordered desired qualities and a fallback strategy.
     *
     * <p>The final quality will be selected according to the order in the quality list.
     * If no quality is supported, the fallback strategy will be applied. The fallback
     * strategy can be created by {@link FallbackStrategy} API such as
     * {@link FallbackStrategy#lowerQualityThan(Quality)}.
     *
     * @param qualities the quality list. Possible values include {@link Quality#LOWEST},
     * {@link Quality#HIGHEST}, {@link Quality#SD}, {@link Quality#HD}, {@link Quality#FHD},
     * or {@link Quality#UHD}.
     * @param fallbackStrategy the fallback strategy that will be applied when the device does
     *                         not support those {@code qualities}.
     * @throws NullPointerException if {@code qualities} is {@code null} or
     * {@code fallbackStrategy} is {@code null}.
     * @throws IllegalArgumentException if {@code qualities} is empty or contains a quality that is
     * not one of the possible values, including a {@code null} value.
     */
    @NonNull
    public static QualitySelector fromOrderedList(@NonNull List<Quality> qualities,
            @NonNull FallbackStrategy fallbackStrategy) {
        Preconditions.checkNotNull(qualities, "qualities cannot be null");
        Preconditions.checkNotNull(fallbackStrategy, "fallbackStrategy cannot be null");
        Preconditions.checkArgument(!qualities.isEmpty(), "qualities cannot be empty");
        checkQualityConstantsOrThrow(qualities);
        return new QualitySelector(qualities, fallbackStrategy);
    }

    /**
     * Generates a sorted quality list that matches the desired quality settings.
     *
     * <p>The method bases on the desired qualities and the fallback strategy to find a matched
     * quality list on this device. The search algorithm first checks which desired quality is
     * supported according to the set sequence and adds to the returned list by order. Then the
     * fallback strategy will be applied to add more valid qualities.
     *
     * @param supportedQualities the supported qualities.
     * @return a sorted supported quality list according to the desired quality settings.
     */
    @NonNull
    List<Quality> getPrioritizedQualities(@NonNull List<Quality> supportedQualities) {
        if (supportedQualities.isEmpty()) {
            Logger.w(TAG, "No supported quality on the device.");
            return new ArrayList<>();
        }
        Logger.d(TAG, "supportedQualities = " + supportedQualities);

        // Use LinkedHashSet to prevent from duplicate quality and keep the adding order.
        Set<Quality> sortedQualities = new LinkedHashSet<>();
        // Add exact quality.
        for (Quality quality : mPreferredQualityList) {
            if (quality == Quality.HIGHEST) {
                // Highest means user want a quality as higher as possible, so the return list can
                // contain all supported resolutions from large to small.
                sortedQualities.addAll(supportedQualities);
                break;
            } else if (quality == Quality.LOWEST) {
                // Opposite to the highest
                List<Quality> reversedList = new ArrayList<>(supportedQualities);
                Collections.reverse(reversedList);
                sortedQualities.addAll(reversedList);
                break;
            } else {
                if (supportedQualities.contains(quality)) {
                    sortedQualities.add(quality);
                } else {
                    Logger.w(TAG, "quality is not supported and will be ignored: " + quality);
                }
            }
        }

        // Add quality by fallback strategy based on fallback quality.
        addByFallbackStrategy(supportedQualities, sortedQualities);

        return new ArrayList<>(sortedQualities);
    }

    @NonNull
    @Override
    public String toString() {
        return "QualitySelector{"
                + "preferredQualities=" + mPreferredQualityList
                + ", fallbackStrategy=" + mFallbackStrategy
                + "}";
    }

    private void addByFallbackStrategy(@NonNull List<Quality> supportedQualities,
            @NonNull Set<Quality> priorityQualities) {
        if (supportedQualities.isEmpty()) {
            return;
        }
        if (priorityQualities.containsAll(supportedQualities)) {
            // priorityQualities already contains all supported qualities, no need to add by
            // fallback strategy.
            return;
        }
        Logger.d(TAG, "Select quality by fallbackStrategy = " + mFallbackStrategy);
        // No fallback strategy, return directly.
        if (mFallbackStrategy == FallbackStrategy.NONE) {
            return;
        }
        Preconditions.checkState(mFallbackStrategy instanceof FallbackStrategy.RuleStrategy,
                "Currently only support type RuleStrategy");
        FallbackStrategy.RuleStrategy fallbackStrategy =
                (FallbackStrategy.RuleStrategy) mFallbackStrategy;

        // Note that fallback quality could be an unsupported quality, so all quality constants
        // need to be loaded to find the position of fallback quality.
        // The list returned from getSortedQualities() is sorted from large to small.
        List<Quality> sizeSortedQualities = Quality.getSortedQualities();
        Quality fallbackQuality;
        if (fallbackStrategy.getFallbackQuality() == Quality.HIGHEST) {
            fallbackQuality = sizeSortedQualities.get(0);
        } else if (fallbackStrategy.getFallbackQuality() == Quality.LOWEST) {
            fallbackQuality = sizeSortedQualities.get(sizeSortedQualities.size() - 1);
        } else {
            fallbackQuality = fallbackStrategy.getFallbackQuality();
        }

        int index = sizeSortedQualities.indexOf(fallbackQuality);
        Preconditions.checkState(index != -1); // Should not happen.

        // search larger supported quality
        List<Quality> largerQualities = new ArrayList<>();
        for (int i = index - 1; i >= 0; i--) {
            Quality quality = sizeSortedQualities.get(i);
            if (supportedQualities.contains(quality)) {
                largerQualities.add(quality);
            }
        }

        // search smaller supported quality
        List<Quality> smallerQualities = new ArrayList<>();
        for (int i = index + 1; i < sizeSortedQualities.size(); i++) {
            Quality quality = sizeSortedQualities.get(i);
            if (supportedQualities.contains(quality)) {
                smallerQualities.add(quality);
            }
        }

        Logger.d(TAG, "sizeSortedQualities = " + sizeSortedQualities
                + ", fallback quality = " + fallbackQuality
                + ", largerQualities = " + largerQualities
                + ", smallerQualities = " + smallerQualities);

        switch (fallbackStrategy.getFallbackRule()) {
            case FallbackStrategy.FALLBACK_RULE_HIGHER_OR_LOWER:
                priorityQualities.addAll(largerQualities);
                priorityQualities.addAll(smallerQualities);
                break;
            case FallbackStrategy.FALLBACK_RULE_HIGHER:
                priorityQualities.addAll(largerQualities);
                break;
            case FallbackStrategy.FALLBACK_RULE_LOWER_OR_HIGHER:
                priorityQualities.addAll(smallerQualities);
                priorityQualities.addAll(largerQualities);
                break;
            case FallbackStrategy.FALLBACK_RULE_LOWER:
                priorityQualities.addAll(smallerQualities);
                break;
            case FallbackStrategy.FALLBACK_RULE_NONE:
                // No-Op
                break;
            default:
                throw new AssertionError("Unhandled fallback strategy: " + mFallbackStrategy);
        }
    }

    @NonNull
    private static Size getProfileVideoSize(@NonNull VideoValidatedEncoderProfilesProxy profiles) {
        VideoProfileProxy videoProfile = profiles.getDefaultVideoProfile();
        return new Size(videoProfile.getWidth(), videoProfile.getHeight());
    }

    private static void checkQualityConstantsOrThrow(@NonNull List<Quality> qualities) {
        for (Quality quality : qualities) {
            Preconditions.checkArgument(Quality.containsQuality(quality),
                    "qualities contain invalid quality: " + quality);
        }
    }

    private static void checkQualityConstantsOrThrow(@NonNull Quality quality) {
        Preconditions.checkArgument(Quality.containsQuality(quality),
                "Invalid quality: " + quality);
    }
}
