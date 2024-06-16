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
import static androidx.camera.core.impl.utils.TransformUtils.is90or270;
import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;
import static androidx.camera.core.impl.utils.TransformUtils.reverseSize;
import static androidx.camera.core.resolutionselector.ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE;

import static java.lang.Math.sqrt;

import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Pair;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.ImageOutputConfig;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.impl.utils.CompareSizesByArea;
import androidx.camera.core.internal.SupportedOutputSizesSorter;
import androidx.camera.core.resolutionselector.ResolutionSelector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A class for calculating parent resolutions based on the children's configs.
 */
public class ResolutionsMerger {

    private static final String TAG = "ResolutionsMerger";
    // The width to height ratio that has same area when cropping into 4:3 and 16:9.
    private static final double SAME_AREA_WIDTH_HEIGHT_RATIO = sqrt(4.0 / 3.0 * 16.0 / 9.0);

    @NonNull
    private final Size mSensorSize;
    @NonNull
    private final Rational mSensorAspectRatio;
    @NonNull
    private final Rational mFallbackAspectRatio;
    @NonNull
    private final Set<UseCaseConfig<?>> mChildrenConfigs;
    @NonNull
    private final SupportedOutputSizesSorter mSizeSorter;
    @NonNull
    private final CameraInfoInternal mCameraInfo;
    @NonNull
    private final Map<UseCaseConfig<?>, List<Size>> mChildSizesCache = new HashMap<>();

    ResolutionsMerger(@NonNull CameraInternal cameraInternal,
            @NonNull Set<UseCaseConfig<?>> childrenConfigs) {
        this(rectToSize(cameraInternal.getCameraControlInternal().getSensorRect()),
                cameraInternal.getCameraInfoInternal(), childrenConfigs);
    }

    private ResolutionsMerger(@NonNull Size sensorSize, @NonNull CameraInfoInternal cameraInfo,
            @NonNull Set<UseCaseConfig<?>> childrenConfigs) {
        this(sensorSize, cameraInfo, childrenConfigs,
                new SupportedOutputSizesSorter(cameraInfo, sensorSize));
    }

    @VisibleForTesting
    ResolutionsMerger(@NonNull Size sensorSize, @NonNull CameraInfoInternal cameraInfo,
            @NonNull Set<UseCaseConfig<?>> childrenConfigs,
            @NonNull SupportedOutputSizesSorter supportedOutputSizesSorter) {
        mSensorSize = sensorSize;
        mSensorAspectRatio = getSensorAspectRatio(sensorSize);
        mFallbackAspectRatio = getFallbackAspectRatio(mSensorAspectRatio);
        mCameraInfo = cameraInfo;
        mChildrenConfigs = childrenConfigs;
        mSizeSorter = supportedOutputSizesSorter;
    }

    /**
     * Returns a list of {@link Surface} resolution sorted by priority.
     *
     * <p> This method calculates the resolution for the parent {@link StreamSharing} based on 1)
     * the supported PRIV resolutions, 2) the sensor size and 3) the children's configs.
     */
    @NonNull
    List<Size> getMergedResolutions(@NonNull MutableConfig parentConfig) {
        List<Size> candidateSizes = getCameraSupportedResolutions();

        // Add high resolutions if they need to be included.
        if (shouldIncludeHighResolutions()) {
            candidateSizes = new ArrayList<>(candidateSizes);
            candidateSizes.addAll(getCameraSupportedHighResolutions());
        }

        // Use parent config's supported resolutions when it is set (e.g. Extensions may have
        // its limitations on resolutions).
        List<Pair<Integer, Size[]>> parentSupportedSizesMap =
                parentConfig.retrieveOption(OPTION_SUPPORTED_RESOLUTIONS, null);
        if (parentSupportedSizesMap != null) {
            candidateSizes = getSupportedPrivResolutions(parentSupportedSizesMap);
        }

        return selectParentResolutions(candidateSizes);
    }

    /**
     * Returns a preferred pair composed of a crop rect before scaling and a size after scaling.
     *
     * <p>The first size in the child's ordered size list that does not require the parent to
     * upscale and does not cause double-cropping will be used to generate the pair, or {@code
     * parentCropRect} will be used if no matching is found.
     *
     * <p>The returned crop rect and size will have the same aspect-ratio. When {@code
     * isViewportSet}, the viewport setting will be respected, so the aspect-ratio of resulting
     * crop rect and size will be same as {@code parentCropRect}.
     *
     * <p>Notes that the input {@code childConfig} is expected to be one of the values that use to
     * construct the {@link ResolutionsMerger}, if not an IllegalArgumentException will be thrown.
     */
    @NonNull
    Pair<Rect, Size> getPreferredChildSizePair(@NonNull UseCaseConfig<?> childConfig,
            @NonNull Rect parentCropRect, int sensorToBufferRotationDegrees,
            boolean isViewportSet) {
        // For easier in following computations, width and height are reverted when the rotation
        // degrees of sensor-to-buffer is 90 or 270.
        boolean isWidthHeightRevertedForComputation = false;
        if (is90or270(sensorToBufferRotationDegrees)) {
            parentCropRect = reverseRect(parentCropRect);
            isWidthHeightRevertedForComputation = true;
        }

        // Get preferred child size pair.
        Pair<Rect, Size> pair = getPreferredChildSizePairInternal(parentCropRect, childConfig,
                isViewportSet);
        Rect cropRectBeforeScaling = pair.first;
        Size childSizeToScale = pair.second;

        // Restore the reversion of width and height
        if (isWidthHeightRevertedForComputation) {
            childSizeToScale = reverseSize(childSizeToScale);
            cropRectBeforeScaling = reverseRect(cropRectBeforeScaling);
        }

        return new Pair<>(cropRectBeforeScaling, childSizeToScale);

    }

    @NonNull
    private Pair<Rect, Size> getPreferredChildSizePairInternal(@NonNull Rect parentCropRect,
            @NonNull UseCaseConfig<?> childConfig, boolean isViewportSet) {
        Rect cropRectBeforeScaling;
        Size childSizeToScale;

        if (isViewportSet) {
            cropRectBeforeScaling = parentCropRect;

            // When viewport is set, child size needs to be cropped to match viewport's
            // aspect-ratio.
            Size viewPortSize = rectToSize(parentCropRect);
            childSizeToScale = getPreferredChildSizeForViewport(viewPortSize, childConfig);
        } else {
            Size parentSize = rectToSize(parentCropRect);
            childSizeToScale = getPreferredChildSize(parentSize, childConfig);
            cropRectBeforeScaling = getCropRectOfReferenceAspectRatio(parentSize, childSizeToScale);
        }

        return new Pair<>(cropRectBeforeScaling, childSizeToScale);
    }

    /**
     * Returns the preferred child size with considering parent size and child's configuration.
     *
     * <p>Returns the first size in the child's ordered size list that can be cropped from {@code
     * parentSize} without upscaling it, or {@code parentSize} if no matching is found.
     *
     * <p>The child size not causing double-cropping will be selected in priority.
     *
     * <p>Notes that the input {@code childConfig} is expected to be one of the values that use to
     * construct the {@link ResolutionsMerger}, if not an IllegalArgumentException will be thrown.
     */
    @VisibleForTesting
    @NonNull
    Size getPreferredChildSize(@NonNull Size parentSize, @NonNull UseCaseConfig<?> childConfig) {
        // Select the first child resolution that does not result in double-cropping and upscaling.
        List<Size> candidateChildSizes = getSortedChildSizes(childConfig);
        for (Size childSize : candidateChildSizes) {
            if (isDoubleCropping(parentSize, childSize)) {
                continue;
            }

            if (!hasUpscaling(childSize, parentSize)) {
                return childSize;
            }
        }

        // Select the first child resolution that does not result in upscaling (might have
        // smaller FOV due to double-cropping). This may occur when selecting parent resolutions
        // that are expected to result in double-clipping in order to reduce binding failures in
        // edge cases.
        for (Size childSize : candidateChildSizes) {
            if (!hasUpscaling(childSize, parentSize)) {
                return childSize;
            }
        }

        return parentSize;
    }

    /**
     * Returns the preferred child size with considering config and viewport applied parent size.
     *
     * <p>Returns the first size in the child's ordered size list that can be cropped to same
     * aspect-ratio as {@code parentSize} without upscaling, or {@code parentSize} if no matching
     * is found.
     *
     * <p>Notes that the input {@code childConfig} is expected to be one of the values that use to
     * construct the {@link ResolutionsMerger}, if not an IllegalArgumentException will be thrown.
     */
    @VisibleForTesting
    @NonNull
    Size getPreferredChildSizeForViewport(@NonNull Size parentSize,
            @NonNull UseCaseConfig<?> childConfig) {
        List<Size> candidateChildSizes = getSortedChildSizes(childConfig);

        for (Size childSize : candidateChildSizes) {
            Size childSizeToCrop = rectToSize(
                    getCropRectOfReferenceAspectRatio(childSize, parentSize));

            if (!hasUpscaling(childSizeToCrop, parentSize)) {
                return childSizeToCrop;
            }
        }

        return parentSize;
    }

    @NonNull
    private List<Size> getCameraSupportedResolutions() {
        return mCameraInfo.getSupportedResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
    }

    @NonNull
    private List<Size> getCameraSupportedHighResolutions() {
        return mCameraInfo.getSupportedHighResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
    }

    private boolean shouldIncludeHighResolutions() {
        // High resolutions need to be included if the feature is not disabled and allowed in any
        // child configuration.
        for (UseCaseConfig<?> childConfig : mChildrenConfigs) {
            if (childConfig.isHighResolutionDisabled(false)) {
                continue;
            }

            if (childConfig instanceof ImageOutputConfig) {
                ResolutionSelector resolutionSelector =
                        ((ImageOutputConfig) childConfig).getResolutionSelector(null);
                if (resolutionSelector != null && resolutionSelector.getAllowedResolutionMode()
                        == PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE) {
                    return true;
                }
            }
        }

        return false;
    }

    @NonNull
    private List<Size> selectParentResolutions(@NonNull List<Size> candidateParentResolutions) {
        // The following sequence of parent resolution selection is used to prevent double-cropping
        // from happening:
        // 1. Add sensor aspect-ratio resolutions, which do not result in double-cropping with any
        // aspect-ratio of child resolution. This is to provide parent resolution that can be
        // accepted by children in general cases.
        // 2. Add fallback aspect-ratio (the one between 4:3 and 16:9 that is not sensor
        // aspect-ratio) resolutions, that can crop/downscale to at least one child size for
        // each child.
        // 3. Add other aspect-ratio resolutions, that can crop/downscale to at least one child
        // size for each child.
        // The parent sizes added in step 2 and step 3 will only be used to crop to child sizes
        // that do not result in double-cropping. For example, if childRatio < parentRatio <
        // sensorRatio or childRatio > parentRatio > sensorRatio, then there is no double-cropping.
        List<Size> result = new ArrayList<>();

        // Add resolutions for sensor aspect-ratio.
        if (needToAddSensorResolutions()) {
            result.addAll(selectParentResolutionsByAspectRatio(mSensorAspectRatio,
                    candidateParentResolutions, false));
        }

        // Add resolutions for fallback aspect-ratio.
        result.addAll(selectParentResolutionsByAspectRatio(mFallbackAspectRatio,
                candidateParentResolutions, false));

        // Add other aspect-ratio resolutions. Resolutions with larger FOV will be added first.
        result.addAll(selectOtherAspectRatioParentResolutionsWithFovPriority(
                candidateParentResolutions, false));

        if (result.isEmpty()) {
            // When the resulting parent resolution list is empty (this may be due to the camera
            // not supporting 4:3 and 16:9 resolutions or a strict ResolutionSelector settings),
            // add resolutions that are neither 4:3 nor 16:9 to prevent binding failures.
            // Resolutions with larger FOV will be added first.
            Logger.w(TAG, "Failed to find a parent resolution that does not result in "
                    + "double-cropping, this might due to camera not supporting 4:3 and 16:9"
                    + "resolutions or a strict ResolutionSelector settings. Starting resolution "
                    + "selection process with resolutions that might have a smaller FOV.");
            result.addAll(selectOtherAspectRatioParentResolutionsWithFovPriority(
                    candidateParentResolutions, true));
        }

        Logger.d(TAG, "Parent resolutions: " + result);

        return result;
    }

    private List<Size> selectParentResolutionsByAspectRatio(@NonNull Rational aspectRatio,
            @NonNull List<Size> candidateParentResolutions, boolean allowDoubleCropping) {
        List<Size> candidates = filterResolutionsByAspectRatio(aspectRatio,
                candidateParentResolutions);
        sortInDescendingOrder(candidates);

        // Filter resolutions that are too small and track resolutions that might be too large.
        Set<Size> sizesTooLarge = new HashSet<>(candidates);
        for (UseCaseConfig<?> childConfig : mChildrenConfigs) {
            List<Size> childSizes = getSortedChildSizes(childConfig);
            if (!allowDoubleCropping) {
                childSizes = filterOutChildSizesCausingDoubleCropping(aspectRatio, childSizes);
            }
            if (childSizes.isEmpty()) {
                // When the list is empty, which means no child sizes match requirement, make the
                // parent list to be empty to reflect this.
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

    @NonNull
    private List<Size> selectOtherAspectRatioParentResolutionsWithFovPriority(
            @NonNull List<Size> candidates, boolean allowDoubleCropping) {
        Map<Rational, List<Size>> ratioToSizesMap = groupSizesByAspectRatio(candidates);

        // Get aspect-ratio of candidate parent sizes and sort by overlapping area (FOV) in
        // descending order.
        List<Rational> ratios = new ArrayList<>(ratioToSizesMap.keySet());
        sortByFov(ratios);

        // Add resolutions that are neither 4:3 nor 16:9. Resolutions with larger FOV will be
        // added first.
        List<Size> result = new ArrayList<>();
        for (Rational ratio: ratios) {
            if (ratio.equals(ASPECT_RATIO_16_9) || ratio.equals(ASPECT_RATIO_4_3)) {
                continue;
            }

            List<Size> sizes = Objects.requireNonNull(ratioToSizesMap.get(ratio));
            result.addAll(selectParentResolutionsByAspectRatio(ratio, sizes, allowDoubleCropping));
        }

        return result;
    }

    @NonNull
    private Map<Rational, List<Size>> groupSizesByAspectRatio(@NonNull List<Size> sizes) {
        Map<Rational, List<Size>> result = new HashMap<>();

        // Add 4:3 and 16:9 first so that other mod-16 sizes won't introduce additional keys.
        result.put(ASPECT_RATIO_4_3, new ArrayList<>());
        result.put(ASPECT_RATIO_16_9, new ArrayList<>());

        // Add 4:3 and 16:9 first so that sizes won't be grouped to other close aspect-ratios.
        List<Rational> ratios = new ArrayList<>();
        ratios.add(ASPECT_RATIO_4_3);
        ratios.add(ASPECT_RATIO_16_9);

        // Group sizes by aspect-ratio with mod-16 considered.
        for (Size size : sizes) {
            if (size.getHeight() <= 0) {
                continue;
            }

            // Get the aspect-ratio group if it is ready existed.
            List<Size> group = null;
            for (Rational ratio : ratios) {
                if (hasMatchingAspectRatio(size, ratio)) {
                    group = result.get(ratio);
                    break;
                }
            }

            // Create a new aspect-ratio group if it is not existed.
            if (group == null) {
                group = new ArrayList<>();
                Rational newRatio = toRational(size);
                ratios.add(newRatio);
                result.put(newRatio, group);
            }

            Objects.requireNonNull(group).add(size);
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
        childSizes = filterOutChildSizesThatWillNeverBeSelected(childSizes);
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

    @NonNull
    private Set<Size> getChildrenRequiredResolutions() {
        Set<Size> result = new HashSet<>();
        for (UseCaseConfig<?> childConfig : mChildrenConfigs) {
            List<Size> childSizes = getSortedChildSizes(childConfig);
            result.addAll(childSizes);
        }

        return result;
    }

    @NonNull
    private List<Size> filterOutChildSizesCausingDoubleCropping(@NonNull Rational parentAspectRatio,
            @NonNull List<Size> childSizes) {
        List<Size> result = new ArrayList<>();
        for (Size childSize: childSizes) {
            if (!isDoubleCropping(parentAspectRatio, childSize)) {
                result.add(childSize);
            }
        }

        return result;
    }

    private boolean isDoubleCropping(@NonNull Rational parentRatio, @NonNull Size childSize) {
        // No double cropping results when the sensor and parent have the same aspect ratio or
        // when the parent and child have the same aspect ratio.
        if (mSensorAspectRatio.equals(parentRatio) || hasMatchingAspectRatio(childSize,
                parentRatio)) {
            return false;
        }

        // When the cropping from sensor to parent and from parent to child are in the same
        // direction, there is no double-cropping.
        return areCroppingInDifferentDirection(
                mSensorAspectRatio.floatValue(),
                parentRatio.floatValue(),
                toRationalWithMod16Considered(childSize).floatValue()
        );
    }

    private boolean isDoubleCropping(@NonNull Size parentSize, @NonNull Size childSize) {
        Rational parentRatio = toRationalWithMod16Considered(parentSize);
        return isDoubleCropping(parentRatio, childSize);
    }

    private boolean areCroppingInDifferentDirection(float sensorRatioValue, float parentRatioValue,
            float childRatioValue) {
        // There is only one cropping direction When the sensor and parent have the same
        // aspect-ratio or when the parent and child have the same aspect-ratio.
        if (sensorRatioValue == parentRatioValue || parentRatioValue == childRatioValue) {
            return false;
        }

        // When childRatio < parentRatio < sensorRatio or childRatio > parentRatio > sensorRatio,
        // the cropping from sensor to parent and from parent to child are in the same direction.
        if (sensorRatioValue > parentRatioValue) {
            return parentRatioValue < childRatioValue;
        } else {
            return parentRatioValue > childRatioValue;
        }
    }

    /**
     * Sorts the input aspect-ratio by overlapping area with sensor (FOV) in descending order.
     */
    private void sortByFov(@NonNull List<Rational> ratios) {
        Rational actualSensorAspectRatio = toRational(mSensorSize);
        Collections.sort(ratios, new CompareAspectRatioByOverlappingAreaToReference(
                actualSensorAspectRatio, true));
    }

    /**
     * Returns the crop rectangle for target that has the same aspect-ratio as the reference.
     */
    @VisibleForTesting
    @NonNull
    static Rect getCropRectOfReferenceAspectRatio(@NonNull Size targetSize,
            @NonNull Size referenceSize) {
        Rational referenceRatio = toRational(referenceSize);
        return getCenterCroppedRectangle(referenceRatio, targetSize);
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

    /** @noinspection SuspiciousNameCombination */
    @VisibleForTesting
    @NonNull
    static Rect reverseRect(@NonNull Rect rect) {
        return new Rect(rect.top, rect.left, rect.bottom, rect.right);
    }

    @NonNull
    private static Rect getCenterCroppedRectangle(@NonNull Rational cropRatio,
            @NonNull Size baseSize) {
        int width = baseSize.getWidth();
        int height = baseSize.getHeight();
        Rational referenceRatio = toRational(baseSize);

        RectF cropRectInFloat;
        if (cropRatio.floatValue() == referenceRatio.floatValue()) {
            cropRectInFloat = new RectF(0, 0, width, height);
        } else if (cropRatio.floatValue() > referenceRatio.floatValue()) {
            float cropHeight = width / cropRatio.floatValue();
            float yOffset = (height - cropHeight) / 2;
            cropRectInFloat = new RectF(0, yOffset, width, yOffset + cropHeight);
        } else {
            float cropWidth = height * cropRatio.floatValue();
            float xOffset = (width - cropWidth) / 2;
            cropRectInFloat = new RectF(xOffset, 0, xOffset + cropWidth, height);
        }

        // RectF to Rect.
        Rect result = new Rect();
        cropRectInFloat.round(result);

        return result;
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

    /** Removes duplicate sizes and preserves the order. */
    @NonNull
    private static List<Size> removeDuplicates(@NonNull List<Size> resolutions) {
        if (resolutions.isEmpty()) {
            return resolutions;
        }
        return new ArrayList<>(new LinkedHashSet<>(resolutions));
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
     * <p>The order of the {@code parentSizes} will be preserved in the resulting list.
     */
    @VisibleForTesting
    @NonNull
    static List<Size> filterOutParentSizeThatIsTooSmall(@NonNull Collection<Size> childSizes,
            @NonNull List<Size> parentSizes) {
        if (childSizes.isEmpty() || parentSizes.isEmpty()) {
            return new ArrayList<>();
        }

        List<Size> result = new ArrayList<>();
        for (Size parentSize : parentSizes) {
            if (isAnyChildSizeCanBeCroppedOutWithoutUpscalingParent(childSizes, parentSize)) {
                result.add(parentSize);
            }
        }

        return result;
    }

    /**
     * Filters out child sizes that will never be selected.
     *
     * <p>A child size will never be selected if there is another same aspect-ratio child size
     *  before it and has smaller size.
     */
    private static List<Size> filterOutChildSizesThatWillNeverBeSelected(
            @NonNull List<Size> childSizes) {
        Map<Rational, Size> smallestSizeMap = new HashMap<>();
        List<Size> result = new ArrayList<>();
        for (Size size : childSizes) {
            // Check if a same aspect-ratio size already seen.
            Rational keyRatio = null;
            for (Rational seenRatio : smallestSizeMap.keySet()) {
                if (hasMatchingAspectRatio(size, seenRatio)) {
                    keyRatio = seenRatio;
                    break;
                }
            }

            if (keyRatio != null) {
                // Filter out child size if it is not smaller than previous same aspect-ratio size.
                Size smallestSize = Objects.requireNonNull(smallestSizeMap.get(keyRatio));
                if (size.getHeight() > smallestSize.getHeight()
                        || size.getWidth() > smallestSize.getWidth()
                        || (size.getWidth() == smallestSize.getWidth()
                        && size.getHeight() == smallestSize.getHeight())) {
                    continue;
                }
            } else {
                keyRatio = toRational(size);
            }

            result.add(size);
            smallestSizeMap.put(keyRatio, size);
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
     * <p>The order of the {@code parentSizes} will be preserved in the resulting list.
     */
    @VisibleForTesting
    @NonNull
    static List<Size> getParentSizesThatAreTooLarge(@NonNull Collection<Size> childSizes,
            @NonNull List<Size> parentSizes) {
        if (childSizes.isEmpty() || parentSizes.isEmpty()) {
            return new ArrayList<>();
        }
        // This method requires no duplicate parentSizes to correctly remove the last item.
        // For example, if parentSizes = [1280x720, 1280x720] and 1280x720 can cover childSizes,
        // removing the last item would cause 1280x720 to be mistakenly considered too large.
        parentSizes = removeDuplicates(parentSizes);

        List<Size> result = new ArrayList<>();
        for (Size parentSize : parentSizes) {
            if (isAllChildSizesCanBeCroppedOutWithoutUpscalingParent(childSizes, parentSize)) {
                result.add(parentSize);
            }
        }

        // Remove the last element, which is the smallest parent size that can be cropped to all
        // child sizes and should remain in the final parent size list.
        if (!result.isEmpty()) {
            result.remove(result.size() - 1);
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

    @NonNull
    private static Rational toRationalWithMod16Considered(@NonNull Size size) {
        // For 4:3 and 16:9, use hasMatchingAspectRatio to take "mod 16 calculation" into
        // consideration. For example, a standard 16:9 supported size is 1920x1080. It may become
        // 1920x1088 on some devices because 1088 is multiple of 16.
        if (hasMatchingAspectRatio(size, ASPECT_RATIO_4_3)) {
            return ASPECT_RATIO_4_3;
        } else if (hasMatchingAspectRatio(size, ASPECT_RATIO_16_9)) {
            return ASPECT_RATIO_16_9;
        } else {
            return toRational(size);
        }
    }

    @NonNull
    private static Rational toRational(@NonNull Size size) {
        return new Rational(size.getWidth(), size.getHeight());
    }

    private static float computeAreaOverlapping(@NonNull Rational croppingRatio,
            @NonNull Rational baseRatio) {
        float croppingRatioValue = croppingRatio.floatValue();
        float baseRatioValue = baseRatio.floatValue();

        return (croppingRatioValue > baseRatioValue) ? baseRatioValue / croppingRatioValue
                : croppingRatioValue / baseRatioValue;
    }

    private static class CompareAspectRatioByOverlappingAreaToReference implements
            Comparator<Rational> {
        @NonNull
        private final Rational mReferenceAspectRatio;
        private final boolean mReverse;

        /** Creates a comparator which can reverse the total ordering. */
        CompareAspectRatioByOverlappingAreaToReference(@NonNull Rational referenceAspectRatio,
                boolean reverse) {
            mReferenceAspectRatio = referenceAspectRatio;
            mReverse = reverse;
        }

        @Override
        public int compare(@NonNull Rational lhs, @NonNull Rational rhs) {
            float lhsOverlapping = computeAreaOverlapping(lhs, mReferenceAspectRatio);
            float rhsOverlapping = computeAreaOverlapping(rhs, mReferenceAspectRatio);

            if (mReverse) {
                return Float.compare(rhsOverlapping, lhsOverlapping);
            } else {
                return Float.compare(lhsOverlapping, rhsOverlapping);
            }
        }
    }
}
