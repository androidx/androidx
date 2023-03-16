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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Logger;
import androidx.camera.core.ResolutionSelector;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.internal.utils.SizeUtil;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class used to sort the supported output sizes according to the use case configs
 */
@RequiresApi(21)
public class SupportedOutputSizesSorter {
    private static final String TAG = "SupportedOutputSizesCollector";
    private final CameraInfoInternal mCameraInfoInternal;
    private final Rational mFullFovRatio;
    private final boolean mIsSensorLandscapeResolution;
    private final SupportedOutputSizesSorterLegacy mSupportedOutputSizesSorterLegacy;

    public SupportedOutputSizesSorter(@NonNull CameraInfoInternal cameraInfoInternal) {
        mCameraInfoInternal = cameraInfoInternal;
        mFullFovRatio = calculateFullFovRatio(mCameraInfoInternal);
        // Determines the sensor resolution orientation info by the full FOV ratio.
        mIsSensorLandscapeResolution = mFullFovRatio != null ? mFullFovRatio.getNumerator()
                >= mFullFovRatio.getDenominator() : true;
        mSupportedOutputSizesSorterLegacy =
                new SupportedOutputSizesSorterLegacy(cameraInfoInternal, mFullFovRatio);
    }

    /**
     * Calculates the full FOV ratio by the output sizes retrieved from CameraInfoInternal.
     *
     * <p>For most devices, the full FOV ratio should match the aspect ratio of the max supported
     * output sizes. The active pixel array info is not used because it may cause robolectric
     * test to fail if it is not set in the test environment.
     */
    @Nullable
    private Rational calculateFullFovRatio(@NonNull CameraInfoInternal cameraInfoInternal) {
        List<Size> jpegOutputSizes = cameraInfoInternal.getSupportedResolutions(ImageFormat.JPEG);
        if (jpegOutputSizes.isEmpty()) {
            return null;
        }
        Size maxSize = Collections.max(jpegOutputSizes, new CompareSizesByArea());
        return new Rational(maxSize.getWidth(), maxSize.getHeight());
    }

    @NonNull
    public List<Size> getSortedSupportedOutputSizes(@NonNull UseCaseConfig<?> useCaseConfig) {
        ImageOutputConfig imageOutputConfig = (ImageOutputConfig) useCaseConfig;
        List<Size> customOrderedResolutions = imageOutputConfig.getCustomOrderedResolutions(null);

        // Directly returns the custom ordered resolutions list if it is set.
        if (customOrderedResolutions != null) {
            return customOrderedResolutions;
        }

        // Retrieves the resolution candidate list according to the use case config if
        List<Size> resolutionCandidateList = getResolutionCandidateList(useCaseConfig);

        ResolutionSelector resolutionSelector = imageOutputConfig.getResolutionSelector(null);

        if (resolutionSelector == null) {
            return mSupportedOutputSizesSorterLegacy.sortSupportedOutputSizes(
                    resolutionCandidateList, useCaseConfig);
        } else {
            Size miniBoundingSize = resolutionSelector.getPreferredResolution();
            if (miniBoundingSize == null) {
                miniBoundingSize = imageOutputConfig.getDefaultResolution(null);
            }
            return sortSupportedOutputSizesByResolutionSelector(resolutionCandidateList,
                    resolutionSelector, miniBoundingSize);
        }
    }

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

        // Appends high resolution output sizes if high resolution is enabled by ResolutionSelector
        if (imageOutputConfig.getResolutionSelector(null) != null
                && imageOutputConfig.getResolutionSelector().isHighResolutionEnabled()) {
            List<Size> allSizesList = new ArrayList<>();
            allSizesList.addAll(resolutionCandidateList);
            allSizesList.addAll(mCameraInfoInternal.getSupportedHighResolutions(imageFormat));
            return allSizesList;
        }

        return resolutionCandidateList;
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
     * Sorts the resolution candidate list by the following steps:
     *
     * 1. Filters out the candidate list according to the max resolution.
     * 2. Sorts the candidate list according to ResolutionSelector strategies.
     */
    @NonNull
    private List<Size> sortSupportedOutputSizesByResolutionSelector(
            @NonNull List<Size> resolutionCandidateList,
            @NonNull ResolutionSelector resolutionSelector,
            @Nullable Size miniBoundingSize) {
        if (resolutionCandidateList.isEmpty()) {
            return resolutionCandidateList;
        }

        List<Size> descendingSizeList = new ArrayList<>(resolutionCandidateList);

        // Sort the result sizes. The Comparator result must be reversed to have a descending
        // order result.
        Collections.sort(descendingSizeList, new CompareSizesByArea(true));

        // 1. Filters out the candidate list according to the min size bound and max resolution.
        List<Size> filteredSizeList = filterOutResolutionCandidateListByMaxResolutionSetting(
                descendingSizeList, resolutionSelector);

        // 2. Sorts the candidate list according to the rules of new Resolution API.
        return sortResolutionCandidateListByTargetAspectRatioAndResolutionSettings(
                filteredSizeList, resolutionSelector, miniBoundingSize);

    }

    /**
     * Filters out the resolution candidate list by the max resolution setting.
     *
     * The input size list should have been sorted in descending order.
     */
    private static List<Size> filterOutResolutionCandidateListByMaxResolutionSetting(
            @NonNull List<Size> resolutionCandidateList,
            @NonNull ResolutionSelector resolutionSelector) {
        // Retrieves the max resolution setting. When ResolutionSelector is used, all resolution
        // selection logic should depend on ResolutionSelector's settings.
        Size maxResolution = resolutionSelector.getMaxResolution();

        if (maxResolution == null) {
            return resolutionCandidateList;
        }

        // Filter out the resolution candidate list by the max resolution. Sizes that any edge
        // exceeds the max resolution will be filtered out.
        List<Size> resultList = new ArrayList<>();
        for (Size outputSize : resolutionCandidateList) {
            if (!SizeUtil.isLongerInAnyEdge(outputSize, maxResolution)) {
                resultList.add(outputSize);
            }
        }

        if (resultList.isEmpty()) {
            throw new IllegalArgumentException(
                    "Resolution candidate list is empty after filtering out by the settings!");
        }

        return resultList;
    }

    /**
     * Sorts the resolution candidate list according to the new ResolutionSelector API logic.
     *
     * The list will be sorted by the following order:
     * 1. size of preferred resolution
     * 2. a resolution with preferred aspect ratio, is not smaller than, and is closest to the
     * preferred resolution.
     * 3. resolutions with preferred aspect ratio and is smaller than the preferred resolution
     * size in descending order of resolution area size.
     * 4. Other sizes sorted by CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace and
     * area size.
     */
    @NonNull
    private List<Size> sortResolutionCandidateListByTargetAspectRatioAndResolutionSettings(
            @NonNull List<Size> resolutionCandidateList,
            @NonNull ResolutionSelector resolutionSelector,
            @Nullable Size miniBoundingSize) {
        Rational aspectRatio = getTargetAspectRatioRationalValue(
                resolutionSelector.getPreferredAspectRatio(), mIsSensorLandscapeResolution);
        Preconditions.checkNotNull(aspectRatio, "ResolutionSelector should also have aspect ratio"
                + " value.");

        Size targetSize = resolutionSelector.getPreferredResolution();
        List<Size> resultList = sortResolutionCandidateListByTargetAspectRatioAndSize(
                resolutionCandidateList, aspectRatio, miniBoundingSize);

        // Moves the target size to the first position if it exists in the resolution candidate
        // list.
        if (resultList.contains(targetSize)) {
            resultList.remove(targetSize);
            resultList.add(0, targetSize);
        }

        return resultList;
    }

    /**
     * Sorts the resolution candidate list according to the target aspect ratio and size settings.
     *
     * 1. The resolution candidate list will be grouped by aspect ratio.
     * 2. Moves the smallest size larger than the mini bounding size to the first position for each
     * aspect ratio sizes group.
     * 3. The aspect ratios of groups will be sorted against to the target aspect ratio setting by
     * CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace.
     * 4. Concatenate all sizes as the result list
     */
    @NonNull
    private List<Size> sortResolutionCandidateListByTargetAspectRatioAndSize(
            @NonNull List<Size> resolutionCandidateList, @NonNull Rational aspectRatio,
            @Nullable Size miniBoundingSize) {
        // Rearrange the supported size to put the ones with the same aspect ratio in the front
        // of the list and put others in the end from large to small. Some low end devices may
        // not able to get an supported resolution that match the preferred aspect ratio.

        // Group output sizes by aspect ratio.
        Map<Rational, List<Size>> aspectRatioSizeListMap =
                groupSizesByAspectRatio(resolutionCandidateList);

        // If the target resolution is set, use it to remove unnecessary larger sizes.
        if (miniBoundingSize != null) {
            // Sorts sizes from each aspect ratio size list
            for (Rational key : aspectRatioSizeListMap.keySet()) {
                List<Size> sortedResult = sortSupportedSizesByMiniBoundingSize(
                        aspectRatioSizeListMap.get(key), miniBoundingSize);
                aspectRatioSizeListMap.put(key, sortedResult);
            }
        }

        // Sort the aspect ratio key set by the target aspect ratio.
        List<Rational> aspectRatios = new ArrayList<>(aspectRatioSizeListMap.keySet());
        Collections.sort(aspectRatios,
                new AspectRatioUtil.CompareAspectRatiosByMappingAreaInFullFovAspectRatioSpace(
                        aspectRatio, mFullFovRatio));

        List<Size> resultList = new ArrayList<>();

        // Put available sizes into final result list by aspect ratio distance to target ratio.
        for (Rational rational : aspectRatios) {
            for (Size size : aspectRatioSizeListMap.get(rational)) {
                // A size may exist in multiple groups in mod16 condition. Keep only one in
                // the final list.
                if (!resultList.contains(size)) {
                    resultList.add(size);
                }
            }
        }

        return resultList;
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
     * Removes unnecessary sizes by target size.
     *
     * <p>If the target resolution is set, a size that is equal to or closest to the target
     * resolution will be selected. If the list includes more than one size equal to or larger
     * than the target resolution, only one closest size needs to be kept. The other larger sizes
     * can be removed so that they won't be selected to use.
     *
     * @param supportedSizesList The list should have been sorted in descending order.
     * @param miniBoundingSize   The target size used to remove unnecessary sizes.
     */
    static List<Size> sortSupportedSizesByMiniBoundingSize(@NonNull List<Size> supportedSizesList,
            @NonNull Size miniBoundingSize) {
        if (supportedSizesList.isEmpty()) {
            return supportedSizesList;
        }

        List<Size> resultList = new ArrayList<>();

        // Get the index of the item that is equal to or closest to the target size.
        for (int i = 0; i < supportedSizesList.size(); i++) {
            Size outputSize = supportedSizesList.get(i);
            if (outputSize.getWidth() >= miniBoundingSize.getWidth()
                    && outputSize.getHeight() >= miniBoundingSize.getHeight()) {
                // The supportedSizesList is in descending order. Checking and put the
                // mini-bounding-above size at position 0 so that the smallest larger resolution
                // will be put in the first position finally.
                resultList.add(0, outputSize);
            } else {
                // Appends the remaining smaller sizes in descending order.
                resultList.add(outputSize);
            }
        }

        return resultList;
    }

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
