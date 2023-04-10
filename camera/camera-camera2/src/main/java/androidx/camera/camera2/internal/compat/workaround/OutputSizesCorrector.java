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
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.ExtraSupportedOutputSizeQuirk;
import androidx.camera.core.Logger;
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
    private static final String TAG = "OutputSizesCorrector";
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
    @NonNull
    public Size[] applyQuirks(@NonNull Size[] sizes, int format) {
        List<Size> sizeList = new ArrayList<>(Arrays.asList(sizes));
        addExtraSupportedOutputSizesByFormat(sizeList, format);
        excludeProblematicOutputSizesByFormat(sizeList, format);
        if (sizeList.isEmpty()) {
            Logger.w(TAG, "Sizes array becomes empty after excluding problematic output sizes.");
        }
        Size[] resultSizeArray = excludeOutputSizesByTargetAspectRatioWorkaround(sizeList);
        if (resultSizeArray.length == 0) {
            Logger.w(TAG, "Sizes array becomes empty after excluding output sizes by target "
                    + "aspect ratio workaround.");
        }
        return resultSizeArray;
    }

    /**
     * Applies the output sizes related quirks onto the input sizes array.
     */
    @NonNull
    public <T> Size[] applyQuirks(@NonNull Size[] sizes, @NonNull Class<T> klass) {
        List<Size> sizeList = new ArrayList<>(Arrays.asList(sizes));
        addExtraSupportedOutputSizesByClass(sizeList, klass);
        excludeProblematicOutputSizesByClass(sizeList, klass);
        if (sizeList.isEmpty()) {
            Logger.w(TAG, "Sizes array becomes empty after excluding problematic output sizes.");
        }
        Size[] resultSizeArray = excludeOutputSizesByTargetAspectRatioWorkaround(sizeList);
        if (resultSizeArray.length == 0) {
            Logger.w(TAG, "Sizes array becomes empty after excluding output sizes by target "
                    + "aspect ratio workaround.");
        }
        return resultSizeArray;
    }

    /**
     * Adds extra supported output sizes for the specified format by ExtraSupportedOutputSizeQuirk.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param format the image format to apply the workaround
     */
    private void addExtraSupportedOutputSizesByFormat(@NonNull List<Size> sizeList, int format) {
        if (mExtraSupportedOutputSizeQuirk == null) {
            return;
        }

        Size[] extraSizes = mExtraSupportedOutputSizeQuirk.getExtraSupportedResolutions(format);

        if (extraSizes.length > 0) {
            sizeList.addAll(Arrays.asList(extraSizes));
        }
    }

    /**
     * Adds extra supported output sizes for the specified class by ExtraSupportedOutputSizeQuirk.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param klass the class to apply the workaround
     */
    private void addExtraSupportedOutputSizesByClass(@NonNull List<Size> sizeList,
            @NonNull Class<?> klass) {
        if (mExtraSupportedOutputSizeQuirk == null) {
            return;
        }

        Size[] extraSizes = mExtraSupportedOutputSizeQuirk.getExtraSupportedResolutions(klass);

        if (extraSizes.length > 0) {
            sizeList.addAll(Arrays.asList(extraSizes));
        }
    }

    /**
     * Excludes problematic output sizes for the specified format by
     * ExcludedSupportedSizesContainer.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param format the image format to apply the workaround
     */
    private void excludeProblematicOutputSizesByFormat(@NonNull List<Size> sizeList, int format) {
        List<Size> excludedSizes = mExcludedSupportedSizesContainer.get(format);

        if (excludedSizes.isEmpty()) {
            return;
        }

        sizeList.removeAll(excludedSizes);
    }

    /**
     * Excludes problematic output sizes for the specified class type by
     * ExcludedSupportedSizesContainer.
     *
     * @param sizeList the original sizes list which must be a mutable list
     * @param klass the class to apply the workaround
     */
    private void excludeProblematicOutputSizesByClass(@NonNull List<Size> sizeList,
            @NonNull Class<?> klass) {
        List<Size> excludedSizes = mExcludedSupportedSizesContainer.get(klass);

        if (excludedSizes.isEmpty()) {
            return;
        }

        sizeList.removeAll(excludedSizes);
    }

    /**
     * Excludes output sizes by TargetAspectRatio.
     *
     * @param sizeList the original sizes list
     */
    @NonNull
    private Size[] excludeOutputSizesByTargetAspectRatioWorkaround(@NonNull List<Size> sizeList) {
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
                Size maxJpegSize = Collections.max(sizeList, new CompareSizesByArea());
                ratio = new Rational(maxJpegSize.getWidth(), maxJpegSize.getHeight());
                break;
            case TargetAspectRatio.RATIO_ORIGINAL:
                ratio = null;
        }

        if (ratio == null) {
            return sizeList.toArray(sizeList.toArray(new Size[0]));
        }

        List<Size> resultList = new ArrayList<>();

        for (Size size : sizeList) {
            if (AspectRatioUtil.hasMatchingAspectRatio(size, ratio)) {
                resultList.add(size);
            }
        }

        return resultList.toArray(new Size[0]);
    }
}
