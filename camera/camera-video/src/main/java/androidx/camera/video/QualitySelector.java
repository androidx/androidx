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
import java.util.List;

/**
 * QualitySelector defines the desired quality setting.
 *
 * <p>There are several defined quality constants such as {@link #QUALITY_SD},
 * {@link #QUALITY_HD}, {@link #QUALITY_FHD} and {@link #QUALITY_FHD}, but not all of them
 * are supported on every device since each device has its own capabilities.
 * {@link #isQualitySupported(CameraInfo, int)} can be used to check whether a quality is
 * supported on the device or not and {@link #getResolution(CameraInfo, int)} can be used to get
 * the actual resolution defined in the device. However, checking qualities one by one is not
 * so inconvenient for the quality setting. QualitySelector is designed to facilitate the quality
 * setting. The typical usage is
 * <pre>
 *     <code>
 * QualitySelector qualitySelector = QualitySelector.of(QualitySelector.QUALITY_FHD)
 *     </code>
 * </pre>
 * if there is only one desired quality, or a series of quality constants can be set by desired
 * order
 * <pre>
 *     <code>
 * QualitySelector qualitySelector = QualitySelector
 *         .firstTry(QualitySelector.QUALITY_FHD)
 *         .thenTry(QualitySelector.QUALITY_HD)
 *         .finallyTry(QualitySelector.QUALITY_SHD)
 *     </code>
 * </pre>
 * A recommended way to set the {@link Procedure#finallyTry(int)} is giving guaranteed supported
 * qualities such as {@link #QUALITY_LOWEST} and {@link #QUALITY_HIGHEST}, which ensures the
 * QualitySelector can always choose a supported quality. Another way to ensure a quality is
 * selected when none of the desired qualities are supported is to use
 * {@link Procedure#finallyTry(int, int)} with an open-ended fallback strategy such as
 * {@link #FALLBACK_STRATEGY_LOWER}.
 * <pre>
 *     <code>
 * QualitySelector qualitySelector = QualitySelector
 *         .firstTry(QualitySelector.QUALITY_UHD)
 *         .finallyTry(QualitySelector.QUALITY_FHD, FALLBACK_STRATEGY_LOWER)
 *     </code>
 * </pre>
 * If QUALITY_UHD and QUALITY_FHD are not supported on the device, the next lower supported
 * quality than QUALITY_FHD will be attempted. If no lower quality is supported, the next higher
 * supported quality will be selected. {@link #select(CameraInfo)} can obtain the final result
 * quality based on the desired qualities and fallback strategy, {@link #QUALITY_NONE} will be
 * returned if all desired qualities are not supported and fallback strategy also cannot find a
 * supported one.
 */
public class QualitySelector {
    private static final String TAG = "QualitySelector";

    /**
     * Indicates no quality.
     *
     * <p>Check QUALITY_NONE via {@link #isQualitySupported(CameraInfo, int)} will return
     * {@code false}. {@link #select(CameraInfo)} will return QUALITY_NONE if all desired
     * qualities are not supported and fallback strategy is not able to find a supported one.
     */
    public static final int QUALITY_NONE = -1;
    /**
     * Choose the lowest video quality supported by the video frame producer.
     */
    public static final int QUALITY_LOWEST = CamcorderProfile.QUALITY_LOW;
    /**
     * Choose the highest video quality supported by the video frame producer.
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
     * No fallback strategy.
     *
     * <p>When using this fallback strategy, if {@link #select(CameraInfo)} fails to find a
     * supported quality, it will return {@link #QUALITY_NONE}.
     */
    public static final int FALLBACK_STRATEGY_NONE = 0;

    /**
     * Choose a higher quality if the desired quality isn't supported. Choose a lower quality if
     * no higher quality is supported.
     */
    public static final int FALLBACK_STRATEGY_HIGHER = 1;

    /**
     * Choose a higher quality if the desired quality isn't supported.
     *
     * <p>When a higher quality can't be found, {@link #select(CameraInfo)} will return
     * {@link #QUALITY_NONE}.
     */
    public static final int FALLBACK_STRATEGY_STRICTLY_HIGHER = 2;

    /**
     * Choose a lower quality if the desired quality isn't supported. Choose a higher quality if
     * no lower quality is supported.
     */
    public static final int FALLBACK_STRATEGY_LOWER = 3;

    /**
     * Choose a lower quality if the desired quality isn't supported.
     *
     * <p>When a lower quality can't be found, {@link #select(CameraInfo)} will return
     * {@link #QUALITY_NONE}.
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
     * Check if the input quality is one of video quality constants.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY)
    public static boolean containsQuality(int quality) {
        return QUALITIES.contains(quality);
    }

    /**
     * Get all video quality constants with clearly defined size sorted from large to small.
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
     * <p>The returned list is sorted by quality size from large to small. For the qualities in
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
     * <p>For the qualities in the list of {@link #getSupportedQualities}, calling this method with
     * these qualities will return {@code true}.
     *
     * @param cameraInfo the cameraInfo
     * @param quality one of the quality constants. Possible values include
     * {@link #QUALITY_LOWEST}, {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD},
     * {@link #QUALITY_FHD}, or {@link #QUALITY_UHD}.
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
     * @param cameraInfo the cameraInfo
     * @param quality one of the quality constants. Possible values include
     * {@link QualitySelector#QUALITY_LOWEST}, {@link QualitySelector#QUALITY_HIGHEST},
     * {@link QualitySelector#QUALITY_SD}, {@link QualitySelector#QUALITY_HD},
     * {@link QualitySelector#QUALITY_FHD}, or {@link QualitySelector#QUALITY_UHD}.
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
     * Sets the first desired quality.
     *
     * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
     * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD}, {@link #QUALITY_FHD},
     * or {@link #QUALITY_UHD}.
     * @return the procedure that can continue to be set
     * @throws IllegalArgumentException if not a quality constant.
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
     * @throws IllegalArgumentException if not a quality constant.
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
     * Find a quality match to the desired quality settings.
     *
     * <p>The method bases on the desired qualities and the fallback strategy to find out a
     * supported quality on this device. The desired qualities can be set by a series of try
     * methods such as {@link #firstTry(int)}, {@link #of(int)},
     * {@link Procedure#thenTry(int)} and {@link Procedure#finallyTry(int)}. The fallback strategy
     * can be set via {@link #of(int, int)} and {@link Procedure#finallyTry(int, int)}. If no
     * fallback strategy is specified, {@link #FALLBACK_STRATEGY_NONE} will be applied by default.
     *
     * <p>The search algorithm first checks which desired quality is supported according to the
     * set sequence. If no desired quality is supported, the fallback strategy will be applied to
     * the quality set with it. If there is still no quality can be found, {@link #QUALITY_NONE}
     * will be returned.
     *
     * @param cameraInfo the cameraInfo
     * @return the first supported quality of the desired qualities, or a supported quality
     * searched by fallback strategy, or {@link #QUALITY_NONE} when no quality is found.
     */
    @VideoQuality
    public int select(@NonNull CameraInfo cameraInfo) {
        VideoCapabilities videoCapabilities = VideoCapabilities.from(cameraInfo);

        List<Integer> supportedQualityList = videoCapabilities.getSupportedQualities();
        if (supportedQualityList.isEmpty()) {
            Logger.w(TAG, "No supported quality on the device.");
            return QUALITY_NONE;
        }

        // Find exact quality.
        for (Integer quality : mPreferredQualityList) {
            if (videoCapabilities.isQualitySupported(quality)) {
                Logger.d(TAG, "Quality is selected by exact quality = " + quality);
                return quality;
            }
        }

        // Find quality by fallback strategy based on fallback quality.
        return selectByFallbackStrategy(videoCapabilities);
    }

    @VideoQuality
    private int selectByFallbackStrategy(VideoCapabilities videoCapabilities) {
        Logger.d(TAG, "Select quality by fallbackStrategy = " + mFallbackStrategy
                + " on fallback quality = " + mFallbackQuality);
        // If fallback quality is already supported, return directly.
        if (videoCapabilities.isQualitySupported(mFallbackQuality)) {
            return mFallbackQuality;
        }

        // No fallback strategy, return directly.
        if (mFallbackStrategy == QualitySelector.FALLBACK_STRATEGY_NONE) {
            return QUALITY_NONE;
        }

        // Size is from large to small
        List<Integer> sizeSortedQualities = getSortedQualities();
        int index = sizeSortedQualities.indexOf(mFallbackQuality);
        Preconditions.checkState(index != -1); // Should not happen.

        // search larger supported quality
        int largerQuality = QUALITY_NONE;
        for (int i = index - 1; i > 0; i--) {
            int quality = sizeSortedQualities.get(i);
            if (videoCapabilities.getProfile(quality) != null) {
                largerQuality = quality;
                break;
            }
        }

        // search smaller supported quality
        int smallerQuality = QUALITY_NONE;
        for (int i = index + 1; index < sizeSortedQualities.size() - 1; i++) {
            int quality = sizeSortedQualities.get(i);
            if (videoCapabilities.getProfile(quality) != null) {
                smallerQuality = quality;
                break;
            }
        }

        Logger.d(TAG, "sizeSortedQualities = " + sizeSortedQualities
                + ", fallback quality = " + mFallbackQuality
                + ", largerQuality = " + largerQuality
                + ", smallerQuality = " + smallerQuality);

        switch (mFallbackStrategy) {
            case QualitySelector.FALLBACK_STRATEGY_HIGHER:
                if (largerQuality != QUALITY_NONE) {
                    return largerQuality;
                } else if (smallerQuality != QUALITY_NONE) {
                    return smallerQuality;
                }
                break;
            case QualitySelector.FALLBACK_STRATEGY_STRICTLY_HIGHER:
                if (largerQuality != QUALITY_NONE) {
                    return largerQuality;
                }
                break;
            case QualitySelector.FALLBACK_STRATEGY_LOWER:
                if (smallerQuality != QUALITY_NONE) {
                    return smallerQuality;
                } else if (largerQuality != QUALITY_NONE) {
                    return largerQuality;
                }
                break;
            case QualitySelector.FALLBACK_STRATEGY_STRICTLY_LOWER:
                if (smallerQuality != QUALITY_NONE) {
                    return smallerQuality;
                }
                break;
            case QualitySelector.FALLBACK_STRATEGY_NONE:
                // No-Op
                break;
            default:
                throw new AssertionError("Unhandled fallback strategy: " + mFallbackStrategy);
        }
        return QUALITY_NONE;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void checkQualityConstantsOrThrow(@QualitySelector.VideoQuality int quality) {
        Preconditions.checkArgument(QualitySelector.containsQuality(quality),
                "Unknown quality: " + quality);
    }

    /**
     * The procedure can continue to set the desired quality and fallback strategy.
     */
    public static class Procedure {
        private final List<Integer> mPreferredQualityList = new ArrayList<>();

        Procedure(int quality) {
            addQuality(quality);
        }

        /**
         * Sets the next desired quality.
         *
         * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
         * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD},
         * {@link #QUALITY_FHD} or {@link #QUALITY_UHD}.
         * @return the procedure that can continue to be set
         * @throws IllegalArgumentException if not a quality constant
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
         * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
         * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD},
         * {@link #QUALITY_FHD} or {@link #QUALITY_UHD}.
         * @return the QualitySelector.
         * @throws IllegalArgumentException if not a quality constant
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
         * @param quality the quality constant. Possible values include {@link #QUALITY_LOWEST},
         * {@link #QUALITY_HIGHEST}, {@link #QUALITY_SD}, {@link #QUALITY_HD},
         * {@link #QUALITY_FHD} or {@link #QUALITY_UHD}.
         * @param fallbackStrategy the fallback strategy. Possible values include
         * {@link #FALLBACK_STRATEGY_NONE}, {@link #FALLBACK_STRATEGY_HIGHER},
         * {@link #FALLBACK_STRATEGY_STRICTLY_HIGHER}, {@link #FALLBACK_STRATEGY_LOWER} and
         * {@link #FALLBACK_STRATEGY_STRICTLY_LOWER}.
         * @return the QualitySelector.
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
