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
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_CUSTOM_ORDERED_RESOLUTIONS;
import static androidx.camera.core.impl.ImageOutputConfig.OPTION_SUPPORTED_RESOLUTIONS;

import android.os.Build;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.MutableConfig;
import androidx.camera.core.impl.UseCaseConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for calculating resolutions.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ResolutionUtils {

    private ResolutionUtils() {
    }

    /**
     * Returns a list of {@link Surface} resolution sorted by priority.
     *
     * <p> This method calculates the resolution for the parent {@link StreamSharing} based on 1)
     * the supported PRIV resolutions, 2) the sensor size and 3) the children's configs.
     */
    static List<Size> getMergedResolutions(
            @NonNull List<Size> cameraSupportedResolutions,
            @NonNull Size sensorSize,
            @NonNull MutableConfig parentConfig,
            @NonNull Set<UseCaseConfig<?>> childrenConfigs) {
        List<Size> result = mergeChildrenResolutions(childrenConfigs);
        if (result == null) {
            // Use camera supported resolutions if there is no requirement from children config.
            result = cameraSupportedResolutions;
        }

        // Filter out resolutions that are not supported by the parent config (e.g. Extensions
        // may have additional limitations on resolutions).
        List<Pair<Integer, Size[]>> parentSupportedResolutions =
                parentConfig.retrieveOption(OPTION_SUPPORTED_RESOLUTIONS, null);
        if (parentSupportedResolutions != null) {
            result = filterOutUnsupportedResolutions(result, parentSupportedResolutions,
                    INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
        }

        return result;
    }

    @Nullable
    private static List<Size> mergeChildrenResolutions(
            @NonNull Set<UseCaseConfig<?>> childrenConfigs) {
        // TODO(b/264936115): This is a temporary placeholder solution that returns the config of
        //  VideoCapture if it exists. Later we will update it to actually merge the children's
        //  configs.
        for (UseCaseConfig<?> childConfig : childrenConfigs) {
            List<Size> customOrderedResolutions =
                    childConfig.retrieveOption(OPTION_CUSTOM_ORDERED_RESOLUTIONS, null);
            if (customOrderedResolutions != null) {
                return customOrderedResolutions;
            }
        }
        return null;
    }

    /**
     * Returns a list of resolution that all resolutions are supported.
     *
     * <p> The order of the {@code resolutionsToFilter} will be preserved in the resulting list.
     */
    @NonNull
    private static List<Size> filterOutUnsupportedResolutions(
            @NonNull List<Size> resolutionsToFilter,
            @NonNull List<Pair<Integer, Size[]>> supportedResolutions, int format) {
        // Get resolutions to keep.
        Set<Size> resolutionsToKeep = new HashSet<>();
        for (Pair<Integer, Size[]> pair : supportedResolutions) {
            if (pair.first.equals(format)) {
                resolutionsToKeep = new HashSet<>(Arrays.asList(pair.second));
                break;
            }
        }

        // Filter out unsupported resolutions.
        List<Size> result = new ArrayList<>();
        for (Size resolution : resolutionsToFilter) {
            if (resolutionsToKeep.contains(resolution)) {
                result.add(resolution);
            }
        }

        return result;
    }
}
