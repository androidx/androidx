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

import android.media.CamcorderProfile;
import android.util.Size;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CamcorderProfileProxy;
import androidx.core.util.Preconditions;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * QualitySelector defines the desired quality setting.
 *
 * <p>There are pre-defined quality constants that are universally used for video, such as
 * {@link #QUALITY_SD}, {@link #QUALITY_HD}, {@link #QUALITY_FHD} and {@link #QUALITY_UHD}, but
 * not all of them are supported on every device since each device has its own capabilities.
 * {@link #isQualitySupported(CameraInfo, int)} can be used to check whether a quality is
 * supported on the device or not and {@link #getResolution(CameraInfo, int)} can be used to get
 * the actual resolution defined in the device. Aside from checking the qualities one by one,
 * QualitySelector provides a more convenient way to select a quality. The typical usage of
 * selecting a single desired quality is:
 * <pre>
 *     <code>
 * QualitySelector qualitySelector = QualitySelector.of(QualitySelector.QUALITY_FHD)
 *     </code>
 * </pre>
 * Or the usage of selecting a series of qualities by desired order:
 * <pre>
 *     <code>
 * QualitySelector qualitySelector = QualitySelector
 *         .firstTry(QualitySelector.QUALITY_FHD)
 *         .thenTry(QualitySelector.QUALITY_HD)
 *         .finallyTry(QualitySelector.QUALITY_SHD)
 *     </code>
 * </pre>
 * The recommended way to set the {@link Procedure#finallyTry(int)} is giving guaranteed supported
 * qualities such as {@link #QUALITY_LOWEST} and {@link #QUALITY_HIGHEST}, which ensures the
 * QualitySelector can always choose a supported quality. Another way to ensure a quality will be
 * selected when none of the desired qualities are supported is to use
 * {@link Procedure#finallyTry(int, int)} with an open-ended fallback strategy such as
 * {@link #FALLBACK_STRATEGY_LOWER}:
 * <pre>
 *     <code>
 * QualitySelector qualitySelector = QualitySelector
 *         .firstTry(QualitySelector.QUALITY_UHD)
 *         .finallyTry(QualitySelector.QUALITY_FHD, FALLBACK_STRATEGY_LOWER)
 *     </code>
 * </pre>
 * If QUALITY_UHD and QUALITY_FHD are not supported on the device, QualitySelector will select
 * the quality that is closest to and lower than QUALITY_FHD. If no lower quality is supported,
 * the quality that is closest to and higher than QUALITY_FHD will be selected.
 *
 * @hide
 */
@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
public class QualitySelector {
    private static final String TAG = "QualitySelector";

    /**
     * A non-applicable quality.
     *
     * <p>Check QUALITY_NONE via {@link #isQualitySupported(CameraInfo, int)} will return
     * {@code false}.
     */
    public static final int QUALITY_NONE = -1;
    /**
     * The lowest video quality supported by the video frame producer.
     */
    public static final int QUALITY_LOWEST = CamcorderProfile.QUALITY_LOW;
    /**
     * The highest video quality supported by the video frame producer.
     */
    public static final int QUALITY_HIGHEST = CamcorderProfile.QUALITY_HIGH;
    /**
     * Standard Definition (SD) 480p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 720 x 480 (480p) pixels.
     */
    public static final int QUALITY_SD = CamcorderProfile.QUALITY_480P;
    /**
     * High Definition (HD) video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1280 x 720 (720p) pixels.
     */
    public static final int QUALITY_HD = CamcorderProfile.QUALITY_720P;
    /**
     * Full High Definition (FHD) 1080p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 1920 x 1080 (1080p) pixels.
     */
    public static final int QUALITY_FHD = CamcorderProfile.QUALITY_1080P;
    /**
     * Ultra High Definition (UHD) 2160p video quality.
     *
     * <p>This video quality usually corresponds to a resolution of 3840 x 2160 (2160p) pixels.
     */
    public static final int QUALITY_UHD = CamcorderProfile.QUALITY_2160P;

    /** @hide */
    @IntDef({QUALITY_NONE, QUALITY_LOWEST, QUALITY_HIGHEST, QUALITY_SD, QUALITY_HD, QUALITY_FHD,
            QUALITY_UHD})
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(Scope.LIBRARY)
    public @interface VideoQuality {
    }

    /** All quality constants. */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final List<Integer> QUALITIES = Arrays.asList(QUALITY_NONE, QUALITY_LOWEST,
            QUALITY_HIGHEST, QUALITY_SD, QUALITY_HD, QUALITY_FHD, QUALITY_UHD);

    /** Quality constants with size from large to small. */
    private static final List<Integer> QUALITIES_ORDER_BY_SIZE = Arrays.asList(QUALITY_UHD,
            QUALITY_FHD, QUALITY_HD, QUALITY_SD);

    /**
     * The strategy that no fallback strategy will be applied.
     */
    public static final int FALLBACK_STRATEGY_NONE = 0;

    /**
     * Choose the quality that is closest to and higher than the desired quality. If that can not
     * result in a supported quality, choose the quality that is closest to and lower than the
     * desired quality.
     */
    public static final int FALLBACK_STRATEGY_HIGHER = 1;

    /**
     * Choose the quality that is closest to and higher than the desired quality.
     */
    public static final int FALLBACK_STRATEGY_STRICTLY_HIGHER = 2;

    /**
     * Choose the quality that is closest to and lower than the desired quality. If that can not
     * result in a supported quality, choose the quality that is closest to and higher than the
     * desired quality.
     */
    public static final int FALLBACK_STRATEGY_LOWER = 3;

    /**
     * Choose the quality that is closest to and lower than the desired quality.
     */
    public static final int FALLBACK_STRATEGY_STRICTLY_LOWER = 4;

    private static final int FALLBACK_STRATEGY_START = FALLBACK_STRATEGY_NONE;
    private static final int FALLBACK_STRATEGY_END = FALLBACK_STRATEGY_STRICTLY_LOWER;

    /**
     * The fallback strategies when desired quality is not supported.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FALLBACK_STRATEGY_NONE,
            FALLBACK_STRATEGY_HIGHER,
            FALLBACK_STRATEGY_STRICTLY_HIGHER,
            FALLBACK_STRATEGY_LOWER,
            FALLBACK_STRATEGY_STRICTLY_LOWER
    })
    public @interface FallbackStrategy {
    }

    /**
     * Checks if the input quality is one of video quality constants.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static boolean containsQuality(int quality) {
        return QUALITIES.contains(quality);
    }

    /**
     * Gets all video quality constants with clearly defined size sorted from largest to smallest.
     *
     * <p>{@link #QUALITY_NONE}, {@link #QUALITY_HIGHEST} and {@link #QUALITY_LOWEST} are not
     * included.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public static List<Integer> getSortedQualities() {
        return new ArrayList<>(QUALITIES_ORDER_BY_SIZE);
    }

    /**
     * Gets all supported qualities on the device.
     *
     * <p>The returned list is sorted by quality size from largest to smallest. For the qualities in
     * the returned list, with the same input cameraInfo,
     * {@link #isQualitySupported(CameraInfo, int)} will return {@code true} and
     * {@link #getResolution(CameraInfo, int)} will return the corresponding resolution.
     *
     * <p>Note: Constants {@link #QUALITY_HIGHEST} and {@link #QUALITY_LOWEST} are not included
     * in the returned list, but their corresponding qualities are included.
     *
     * @param cameraInfo the cameraInfo
     */
    @NonNull
    public static List<Integer> getSupportedQualities(@NonNull CameraInfo cameraInfo) {
        return VideoCapabilities.from(cameraInfo).getSupportedQualities();
    }

    /**
     * Checks if the quality is supported.
     *
     * <p>Calling this method with one of the qualities contained in the returned list of
     * {@link #getSupportedQualities} will return {@code true}.
     *
     * <p>Possible values for {@code quality} include {@link #QUALITY_LOWEST},
     * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD}, {@link #QUALITY_FHD},
     * {@link #QUALITY_UHD} and {@link #QUALITY_NONE}.
     *
     * <p>If this method is called with {@link #QUALITY_LOWEST} or {@link #QUALITY_HIGHEST}, it
     * will return {@code true} except the case that none of the qualities can be supported.
     *
     * <p>If this method is called with {@link #QUALITY_NONE}, it will always return {@code false}.
     *
     * @param cameraInfo the cameraInfo for checking the quality.
     * @param quality one of the quality constants.
     * @return {@code true} if the quality is supported; {@code false} otherwise.
     * @see #getSupportedQualities(CameraInfo)
     */
    public static boolean isQualitySupported(@NonNull CameraInfo cameraInfo,
            @VideoQuality int quality) {
        return VideoCapabilities.from(cameraInfo).isQualitySupported(quality);
    }

    /**
     * Gets the corresponding resolution from the input quality.
     *
     * <p>Possible values for {@code quality} include {@link #QUALITY_LOWEST},
     * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD}, {@link #QUALITY_FHD},
     * {@link #QUALITY_UHD} and {@link #QUALITY_NONE}.
     *
     * <p>If this method is called with {@link #QUALITY_NONE}, it will always return {@code null}.
     *
     * @param cameraInfo the cameraInfo for checking the quality.
     * @param quality one of the quality constants.
     * @return the corresponding resolution from the input quality, or {@code null} if the
     * quality is not supported on the device. {@link #isQualitySupported(CameraInfo, int)} can
     * be used to check if the input quality is supported.
     * @throws IllegalArgumentException if not a quality constant
     * @see #isQualitySupported
     */
    @Nullable
    public static Size getResolution(@NonNull CameraInfo cameraInfo, @VideoQuality int quality) {
        checkQualityConstantsOrThrow(quality);
        CamcorderProfileProxy profile = VideoCapabilities.from(cameraInfo).getProfile(quality);
        return profile != null ? new Size(profile.getVideoFrameWidth(),
                profile.getVideoFrameHeight()) : null;
    }

    private final List<Integer> mPreferredQualityList;
    @VideoQuality
    private final int mFallbackQuality;
    @FallbackStrategy
    private final int mFallbackStrategy;

    QualitySelector(@NonNull List<Integer> preferredQualityList,
            @VideoQuality int fallbackQuality,
            @FallbackStrategy int fallbackStrategy) {
        Preconditions.checkArgument(preferredQualityList.size() > 0, "No preferred quality.");
        mPreferredQualityList = Collections.unmodifiableList(preferredQualityList);
        mFallbackQuality = fallbackQuality;
        mFallbackStrategy = fallbackStrategy;
    }

    /**
     * Sets the desired quality with the highest priority.
     *
     * <p>This method initiates a procedure for specifying the requirements of selecting
     * qualities. Other requirements can be further added with {@link Procedure} methods.
     *
     * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
     * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD}, {@link #QUALITY_FHD},
     * or {@link #QUALITY_UHD}.
     * @return the {@link Procedure} for specifying quality selection requirements.
     * @throws IllegalArgumentException if the given quality is not a quality constant.
     * @see Procedure
     */
    @NonNull
    public static Procedure firstTry(@VideoQuality int quality) {
        return new Procedure(quality);
    }

    /**
     * Gets an instance of QualitySelector with only one desired quality.
     *
     * <p>The returned QualitySelector will adopt {@link #FALLBACK_STRATEGY_NONE}.
     *
     * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
     * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD}, {@link #QUALITY_FHD},
     * or {@link #QUALITY_UHD}.
     * @return the QualitySelector instance.
     * @throws IllegalArgumentException if the given quality is not a quality constant.
     */
    @NonNull
    public static QualitySelector of(@VideoQuality int quality) {
        return of(quality, FALLBACK_STRATEGY_NONE);
    }

    /**
     * Gets an instance of QualitySelector with only one desired quality.
     *
     * <p>If the desired quality is not supported, the fallback strategy will be applied on
     * this quality.
     *
     * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
     * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD}, {@link #QUALITY_FHD},
     * or {@link #QUALITY_UHD}.
     * @param fallbackStrategy the fallback strategy. Possible values include
     * {@link #FALLBACK_STRATEGY_NONE}, {@link #FALLBACK_STRATEGY_HIGHER},
     * {@link #FALLBACK_STRATEGY_STRICTLY_HIGHER}, {@link #FALLBACK_STRATEGY_LOWER} and
     * {@link #FALLBACK_STRATEGY_STRICTLY_LOWER}.
     * @return the QualitySelector instance.
     * @throws IllegalArgumentException if {@code quality} is not a quality constant or
     * {@code fallbackStrategy} is not a fallback strategy constant.
     */
    @NonNull
    public static QualitySelector of(@VideoQuality int quality,
            @FallbackStrategy int fallbackStrategy) {
        return firstTry(quality).finallyTry(quality, fallbackStrategy);
    }

    /**
     * Generates a sorted quality list that matches the desired quality settings.
     *
     * <p>The method bases on the desired qualities and the fallback strategy to find a supported
     * quality list on this device. The desired qualities can be set by a series of try methods such
     * as {@link #firstTry(int)}, {@link #of(int)}, {@link Procedure#thenTry(int)} and
     * {@link Procedure#finallyTry(int)}. The fallback strategy can be set via
     * {@link #of(int, int)} and {@link Procedure#finallyTry(int, int)}. If no fallback strategy
     * is specified, {@link #FALLBACK_STRATEGY_NONE} will be applied by default.
     *
     * <p>The search algorithm first checks which desired quality is supported according to the
     * set sequence and adds to the returned list by order. Then the fallback strategy will be
     * applied to add more valid qualities.
     *
     * @param cameraInfo the cameraInfo for checking the quality.
     * @return a sorted supported quality list according to the desired quality settings.
     * @see Procedure
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    @NonNull
    public List<Integer> getPrioritizedQualities(@NonNull CameraInfo cameraInfo) {
        VideoCapabilities videoCapabilities = VideoCapabilities.from(cameraInfo);

        List<Integer> supportedQualities = videoCapabilities.getSupportedQualities();
        if (supportedQualities.isEmpty()) {
            Logger.w(TAG, "No supported quality on the device.");
            return new ArrayList<>();
        }
        Logger.d(TAG, "supportedQualities = " + supportedQualities);

        // Use LinkedHashSet to prevent from duplicate quality and keep the adding order.
        Set<Integer> sortedQualities = new LinkedHashSet<>();
        // Add exact quality.
        for (Integer quality : mPreferredQualityList) {
            if (quality == QUALITY_HIGHEST) {
                // Highest means user want a quality as higher as possible, so the return list can
                // contain all supported resolutions from large to small.
                sortedQualities.addAll(supportedQualities);
                break;
            } else if (quality == QUALITY_LOWEST) {
                // Opposite to the highest
                List<Integer> reversedList = new ArrayList<>(supportedQualities);
                Collections.reverse(reversedList);
                sortedQualities.addAll(reversedList);
                break;
            } else {
                if (supportedQualities.contains(quality)) {
                    sortedQualities.add(quality);
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
                + ", fallbackQuality=" + mFallbackQuality
                + ", fallbackStrategy=" + mFallbackStrategy
                + "}";
    }

    private void addByFallbackStrategy(@NonNull List<Integer> supportedQualities,
            @NonNull Set<Integer> priorityQualities) {
        if (supportedQualities.isEmpty()) {
            return;
        }
        if (priorityQualities.containsAll(supportedQualities)) {
            // priorityQualities already contains all supported qualities, no need to add by
            // fallback strategy.
            return;
        }
        Logger.d(TAG, "Select quality by fallbackStrategy = " + mFallbackStrategy
                + " on fallback quality = " + mFallbackQuality);
        // No fallback strategy, return directly.
        if (mFallbackStrategy == QualitySelector.FALLBACK_STRATEGY_NONE) {
            return;
        }

        // Note that fallback quality could be an unsupported quality, so all quality constants
        // need to be loaded to find the position of fallback quality.
        // The list returned from getSortedQualities() is sorted from large to small.
        List<Integer> sizeSortedQualities = getSortedQualities();
        int fallbackQuality;
        if (mFallbackQuality == QUALITY_HIGHEST) {
            fallbackQuality = sizeSortedQualities.get(0);
        } else if (mFallbackQuality == QUALITY_LOWEST) {
            fallbackQuality = sizeSortedQualities.get(sizeSortedQualities.size() - 1);
        } else {
            fallbackQuality = mFallbackQuality;
        }

        int index = sizeSortedQualities.indexOf(fallbackQuality);
        Preconditions.checkState(index != -1); // Should not happen.

        // search larger supported quality
        List<Integer> largerQualities = new ArrayList<>();
        for (int i = index - 1; i >= 0; i--) {
            int quality = sizeSortedQualities.get(i);
            if (supportedQualities.contains(quality)) {
                largerQualities.add(quality);
            }
        }

        // search smaller supported quality
        List<Integer> smallerQualities = new ArrayList<>();
        for (int i = index + 1; i < sizeSortedQualities.size(); i++) {
            int quality = sizeSortedQualities.get(i);
            if (supportedQualities.contains(quality)) {
                smallerQualities.add(quality);
            }
        }

        Logger.d(TAG, "sizeSortedQualities = " + sizeSortedQualities
                + ", fallback quality = " + fallbackQuality
                + ", largerQualities = " + largerQualities
                + ", smallerQualities = " + smallerQualities);

        switch (mFallbackStrategy) {
            case QualitySelector.FALLBACK_STRATEGY_HIGHER:
                priorityQualities.addAll(largerQualities);
                priorityQualities.addAll(smallerQualities);
                break;
            case QualitySelector.FALLBACK_STRATEGY_STRICTLY_HIGHER:
                priorityQualities.addAll(largerQualities);
                break;
            case QualitySelector.FALLBACK_STRATEGY_LOWER:
                priorityQualities.addAll(smallerQualities);
                priorityQualities.addAll(largerQualities);
                break;
            case QualitySelector.FALLBACK_STRATEGY_STRICTLY_LOWER:
                priorityQualities.addAll(smallerQualities);
                break;
            case QualitySelector.FALLBACK_STRATEGY_NONE:
                // No-Op
                break;
            default:
                throw new AssertionError("Unhandled fallback strategy: " + mFallbackStrategy);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void checkQualityConstantsOrThrow(@QualitySelector.VideoQuality int quality) {
        Preconditions.checkArgument(QualitySelector.containsQuality(quality),
                "Unknown quality: " + quality);
    }

    /**
     * The procedure can be used to set desired qualities and fallback strategy.
     */
    public static class Procedure {
        private final List<Integer> mPreferredQualityList = new ArrayList<>();

        Procedure(int quality) {
            addQuality(quality);
        }

        /**
         * Adds a quality candidate.
         *
         * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
         * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD},
         * {@link #QUALITY_FHD} or {@link #QUALITY_UHD}.
         * @return the procedure that can continue to be set
         * @throws IllegalArgumentException if the given quality is not a quality constant
         */
        @NonNull
        public Procedure thenTry(@VideoQuality int quality) {
            addQuality(quality);
            return this;
        }

        /**
         * Sets the final desired quality.
         *
         * <p>The returned QualitySelector will adopt {@link #FALLBACK_STRATEGY_NONE}.
         *
         * <p>This method finishes the setting procedure and generates a {@link QualitySelector}
         * with the requirements set to the procedure.
         *
         * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
         * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD},
         * {@link #QUALITY_FHD} or {@link #QUALITY_UHD}.
         * @return the {@link QualitySelector}.
         * @throws IllegalArgumentException if the given quality is not a quality constant
         */
        @NonNull
        public QualitySelector finallyTry(@VideoQuality int quality) {
            return finallyTry(quality, FALLBACK_STRATEGY_NONE);
        }

        /**
         * Sets the final desired quality and fallback strategy.
         *
         * <p>The fallback strategy will be applied on this quality when all desired qualities are
         * not supported.
         *
         * <p>This method finishes the setting procedure and generates a {@link QualitySelector}
         * with the requirements set to the procedure.
         *
         * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
         * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD},
         * {@link #QUALITY_FHD} or {@link #QUALITY_UHD}.
         * @param fallbackStrategy the fallback strategy. Possible values include
         * {@link #FALLBACK_STRATEGY_NONE}, {@link #FALLBACK_STRATEGY_HIGHER},
         * {@link #FALLBACK_STRATEGY_STRICTLY_HIGHER}, {@link #FALLBACK_STRATEGY_LOWER} and
         * {@link #FALLBACK_STRATEGY_STRICTLY_LOWER}.
         * @return the {@link QualitySelector}.
         * @throws IllegalArgumentException if {@code quality} is not a quality constant or
         * {@code fallbackStrategy} is not a fallback strategy constant.
         */
        @NonNull
        public QualitySelector finallyTry(@VideoQuality int quality,
                @FallbackStrategy int fallbackStrategy) {
            Preconditions.checkArgument(fallbackStrategy >= FALLBACK_STRATEGY_START
                            && fallbackStrategy <= FALLBACK_STRATEGY_END,
                    "The value must be a fallback strategy constant.");
            addQuality(quality);
            return new QualitySelector(new ArrayList<>(mPreferredQualityList), quality,
                    fallbackStrategy);
        }

        private void addQuality(@VideoQuality int quality) {
            checkQualityConstantsOrThrow(quality);
            Preconditions.checkArgument(quality != QUALITY_NONE, "Unsupported quality: " + quality);
            mPreferredQualityList.add(quality);
        }
    }
}
