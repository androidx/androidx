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

package androidx.camera.core.streamsharing;


import static androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_SUPPORTED_RESOLUTIONS;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_16_9;
import static androidx.camera.core.impl.utils.AspectRatioUtil.ASPECT_RATIO_4_3;
import static androidx.camera.core.impl.utils.AspectRatioUtil.hasMatchingAspectRatio;
import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;

import static java.lang.Math.sqrt;

import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.internal.SupportedOutputSizesSorter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A class for calculating parent resolutions based on the children's configs.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ResolutionsMerger {

    private static final String TAG = "ResolutionsMerger";
    // The width to height ratio that has same area when cropping into 4:3 and 16:9.
    private static final double SAME_AREA_WIDTH_HEIGHT_RATIO = sqrt(4.0 / 3.0 * 16.0 / 9.0);

    @NonNull
    private final Rational mSensorAspectRatio;
    @NonNull
    private final Rational mFallbackAspectRatio;
    @NonNull
    private final Set<UseCaseConfig<?>> mChildrenConfigs;
    @NonNull
    private final SupportedOutputSizesSorter mSizeSorter;
    @NonNull
    private final List<Size> mCameraSupportedSizes;
    @NonNull
    private final Map<UseCaseConfig<?>, List<Size>> mChildSizesCache = new HashMap<>();

    ResolutionsMerger(@NonNull CameraInternal cameraInternal,
            @NonNull Set<UseCaseConfig<?>> childrenConfigs) {
        this(rectToSize(cameraInternal.getCameraControlInternal().getSensorRect()),
                cameraInternal.getCameraInfoInternal(), childrenConfigs);
    }

    private ResolutionsMerger(@NonNull Size sensorSize, @NonNull CameraInfoInternal cameraInfo,
            @NonNull Set<UseCaseConfig<?>> childrenConfigs) {
        this(sensorSize, childrenConfigs, new SupportedOutputSizesSorter(cameraInfo, sensorSize),
                cameraInfo.getSupportedResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE));
    }

    @VisibleForTesting
    ResolutionsMerger(@NonNull Size sensorSize, @NonNull Set<UseCaseConfig<?>> childrenConfigs,
            @NonNull SupportedOutputSizesSorter supportedOutputSizesSorter,
            @NonNull List<Size> cameraSupportedResolutions) {
        mSensorAspectRatio = getSensorAspectRatio(sensorSize);
        mFallbackAspectRatio = getFallbackAspectRatio(mSensorAspectRatio);
        mChildrenConfigs = childrenConfigs;
        mSizeSorter = supportedOutputSizesSorter;
        mCameraSupportedSizes = cameraSupportedResolutions;
    }

    /**
     * Returns a list of {@link Surface} resolution sorted by priority.
     *
     * <p> This method calculates the resolution for the parent {@link StreamSharing} based on 1)
     * the supported PRIV resolutions, 2) the sensor size and 3) the children's configs.
     */
    @NonNull
    List<Size> getMergedResolutions(@NonNull MutableConfig parentConfig) {
        List<Size> candidateSizes = mCameraSupportedSizes;

        // Use parent config's supported resolutions when it is set (e.g. Extensions may have
        // its limitations on resolutions).
        List<Pair<Integer, Size[]>> parentSupportedSizesMap =
                parentConfig.retrieveOption(OPTION_SUPPORTED_RESOLUTIONS, null);
        if (parentSupportedSizesMap != null) {
            candidateSizes = getSupportedPrivResolutions(parentSupportedSizesMap);
        }

        return mergeChildrenResolutions(candidateSizes);
    }

    /**
     * Returns the preferred child size with considering parent size and child's configuration.
     *
     * <p>Returns the first size in the child's ordered size list that can be cropped from {@code
     * parentSize} without upscaling it and causing double-cropping, or {@code parentSize} if no
     * matching is found.
     *
     * <p>Notes that the input {@code childConfig} is expected to be one of the values that use to
     * construct the {@link ResolutionsMerger}, if not an IllegalArgumentException will be thrown.
     */
    @NonNull
    Size getPreferredChildSize(@NonNull Size parentSize, @NonNull UseCaseConfig<?> childConfig) {
        boolean isParentCropped = !isSensorAspectRatio(parentSize);

        List<Size> candidateChildSizes = getSortedChildSizes(childConfig);
        for (Size childSize : candidateChildSizes) {
            // Skip child sizes that need another cropping when parent is already cropped.
            if (isParentCropped) {
                boolean needAnotherCropping = !(isFallbackAspectRatio(parentSize)
                        && isFallbackAspectRatio(childSize));
                if (needAnotherCropping) {
                    continue;
                }
            }

            if (!hasUpscaling(childSize, parentSize)) {
                return childSize;
            }
        }

        return parentSize;
    }

    @NonNull
    private List<Size> mergeChildrenResolutions(@NonNull List<Size> candidateParentResolutions) {
        // The following sequence of parent resolution selection is used to prevent double-cropping
        // from happening:
        // 1. Add sensor aspect-ratio resolutions, which will not cause double-cropping when the
        // child resolution is in any aspect-ratio. This is to provide parent resolution that can
        // be accepted by children in general cases.
        // 2. Add fallback aspect-ratio resolutions, which will not cause double-cropping only when
        // the child resolution is in fallback aspect-ratio.
        List<Size> result = new ArrayList<>();

        // Add resolutions for sensor aspect-ratio.
        if (needToAddSensorResolutions()) {
            result.addAll(mergeChildrenResolutionsByAspectRatio(mSensorAspectRatio,
                    candidateParentResolutions));
        }

        // Add resolutions for fallback aspect-ratio if needed.
        if (needToAddFallbackResolutions()) {
            result.addAll(mergeChildrenResolutionsByAspectRatio(mFallbackAspectRatio,
                    candidateParentResolutions));
        }

        // TODO(b/315098647): When the resulting parent resolution list is empty, consider adding
        //  resolutions that are neither 4:3 nor 16:9, but have a high overlap area (e.g. 80%)
        //  compared to the sensor size, which do not cause severe reduction of FOV, to prevent
        //  binding failures in some edge cases.

        Logger.d(TAG, "Parent resolutions: " + result);

        return result;
    }

    private List<Size> mergeChildrenResolutionsByAspectRatio(@NonNull Rational aspectRatio,
            @NonNull List<Size> candidateParentResolutions) {
        List<Size> candidates = filterResolutionsByAspectRatio(aspectRatio,
                candidateParentResolutions);
        sortInDescendingOrder(candidates);

        // Filter resolutions that are too small and track resolutions that might be too large.
        Set<Size> sizesTooLarge = new HashSet<>(candidates);
        for (UseCaseConfig<?> childConfig : mChildrenConfigs) {
            List<Size> childSizes = getSortedChildSizes(childConfig);
            if (childSizes.isEmpty()) {
                // When the list is empty, which means no child required resolutions are supported,
                // make the parent list to be empty to reflect this.
                return new ArrayList<>();
            }

            candidates = filterOutParentSizeThatIsTooSmall(childSizes, candidates);
            sizesTooLarge.retainAll(getParentSizesThatAreTooLarge(childSizes, candidates));
        }

        // Filter out sizes that are too large.
        List<Size> result = new ArrayList<>();
        for (Size candidate : candidates) {
            if (!sizesTooLarge.contains(candidate)) {
                result.add(candidate);
            }
        }
        return result;
    }

    /**
     * Gets child sizes sorted by {@link SupportedOutputSizesSorter}.
     *
     * <p>Notes that the input {@code childConfig} is expected to be one of the values that use to
     * construct the {@link ResolutionsMerger}, if not an IllegalArgumentException will be thrown.
     */
    @NonNull
    private List<Size> getSortedChildSizes(@NonNull UseCaseConfig<?> childConfig) {
        if (!mChildrenConfigs.contains(childConfig)) {
            throw new IllegalArgumentException("Invalid child config: " + childConfig);
        }

        // Since getSortedSupportedOutputSizes() might be time consuming, use caching to improve
        // the performance.
        if (mChildSizesCache.containsKey(childConfig)) {
            return Objects.requireNonNull(mChildSizesCache.get(childConfig));
        }

        List<Size> childSizes = mSizeSorter.getSortedSupportedOutputSizes(childConfig);
        mChildSizesCache.put(childConfig, childSizes);

        return childSizes;
    }

    private boolean needToAddSensorResolutions() {
        // Need to add sensor resolutions if any required resolution is not fallback aspect-ratio.
        for (Size size : getChildrenRequiredResolutions()) {
            if (!hasMatchingAspectRatio(size, mFallbackAspectRatio)) {
                return true;
            }
        }
        return false;
    }

    private boolean needToAddFallbackResolutions() {
        // Need to add fallback resolutions if any required resolution is fallback aspect-ratio.
        for (Size size : getChildrenRequiredResolutions()) {
            if (hasMatchingAspectRatio(size, mFallbackAspectRatio)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private Set<Size> getChildrenRequiredResolutions() {
        Set<Size> result = new HashSet<>();
        for (UseCaseConfig<?> childConfig : mChildrenConfigs) {
            List<Size> childSizes = getSortedChildSizes(childConfig);
            result.addAll(childSizes);
        }

        return result;
    }

    private boolean isSensorAspectRatio(@NonNull Size size) {
        return hasMatchingAspectRatio(size, mSensorAspectRatio);
    }

    private boolean isFallbackAspectRatio(@NonNull Size size) {
        return hasMatchingAspectRatio(size, mFallbackAspectRatio);
    }

    @NonNull
    private static List<Size> getSupportedPrivResolutions(
            @NonNull List<Pair<Integer, Size[]>> supportedResolutionsMap) {
        for (Pair<Integer, Size[]> pair : supportedResolutionsMap) {
            if (pair.first.equals(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)) {
                return Arrays.asList(pair.second);
            }
        }

        return new ArrayList<>();
    }

    /**
     * Returns the aspect-ratio of 4:3 or 16:9 that is closer to the sensor size.
     *
     * <p>Parent resolutions with sensor aspect-ratio are considered to be non-cropped, so child
     * resolution can have a different aspect-ratio than the parents without causing
     * double-cropping.
     */
    @NonNull
    private static Rational getSensorAspectRatio(@NonNull Size sensorSize) {
        Rational result = findCloserAspectRatio(sensorSize);
        Logger.d(TAG, "The closer aspect ratio to the sensor size (" + sensorSize + ") is "
                + result + ".");

        return result;
    }

    @NonNull
    private static Rational findCloserAspectRatio(@NonNull Size size) {
        double widthHeightRatio = size.getWidth() / (double) size.getHeight();

        if (widthHeightRatio > SAME_AREA_WIDTH_HEIGHT_RATIO) {
            return ASPECT_RATIO_16_9;
        } else {
            return ASPECT_RATIO_4_3;
        }
    }

    /**
     * Returns the aspect-ratio of 4:3 or 16:9 that is not the sensor aspect-ratio.
     *
     * <p>Parent resolutions with fallback aspect-ratio are considered to be cropped, so child
     * resolution should not different to the parent or double-cropping will happen.
     */
    @NonNull
    private static Rational getFallbackAspectRatio(@NonNull Rational sensorAspectRatio) {
        if (sensorAspectRatio.equals(ASPECT_RATIO_4_3)) {
            return ASPECT_RATIO_16_9;
        } else if (sensorAspectRatio.equals(ASPECT_RATIO_16_9)) {
            return ASPECT_RATIO_4_3;
        } else {
            throw new IllegalArgumentException("Invalid sensor aspect-ratio: " + sensorAspectRatio);
        }
    }

    /**
     * Sorts the input resolutions in descending order.
     */
    @VisibleForTesting
    static void sortInDescendingOrder(@NonNull List<Size> resolutions) {
        Collections.sort(resolutions, new CompareSizesByArea(true));
    }

    /**
     * Returns a list of resolution that all resolutions are with the input aspect-ratio.
     *
     * <p>The order of the {@code resolutionsToFilter} will be preserved in the resulting list.
     */
    @VisibleForTesting
    @NonNull
    static List<Size> filterResolutionsByAspectRatio(@NonNull Rational aspectRatio,
            @NonNull List<Size> resolutionsToFilter) {
        List<Size> result = new ArrayList<>();
        for (Size resolution : resolutionsToFilter) {
            if (hasMatchingAspectRatio(resolution, aspectRatio)) {
                result.add(resolution);
            }
        }

        return result;
    }

    /**
     * Filters out the parent size that is too small with consider target children sizes.
     *
     * <p>A size is too small if it cannot find any child size that can be cropped out without
     * upscaling.
     *
     * <p>The order of the {@code sortedParentSizes} will be preserved in the resulting list.
     *
     * <p>Assuming {@code sortedParentSizes} is sorted in descending order and all sizes have same
     * aspect-ratio.
     */
    @VisibleForTesting
    @NonNull
    static List<Size> filterOutParentSizeThatIsTooSmall(
            @NonNull Collection<Size> childSizes, @NonNull List<Size> sortedParentSizes) {
        if (childSizes.isEmpty() || sortedParentSizes.isEmpty()) {
            return new ArrayList<>();
        }

        // Find the smallest parent size that can be cropped to at least one child size without
        // upscaling by using binary search.
        int n = sortedParentSizes.size();
        int lo = 0;
        int hi = n - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo + 1) / 2;
            Size parentSize = sortedParentSizes.get(mid);
            if (isAnyChildSizeCanBeCroppedOutWithoutUpscalingParent(childSizes, parentSize)) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        // Add all parent sizes that can be cropped to at least one child size.
        List<Size> result = new ArrayList<>();
        for (int i = 0; i <= lo; i++) {
            result.add(sortedParentSizes.get(i));
        }

        return result;
    }

    private static boolean isAnyChildSizeCanBeCroppedOutWithoutUpscalingParent(
            @NonNull Collection<Size> childSizes, @NonNull Size parentSize) {
        for (Size childSize : childSizes) {
            if (!hasUpscaling(childSize, parentSize)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns resolutions that are too large with consider target children sizes.
     *
     * <p>A size is too large if there is another size smaller than that size and all children
     * sizes can be cropped from that other size without upscaling.
     *
     * <p>The order of the {@code sortedParentSizes} will be preserved in the resulting list.
     *
     * <p>Assuming {@code sortedParentSizes} is sorted in descending order and all sizes have same
     * aspect-ratio.
     */
    @VisibleForTesting
    @NonNull
    static List<Size> getParentSizesThatAreTooLarge(@NonNull Collection<Size> childSizes,
            @NonNull List<Size> sortedParentSizes) {
        if (childSizes.isEmpty() || sortedParentSizes.isEmpty()) {
            return new ArrayList<>();
        }

        // Find the smallest parent size that can be cropped to all child sizes without upscaling
        // by using binary search.
        int n = sortedParentSizes.size();
        int lo = 0;
        int hi = n - 1;
        while (lo < hi) {
            int mid = lo + (hi - lo + 1) / 2;
            Size parentSize = sortedParentSizes.get(mid);
            if (isAllChildSizesCanBeCroppedOutWithoutUpscalingParent(childSizes, parentSize)) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        // Add all parent sizes that can be cropped to all child sizes, except the smallest one.
        List<Size> result = new ArrayList<>();
        for (int i = 0; i < lo; i++) {
            result.add(sortedParentSizes.get(i));
        }

        return result;
    }

    private static boolean isAllChildSizesCanBeCroppedOutWithoutUpscalingParent(
            @NonNull Collection<Size> childSizes, @NonNull Size parentSize) {
        for (Size childSize : childSizes) {
            if (hasUpscaling(childSize, parentSize)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Whether the parent size needs upscaling to fill the child size.
     */
    @VisibleForTesting
    static boolean hasUpscaling(@NonNull Size childSize, @NonNull Size parentSize) {
        // Upscaling is needed if child size is larger than the parent.
        return childSize.getHeight() > parentSize.getHeight()
                || childSize.getWidth() > parentSize.getWidth();
    }
}
