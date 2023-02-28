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

package androidx.camera.camera2.internal.compat.workaround;

import android.util.Rational;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ExtraSupportedOutputSizeQuirk;
import androidx.camera.core.impl.utils.AspectRatioUtil;
import androidx.camera.core.impl.utils.CompareSizesByArea;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Helper class to provide the StreamConfigurationMap output sizes related correction functions.
 *
 * 1. ExtraSupportedOutputSizeQuirk
 * 2. ExcludedSupportedSizesContainer
 * 3. TargetAspectRatio
 */
@RequiresApi(21)
public class OutputSizesCorrector {
    private final String mCameraId;
    private final CameraCharacteristicsCompat mCameraCharacteristicsCompat;

    private final ExtraSupportedOutputSizeQuirk mExtraSupportedOutputSizeQuirk =
            DeviceQuirks.get(ExtraSupportedOutputSizeQuirk.class);

    private final ExcludedSupportedSizesContainer mExcludedSupportedSizesContainer;

    private final TargetAspectRatio mTargetAspectRatio = new TargetAspectRatio();

    /**
     * Constructor.
     */
    public OutputSizesCorrector(@NonNull String cameraId,
            @NonNull CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        mCameraId = cameraId;
        mCameraCharacteristicsCompat = cameraCharacteristicsCompat;
        mExcludedSupportedSizesContainer = new ExcludedSupportedSizesContainer(mCameraId);
    }

    /**
     * Applies the output sizes related quirks onto the input sizes array.
     */
    @Nullable
    public Size[] applyQuirks(@Nullable Size[] sizes, int format) {
        Size[] result = addExtraSupportedOutputSizesByFormat(sizes, format);
        result = excludeProblematicOutputSizesByFormat(result, format);
        return excludeOutputSizesByTargetAspectRatioWorkaround(result);
    }

    /**
     * Applies the output sizes related quirks onto the input sizes array.
     */
    @Nullable
    public <T> Size[] applyQuirks(@Nullable Size[] sizes, @NonNull Class<T> klass) {
        Size[] result = addExtraSupportedOutputSizesByClass(sizes, klass);
        result = excludeProblematicOutputSizesByClass(result, klass);
        return excludeOutputSizesByTargetAspectRatioWorkaround(result);
    }

    /**
     * Adds extra supported output sizes for the specified format by ExtraSupportedOutputSizeQuirk.
     */
    @Nullable
    private Size[] addExtraSupportedOutputSizesByFormat(@Nullable Size[] sizes, int format) {
        if (mExtraSupportedOutputSizeQuirk == null) {
            return sizes;
        }

        Size[] extraSizes = mExtraSupportedOutputSizeQuirk.getExtraSupportedResolutions(format);
        return concatNullableSizeLists(Arrays.asList(sizes), Arrays.asList(extraSizes)).toArray(
                new Size[0]);
    }

    /**
     * Adds extra supported output sizes for the specified class by ExtraSupportedOutputSizeQuirk.
     */
    @Nullable
    private <T> Size[] addExtraSupportedOutputSizesByClass(@Nullable Size[] sizes,
            @NonNull Class<T> klass) {
        if (mExtraSupportedOutputSizeQuirk == null) {
            return sizes;
        }

        Size[] extraSizes = mExtraSupportedOutputSizeQuirk.getExtraSupportedResolutions(klass);
        return concatNullableSizeLists(Arrays.asList(sizes), Arrays.asList(extraSizes)).toArray(
                new Size[0]);
    }

    /**
     * Excludes problematic output sizes for the specified format by
     * ExcludedSupportedSizesContainer.
     */
    @Nullable
    private Size[] excludeProblematicOutputSizesByFormat(@Nullable Size[] sizes, int format) {
        if (sizes == null) {
            return null;
        }

        List<Size> excludedSizes = mExcludedSupportedSizesContainer.get(format);

        List<Size> resultList = new ArrayList<>(Arrays.asList(sizes));
        resultList.removeAll(excludedSizes);

        if (resultList.isEmpty()) {
            return null;
        } else {
            return resultList.toArray(new Size[0]);
        }
    }

    /**
     * Excludes problematic output sizes for the specified class type by
     * ExcludedSupportedSizesContainer.
     */
    @Nullable
    private <T> Size[] excludeProblematicOutputSizesByClass(@Nullable Size[] sizes,
            @NonNull Class<T> klass) {
        if (sizes == null) {
            return null;
        }

        List<Size> excludedSizes = mExcludedSupportedSizesContainer.get(klass);

        List<Size> resultList = new ArrayList<>(Arrays.asList(sizes));
        resultList.removeAll(excludedSizes);

        if (resultList.isEmpty()) {
            return null;
        } else {
            return resultList.toArray(new Size[0]);
        }
    }

    /**
     * Excludes output sizes by TargetAspectRatio.
     */
    @Nullable
    private Size[] excludeOutputSizesByTargetAspectRatioWorkaround(@Nullable Size[] sizes) {
        if (sizes == null) {
            return null;
        }

        int targetAspectRatio = mTargetAspectRatio.get(mCameraId, mCameraCharacteristicsCompat);

        Rational ratio = null;

        switch (targetAspectRatio) {
            case TargetAspectRatio.RATIO_4_3:
                ratio = AspectRatioUtil.ASPECT_RATIO_4_3;
                break;
            case TargetAspectRatio.RATIO_16_9:
                ratio = AspectRatioUtil.ASPECT_RATIO_16_9;
                break;
            case TargetAspectRatio.RATIO_MAX_JPEG:
                Size maxJpegSize = Collections.max(Arrays.asList(sizes), new CompareSizesByArea());
                ratio = new Rational(maxJpegSize.getWidth(), maxJpegSize.getHeight());
                break;
            case TargetAspectRatio.RATIO_ORIGINAL:
                ratio = null;
        }

        if (ratio == null) {
            return sizes;
        }

        List<Size> resultList = new ArrayList<>();

        for (Size size : sizes) {
            if (AspectRatioUtil.hasMatchingAspectRatio(size, ratio)) {
                resultList.add(size);
            }
        }

        if (resultList.isEmpty()) {
            return null;
        } else {
            return resultList.toArray(new Size[0]);
        }
    }

    @Nullable
    private static List<Size> concatNullableSizeLists(@Nullable List<Size> sizeList1,
            @Nullable List<Size> sizeList2) {
        if (sizeList1 == null) {
            return sizeList2;
        } else if (sizeList2 == null) {
            return sizeList1;
        } else {
            List<Size> resultList = new ArrayList<>(sizeList1);
            resultList.addAll(sizeList2);
            return resultList;
        }
    }
}
