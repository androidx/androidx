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

package androidx.camera.core.internal;

import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_3_4;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_9_16;
import static androidx.camera.core.impl.utils.AspectRatioUtil.hasMatchingAspectRatio;

import android.graphics.ImageFormat;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.CameraOrientationUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionFilter;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The supported output sizes collector to help collect the available resolution candidate list
 * according to the use case config and the following settings in {@link ResolutionSelector}:
 *
 * <ul>
 *   <li>Aspect ratio strategy
 *   <li>Resolution strategy
 *   <li>Custom resolution filter
 *   <li>High resolution enabled flags
 * </ul>
 */
@RequiresApi(21)
class SupportedOutputSizesSorter {
    private static final String TAG = "SupportedOutputSizesCollector";
    private final CameraInfoInternal mCameraInfoInternal;
    private final int mSensorOrientation;
    private final int mLensFacing;
    private final Rational mFullFovRatio;
    private final boolean mIsSensorLandscapeResolution;
    private final SupportedOutputSizesSorterLegacy mSupportedOutputSizesSorterLegacy;

    SupportedOutputSizesSorter(@NonNull CameraInfoInternal cameraInfoInternal,
            @Nullable Size activeArraySize) {
        mCameraInfoInternal = cameraInfoInternal;
        mSensorOrientation = mCameraInfoInternal.getSensorRotationDegrees();
        mLensFacing = mCameraInfoInternal.getLensFacing();
        mFullFovRatio = activeArraySize != null ? calculateFullFovRatioFromActiveArraySize(
                activeArraySize) : calculateFullFovRatioFromSupportedOutputSizes(
                mCameraInfoInternal);
        // Determines the sensor resolution orientation info by the full FOV ratio.
        mIsSensorLandscapeResolution = mFullFovRatio != null ? mFullFovRatio.getNumerator()
                >= mFullFovRatio.getDenominator() : true;
        mSupportedOutputSizesSorterLegacy =
                new SupportedOutputSizesSorterLegacy(cameraInfoInternal, mFullFovRatio);
    }

    /**
     * Calculates the full FOV ratio by the active array size.
     */
    @NonNull
    private Rational calculateFullFovRatioFromActiveArraySize(@NonNull Size activeArraySize) {
        return new Rational(activeArraySize.getWidth(), activeArraySize.getHeight());
    }

    /**
     * Calculates the full FOV ratio by the output sizes retrieved from CameraInfoInternal.
     *
     * <p>For most devices, the full FOV ratio should match the aspect ratio of the max supported
     * output sizes. The active pixel array info is not used because it may cause robolectric
     * test to fail if it is not set in the test environment.
     */
    @Nullable
    private Rational calculateFullFovRatioFromSupportedOutputSizes(
            @NonNull CameraInfoInternal cameraInfoInternal) {
        List<Size> jpegOutputSizes = cameraInfoInternal.getSupportedResolutions(ImageFormat.JPEG);
        if (jpegOutputSizes.isEmpty()) {
            return null;
        }
        Size maxSize = Collections.max(jpegOutputSizes, new CompareSizesByArea());
        return new Rational(maxSize.getWidth(), maxSize.getHeight());
    }

    /**
     * Returns the sorted output sizes according to the use case config.
     *
     * <p>If ResolutionSelector is specified in the use case config, the output sizes will be
     * sorted according to the ResolutionSelector setting and logic. Otherwise, the output sizes
     * will be sorted according to the legacy resolution API settings and logic.
     */
    @NonNull
    List<Size> getSortedSupportedOutputSizes(@NonNull UseCaseConfig<?> useCaseConfig) {
        ImageOutputConfig imageOutputConfig = (ImageOutputConfig) useCaseConfig;
        List<Size> customOrderedResolutions = imageOutputConfig.getCustomOrderedResolutions(null);

        // Directly returns the custom ordered resolutions list if it is set.
        if (customOrderedResolutions != null) {
            return customOrderedResolutions;
        }

        ResolutionSelector resolutionSelector = imageOutputConfig.getResolutionSelector(null);

        if (resolutionSelector == null) {
            return mSupportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
                    getResolutionCandidateList(useCaseConfig), useCaseConfig);
        } else {
            return sortSupportedOutputSizesByResolutionSelector(useCaseConfig);
        }
    }

    /**
     * Retrieves the customized supported resolutions from the use case config.
     *
     * <p>In some cases, the use case might not be able to use all the supported output sizes
     * retrieved from the stream configuration map. For example, extensions is enabled. These
     * sizes can be set in the use case config by
     * {@link ImageOutputConfig.Builder#setSupportedResolutions(List)}. SupportedOutputSizesSorter
     * should use the customized supported resolutions to run the sort/filter logic if it is set.
     */
    @Nullable
    private List<Size> getCustomizedSupportedResolutionsFromConfig(int imageFormat,
            @NonNull ImageOutputConfig config) {
        Size[] outputSizes = null;

        // Try to retrieve customized supported resolutions from config.
        List<Pair<Integer, Size[]>> formatResolutionsPairList =
                config.getSupportedResolutions(null);

        if (formatResolutionsPairList != null) {
            for (Pair<Integer, Size[]> formatResolutionPair : formatResolutionsPairList) {
                if (formatResolutionPair.first == imageFormat) {
                    outputSizes = formatResolutionPair.second;
                    break;
                }
            }
        }

        return outputSizes == null ? null : Arrays.asList(outputSizes);
    }

    /**
     * Sorts the resolution candidate list according to the ResolutionSelector API logic.
     *
     * <ol>
     *   <li>Collects the output sizes
     *     <ul>
     *       <li>Applies the high resolution settings
     *     </ul>
     *   <li>Applies the aspect ratio strategy
     *     <ul>
     *       <li>Applies the aspect ratio strategy fallback rule
     *     </ul>
     *   <li>Applies the resolution strategy
     *     <ul>
     *       <li>Applies the resolution strategy fallback rule
     *     </ul>
     *   <li>Applies the resolution filter
     * </ol>
     *
     * @return a size list which has been filtered and sorted by the specified resolution
     * selector settings.
     * @throws IllegalArgumentException if the specified resolution filter returns any size which
     *                                  is not included in the provided supported size list.
     */
    @NonNull
    private List<Size> sortSupportedOutputSizesByResolutionSelector(
            @NonNull UseCaseConfig<?> useCaseConfig) {
        ResolutionSelector resolutionSelector =
                ((ImageOutputConfig) useCaseConfig).getResolutionSelector();

        // Retrieves the normal supported output sizes.
        List<Size> resolutionCandidateList = getResolutionCandidateList(useCaseConfig);

        // Applies the high resolution settings onto the resolution candidate list.
        if (!useCaseConfig.isHigResolutionDisabled(false)) {
            resolutionCandidateList = applyHighResolutionSettings(resolutionCandidateList,
                    resolutionSelector, useCaseConfig.getInputFormat());
        }

        // Applies the aspect ratio strategy onto the resolution candidate list.
        LinkedHashMap<Rational, List<Size>> aspectRatioSizeListMap =
                applyAspectRatioStrategy(resolutionCandidateList,
                        resolutionSelector.getAspectRatioStrategy());

        // Applies the resolution strategy onto the resolution candidate list.
        applyResolutionStrategy(aspectRatioSizeListMap, resolutionSelector.getResolutionStrategy());

        // Collects all sizes from the sorted aspect ratio size groups into the final sorted list.
        List<Size> resultList = new ArrayList<>();
        for (List<Size> sortedSizes : aspectRatioSizeListMap.values()) {
            for (Size size : sortedSizes) {
                // A size may exist in multiple groups in mod16 condition. Keep only one in
                // the final list.
                if (!resultList.contains(size)) {
                    resultList.add(size);
                }
            }
        }

        // Applies the resolution filter onto the resolution candidate list.
        return applyResolutionFilter(resultList, resolutionSelector.getResolutionFilter(),
                ((ImageOutputConfig) useCaseConfig).getTargetRotation(Surface.ROTATION_0));
    }

    /**
     * Returns the normal supported output sizes.
     *
     * <p>When using camera-camera2 implementation, the output sizes are retrieved via
     * StreamConfigurationMap#getOutputSizes().
     *
     * @return the resolution candidate list sorted in descending order.
     */
    @NonNull
    private List<Size> getResolutionCandidateList(@NonNull UseCaseConfig<?> useCaseConfig) {
        int imageFormat = useCaseConfig.getInputFormat();
        ImageOutputConfig imageOutputConfig = (ImageOutputConfig) useCaseConfig;
        // Tries to get the custom supported resolutions list if it is set
        List<Size> resolutionCandidateList = getCustomizedSupportedResolutionsFromConfig(
                imageFormat, imageOutputConfig);

        // Tries to get the supported output sizes from the CameraInfoInternal if both custom
        // ordered and supported resolutions lists are not set.
        if (resolutionCandidateList == null) {
            resolutionCandidateList = mCameraInfoInternal.getSupportedResolutions(imageFormat);
        }

        // CameraInfoInternal.getSupportedResolutions is not guaranteed to return a modifiable list
        // needed by Collections.sort(), so it is converted to a modifiable list here
        resolutionCandidateList = new ArrayList<>(resolutionCandidateList);

        Collections.sort(resolutionCandidateList, new CompareSizesByArea(true));

        if (resolutionCandidateList.isEmpty()) {
            Logger.w(TAG, "The retrieved supported resolutions from camera info internal is empty"
                    + ". Format is " + imageFormat + ".");
        }

        return resolutionCandidateList;
    }

    /**
     * Appends the high resolution supported output sizes according to the high resolution settings.
     *
     * <p>When using camera-camera2 implementation, the output sizes are retrieved via
     * StreamConfigurationMap#getHighResolutionOutputSizes().
     *
     * @param resolutionCandidateList the supported size list which contains only normal output
     *                                sizes.
     * @param resolutionSelector      the specified resolution selector.
     * @param imageFormat             the desired image format for the target use case.
     * @return the resolution candidate list including the high resolution output sizes sorted in
     * descending order.
     */
    @NonNull
    private List<Size> applyHighResolutionSettings(@NonNull List<Size> resolutionCandidateList,
            @NonNull ResolutionSelector resolutionSelector, int imageFormat) {
        // Appends high resolution output sizes if high resolution is enabled by ResolutionSelector
        if (resolutionSelector.getAllowedResolutionMode()
                == ResolutionSelector.ALLOWED_RESOLUTIONS_SLOW) {
            List<Size> allSizesList = new ArrayList<>();
            allSizesList.addAll(resolutionCandidateList);
            allSizesList.addAll(mCameraInfoInternal.getSupportedHighResolutions(imageFormat));
            Collections.sort(allSizesList, new CompareSizesByArea(true));
            return allSizesList;
        }

        return resolutionCandidateList;
    }

    /**
     * Applies the aspect ratio strategy onto the input resolution candidate list.
     *
     * @param resolutionCandidateList the supported sizes list which has been sorted in
     *                                descending order.
     * @param aspectRatioStrategy     the specified aspect ratio strategy.
     * @return an aspect ratio to size list linked hash map which the aspect ratio fallback rule
     * is applied and is sorted against the preferred aspect ratio.
     */
    @NonNull
    private LinkedHashMap<Rational, List<Size>> applyAspectRatioStrategy(
            @NonNull List<Size> resolutionCandidateList,
            @NonNull AspectRatioStrategy aspectRatioStrategy) {
        // Group output sizes by aspect ratio.
        Map<Rational, List<Size>> aspectRatioSizeListMap =
                groupSizesByAspectRatio(resolutionCandidateList);

        // Applies the aspect ratio fallback rule
        return applyAspectRatioStrategyFallbackRule(aspectRatioSizeListMap, aspectRatioStrategy);
    }

    /**
     * Applies the aspect ratio strategy fallback rule to the aspect ratio to size list map.
     *
     * @param sizeGroupsMap       the aspect ratio to size list map. The size list should have been
     *                            sorted in descending order.
     * @param aspectRatioStrategy the specified aspect ratio strategy.
     * @return an aspect ratio to size list linked hash map which the aspect ratio fallback rule
     * is applied and is sorted against the preferred aspect ratio.
     */
    private LinkedHashMap<Rational, List<Size>> applyAspectRatioStrategyFallbackRule(
            @NonNull Map<Rational, List<Size>> sizeGroupsMap,
            @NonNull AspectRatioStrategy aspectRatioStrategy) {
        Rational aspectRatio = getTargetAspectRatioRationalValue(
                aspectRatioStrategy.getPreferredAspectRatio(), mIsSensorLandscapeResolution);

        // Remove items of all other aspect ratios if the fallback rule is AspectRatioStrategy
        // .FALLBACK_RULE_NONE
        if (aspectRatioStrategy.getFallbackRule() == AspectRatioStrategy.FALLBACK_RULE_NONE) {
            Rational preferredAspectRatio = getTargetAspectRatioRationalValue(
                    aspectRatioStrategy.getPreferredAspectRatio(), mIsSensorLandscapeResolution);
            for (Rational ratio : new ArrayList<>(sizeGroupsMap.keySet())) {
                if (!ratio.equals(preferredAspectRatio)) {
                    sizeGroupsMap.remove(ratio);
                }
            }
        }

        // Sorts the aspect ratio key set by the preferred aspect ratio.
        List<Rational> aspectRatios = new ArrayList<>(sizeGroupsMap.keySet());
        Collections.sort(aspectRatios,
                new AspectRatioUtil.CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                        aspectRatio, mFullFovRatio));

        // Stores the size groups into LinkedHashMap to keep the order
        LinkedHashMap<Rational, List<Size>> sortedAspectRatioSizeListMap = new LinkedHashMap<>();
        for (Rational ratio : aspectRatios) {
            sortedAspectRatioSizeListMap.put(ratio, sizeGroupsMap.get(ratio));
        }

        return sortedAspectRatioSizeListMap;
    }

    /**
     * Applies the resolution strategy onto the aspect ratio to size list linked hash map.
     *
     * <p>The resolution fallback rule is applied to filter out and sort the sizes in the
     * underlying size list.
     *
     * @param sortedAspectRatioSizeListMap the aspect ratio to size list linked hash map. The
     *                                     entries order should not be changed.
     * @param resolutionStrategy           the resolution strategy to sort the candidate
     *                                     resolutions.
     */
    private static void applyResolutionStrategy(
            @NonNull LinkedHashMap<Rational, List<Size>> sortedAspectRatioSizeListMap,
            @Nullable ResolutionStrategy resolutionStrategy) {
        if (resolutionStrategy == null) {
            return;
        }

        // Applies the resolution strategy with the specified fallback rule
        for (Rational key : sortedAspectRatioSizeListMap.keySet()) {
            applyResolutionStrategyFallbackRule(sortedAspectRatioSizeListMap.get(key),
                    resolutionStrategy);
        }
    }

    /**
     * Applies the resolution strategy fallback rule to the size list.
     *
     * @param supportedSizesList the supported sizes list which has been sorted in descending order.
     * @param resolutionStrategy the resolution strategy to sort the candidate resolutions.
     */
    private static void applyResolutionStrategyFallbackRule(
            @NonNull List<Size> supportedSizesList,
            @NonNull ResolutionStrategy resolutionStrategy) {
        if (supportedSizesList.isEmpty()) {
            return;
        }
        Integer fallbackRule = resolutionStrategy.getFallbackRule();

        if (resolutionStrategy.equals(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)) {
            // Do nothing for HIGHEST_AVAILABLE_STRATEGY case.
            return;
        }

        Size boundSize = resolutionStrategy.getBoundSize();

        switch (fallbackRule) {
            case ResolutionStrategy.FALLBACK_RULE_NONE:
                sortSupportedSizesByFallbackRuleNone(supportedSizesList, boundSize);
                break;
            case ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER:
                sortSupportedSizesByFallbackRuleClosestHigherThenLower(supportedSizesList,
                        boundSize, true);
                break;
            case ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER:
                sortSupportedSizesByFallbackRuleClosestHigherThenLower(supportedSizesList,
                        boundSize, false);
                break;
            case ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER:
                sortSupportedSizesByFallbackRuleClosestLowerThenHigher(supportedSizesList,
                        boundSize, true);
                break;
            case ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER:
                sortSupportedSizesByFallbackRuleClosestLowerThenHigher(supportedSizesList,
                        boundSize, false);
                break;
            default:
                break;
        }
    }

    /**
     * Applies the resolution filtered to the sorted output size list.
     *
     * @param sizeList         the supported size list which has been filtered and sorted by the
     *                         specified aspect ratio, resolution strategies.
     * @param resolutionFilter the specified resolution filter.
     * @param targetRotation   the use case target rotation info
     * @return the result size list applied the specified resolution filter.
     * @throws IllegalArgumentException if the specified resolution filter returns any size which
     *                                  is not included in the provided supported size list.
     */
    @NonNull
    private List<Size> applyResolutionFilter(@NonNull List<Size> sizeList,
            @Nullable ResolutionFilter resolutionFilter,
            @ImageOutputConfig.RotationValue int targetRotation) {
        if (resolutionFilter == null) {
            return sizeList;
        }

        // Invokes ResolutionFilter#filter() to filter/sort and return the result if it is
        // specified.
        int destRotationDegrees = CameraOrientationUtil.surfaceRotationToDegrees(
                targetRotation);
        int rotationDegrees =
                CameraOrientationUtil.getRelativeImageRotation(destRotationDegrees,
                        mSensorOrientation,
                        mLensFacing == CameraSelector.LENS_FACING_BACK);
        List<Size> filteredResultList = resolutionFilter.filter(new ArrayList<>(sizeList),
                rotationDegrees);
        if (sizeList.containsAll(filteredResultList)) {
            return filteredResultList;
        } else {
            throw new IllegalArgumentException("The returned sizes list of the resolution "
                    + "filter must be a subset of the provided sizes list.");
        }
    }

    /**
     * Sorts the size list for {@link ResolutionStrategy#FALLBACK_RULE_NONE}.
     *
     * @param supportedSizesList the supported sizes list which has been sorted in descending order.
     * @param boundSize          the resolution strategy bound size.
     */
    private static void sortSupportedSizesByFallbackRuleNone(
            @NonNull List<Size> supportedSizesList, @NonNull Size boundSize) {
        boolean containsBoundSize = supportedSizesList.contains(boundSize);
        supportedSizesList.clear();
        if (containsBoundSize) {
            supportedSizesList.add(boundSize);
        }
    }

    /**
     * Sorts the size list for {@link ResolutionStrategy#FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER}
     * or {@link ResolutionStrategy#FALLBACK_RULE_CLOSEST_HIGHER}.
     *
     * @param supportedSizesList the supported sizes list which has been sorted in descending order.
     * @param boundSize          the resolution strategy bound size.
     * @param keepLowerSizes     keeps the sizes lower than the bound size in the result list if
     *                           this is {@code true}.
     */
    static void sortSupportedSizesByFallbackRuleClosestHigherThenLower(
            @NonNull List<Size> supportedSizesList, @NonNull Size boundSize,
            boolean keepLowerSizes) {
        List<Size> lowerSizes = new ArrayList<>();

        for (int i = supportedSizesList.size() - 1; i >= 0; i--) {
            Size outputSize = supportedSizesList.get(i);
            if (outputSize.getWidth() < boundSize.getWidth()
                    || outputSize.getHeight() < boundSize.getHeight()) {
                // The supportedSizesList is in descending order. Checking and put the
                // bounding-below size at position 0 so that the largest smaller resolution
                // will be put in the first position finally.
                lowerSizes.add(0, outputSize);
            } else {
                break;
            }
        }
        // Removes the lower sizes from the list
        supportedSizesList.removeAll(lowerSizes);
        // Reverses the list so that the smallest larger resolution will be put in the first
        // position.
        Collections.reverse(supportedSizesList);
        if (keepLowerSizes) {
            // Appends the lower sizes to the tail
            supportedSizesList.addAll(lowerSizes);
        }
    }

    /**
     * Sorts the size list for {@link ResolutionStrategy#FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER}
     * or {@link ResolutionStrategy#FALLBACK_RULE_CLOSEST_LOWER}.
     *
     * @param supportedSizesList the supported sizes list which has been sorted in descending order.
     * @param boundSize          the resolution strategy bound size.
     * @param keepHigherSizes    keeps the sizes higher than the bound size in the result list if
     *                           this is {@code true}.
     */
    private static void sortSupportedSizesByFallbackRuleClosestLowerThenHigher(
            @NonNull List<Size> supportedSizesList, @NonNull Size boundSize,
            boolean keepHigherSizes) {
        List<Size> higherSizes = new ArrayList<>();

        for (int i = 0; i < supportedSizesList.size(); i++) {
            Size outputSize = supportedSizesList.get(i);
            if (outputSize.getWidth() > boundSize.getWidth()
                    || outputSize.getHeight() > boundSize.getHeight()) {
                // The supportedSizesList is in descending order. Checking and put the
                // bounding-above size at position 0 so that the smallest larger resolution
                // will be put in the first position finally.
                higherSizes.add(0, outputSize);
            } else {
                // Breaks the for-loop to keep the equal-to or lower sizes in the list.
                break;
            }
        }
        // Removes the higher sizes from the list
        supportedSizesList.removeAll(higherSizes);
        if (keepHigherSizes) {
            // Appends the higher sizes to the tail
            supportedSizesList.addAll(higherSizes);
        }
    }

    /**
     * Returns the target aspect ratio rational value according to the ResolutionSelector settings.
     */
    @Nullable
    static Rational getTargetAspectRatioRationalValue(@AspectRatio.Ratio int aspectRatio,
            boolean isSensorLandscapeResolution) {
        Rational outputRatio = null;

        switch (aspectRatio) {
            case AspectRatio.RATIO_4_3:
                outputRatio = isSensorLandscapeResolution ? ASPECT_RATIO_4_3
                        : ASPECT_RATIO_3_4;
                break;
            case AspectRatio.RATIO_16_9:
                outputRatio = isSensorLandscapeResolution ? ASPECT_RATIO_16_9
                        : ASPECT_RATIO_9_16;
                break;
            case AspectRatio.RATIO_DEFAULT:
                break;
            default:
                Logger.e(TAG, "Undefined target aspect ratio: " + aspectRatio);
        }

        return outputRatio;
    }

    /**
     * Returns the grouping aspect ratio keys of the input resolution list.
     *
     * <p>Some sizes might be mod16 case. When grouping, those sizes will be grouped into an
     * existing aspect ratio group if the aspect ratio can match by the mod16 rule.
     */
    @NonNull
    static List<Rational> getResolutionListGroupingAspectRatioKeys(
            @NonNull List<Size> resolutionCandidateList) {
        List<Rational> aspectRatios = new ArrayList<>();

        // Adds the default 4:3 and 16:9 items first to avoid their mod16 sizes to create
        // additional items.
        aspectRatios.add(ASPECT_RATIO_4_3);
        aspectRatios.add(ASPECT_RATIO_16_9);

        // Tries to find the aspect ratio which the target size belongs to.
        for (Size size : resolutionCandidateList) {
            Rational newRatio = new Rational(size.getWidth(), size.getHeight());
            boolean aspectRatioFound = aspectRatios.contains(newRatio);

            // The checking size might be a mod16 size which can be mapped to an existing aspect
            // ratio group.
            if (!aspectRatioFound) {
                boolean hasMatchingAspectRatio = false;
                for (Rational aspectRatio : aspectRatios) {
                    if (hasMatchingAspectRatio(size, aspectRatio)) {
                        hasMatchingAspectRatio = true;
                        break;
                    }
                }
                if (!hasMatchingAspectRatio) {
                    aspectRatios.add(newRatio);
                }
            }
        }

        return aspectRatios;
    }

    /**
     * Groups the input sizes into an aspect ratio to size list map.
     */
    static Map<Rational, List<Size>> groupSizesByAspectRatio(@NonNull List<Size> sizes) {
        Map<Rational, List<Size>> aspectRatioSizeListMap = new HashMap<>();

        List<Rational> aspectRatioKeys = getResolutionListGroupingAspectRatioKeys(sizes);

        for (Rational aspectRatio : aspectRatioKeys) {
            aspectRatioSizeListMap.put(aspectRatio, new ArrayList<>());
        }

        for (Size outputSize : sizes) {
            for (Rational key : aspectRatioSizeListMap.keySet()) {
                // Put the size into all groups that is matched in mod16 condition since a size
                // may match multiple aspect ratio in mod16 algorithm.
                if (hasMatchingAspectRatio(outputSize, key)) {
                    aspectRatioSizeListMap.get(key).add(outputSize);
                }
            }
        }

        return aspectRatioSizeListMap;
    }
}
