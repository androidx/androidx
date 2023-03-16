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

import static androidx.camera.core.impl.UseCaseConfig.OPTION_SURFACE_OCCUPANCY_PRIORITY;
import static androidx.camera.core.impl.utils.TransformUtils.rectToSize;
import static androidx.core.util.Preconditions.checkState;

import static java.util.Objects.requireNonNull;

import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.CameraInternal;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.UseCaseConfig;
import androidx.camera.core.internal.SupportedOutputSizesSorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A class for calculating parent resolutions based on the children's configs.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ResolutionMerger {

    @NonNull
    private final Size mSensorSize;
    @NonNull
    private final SupportedOutputSizesSorter mChildSizeSorter;
    @NonNull
    private final List<Size> mParentSizes;

    ResolutionMerger(@NonNull CameraInternal cameraInternal) {
        this(rectToSize(cameraInternal.getCameraControlInternal().getSensorRect()),
                cameraInternal.getCameraInfoInternal());
    }

    private ResolutionMerger(@NonNull Size sensorSize,
            @NonNull CameraInfoInternal cameraInfoInternal) {
        this(sensorSize, new SupportedOutputSizesSorter(cameraInfoInternal),
                cameraInfoInternal.getSupportedResolutions(
                        ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE));
    }

    ResolutionMerger(@NonNull Size sensorSize,
            @NonNull SupportedOutputSizesSorter supportedOutputSizesSorter,
            @NonNull List<Size> parentSizes) {
        mSensorSize = sensorSize;
        mChildSizeSorter = supportedOutputSizesSorter;
        mParentSizes = parentSizes;
    }

    /**
     * Returns a list of parent resolution sorted by preference.
     *
     * <p> This method calculates the resolution for the parent {@link StreamSharing} based on 1)
     * the supported PRIV resolutions, 2) the sensor size and 3) the children's configs.
     */
    List<Size> getMergedResolutions(@NonNull Set<UseCaseConfig<?>> childrenConfigs) {
        List<Size> parentSizes = new ArrayList<>(mParentSizes);
        Set<Integer> priorities = new HashSet<>();
        // Parent/child scores, in the form of <parent size, <child priority, score>>.
        Map<Size, Map<Integer, Integer>> parentSizeScores = new HashMap<>();

        for (UseCaseConfig<?> childConfig : childrenConfigs) {
            // Get sorted child sizes.
            List<Size> childrenSizes = mChildSizeSorter.getSortedSupportedOutputSizes(childConfig);

            // Get child's Surface priority
            int childPriority = requireNonNull(childConfig.retrieveOption(
                    OPTION_SURFACE_OCCUPANCY_PRIORITY, null));
            checkState(!priorities.contains(childPriority),
                    "No 2 UseCases can have the same Surface priority.");
            priorities.add(childPriority);

            // Score parent sizes against this child.
            for (Size parentSize : mParentSizes) {
                int parentChildScore = scoreParentAgainstChild(
                        parentSize, mSensorSize, childrenSizes);
                if (!parentSizeScores.containsKey(parentSize)) {
                    parentSizeScores.put(parentSize, new HashMap<>());
                }
                requireNonNull(parentSizeScores.get(parentSize))
                        .put(childPriority, parentChildScore);
            }
        }

        // Sort parent sizes by scores.
        Collections.sort(parentSizes, new ParentSizeComparator(parentSizeScores, priorities));
        return parentSizes;
    }

    /**
     * Scores the given parent size given the child's sorted sizes.
     *
     * <p> The score is the index of the highest ranking child size that works with the parent
     * size without upscaling or double cropping. If no child size works with the parent size, it
     * returns {@link Integer#MAX_VALUE}.
     */
    static int scoreParentAgainstChild(@NonNull Size parentSize, @NonNull Size sensorSize,
            @NonNull List<Size> childSizes) {
        for (int i = 0; i < childSizes.size(); i++) {
            Size childSize = childSizes.get(i);
            if (hasUpscaling(childSize, parentSize)) {
                continue;
            }
            if (isDoubleCropping(childSize, parentSize, sensorSize)) {
                continue;
            }
            return i;
        }
        // The parent size does not work with any of the children sizes..
        return Integer.MAX_VALUE;
    }

    /**
     * Whether the parent size needs upscaling to fill the child size.
     */
    private static boolean hasUpscaling(@NonNull Size childSize, @NonNull Size parentSize) {
        // Upscaling is needed if child size is larger than the parent.
        return childSize.getHeight() > parentSize.getHeight()
                || childSize.getWidth() > parentSize.getWidth();
    }

    /**
     * Whether there is double cropping, given the child size, parent size and sensor size.
     */
    static boolean isDoubleCropping(@NonNull Size child, @NonNull Size parent,
            @NonNull Size sensor) {
        // Crop the sensor size by the parent size.
        Size afterCroppingParent = getCroppedSize(sensor, parent);
        // Crop the result again by the child size.
        Size afterCroppingChild = getCroppedSize(afterCroppingParent, child);
        // If the result is smaller than the sensor in both width/height, then it's double cropping.
        return afterCroppingChild.getWidth() < sensor.getWidth()
                && afterCroppingChild.getHeight() < sensor.getHeight();
    }

    /**
     * Returns the size after cropping the original size by the crop size.
     */
    static Size getCroppedSize(@NonNull Size original, @NonNull Size crop) {
        float cropAspectRatio = (float) crop.getWidth() / crop.getHeight();
        float originalAspectRatio = (float) original.getWidth() / original.getHeight();
        if (originalAspectRatio > cropAspectRatio) {
            return new Size(original.getHeight() * crop.getWidth() / crop.getHeight(),
                    original.getHeight());
        } else {
            return new Size(original.getWidth(),
                    original.getWidth() * crop.getHeight() / crop.getWidth());
        }
    }
}
