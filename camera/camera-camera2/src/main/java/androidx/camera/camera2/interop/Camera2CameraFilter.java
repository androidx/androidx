/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.interop;

import android.hardware.camera2.CameraCharacteristics;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraFilter;
import androidx.camera.core.CameraInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides ability to filter cameras with camera IDs and characteristics and create the
 * corresponding {@link CameraFilter}.
 */
@ExperimentalCamera2Interop
public final class Camera2CameraFilter {

    /**
     * Creates a {@link CameraFilter} from a {@link Camera2Filter}.
     */
    @NonNull
    public static CameraFilter createCameraFilter(@NonNull Camera2Filter filter) {
        return cameraInfos -> {
            List<Camera2CameraInfo> input = new ArrayList<>();
            for (CameraInfo cameraInfo : cameraInfos) {
                input.add(Camera2CameraInfo.from(cameraInfo));
            }

            List<Camera2CameraInfo> result = filter.filter(Collections.unmodifiableList(input));

            List<CameraInfo> output = new ArrayList<>();
            for (CameraInfo cameraInfo : cameraInfos) {
                if (result.contains(Camera2CameraInfo.from(cameraInfo))) {
                    output.add(cameraInfo);
                }
            }

            return output;
        };
    }

    /**
     * An interface that filters cameras based on camera IDs and characteristics. Applications
     * can implement the filter method for custom camera selection.
     */
    public interface Camera2Filter {
        /**
         * Filters a list of {@link Camera2CameraInfo} then returns those matching the requirements.
         *
         * <p>If the output list contains Camera2CameraInfo not in the input list, when used by a
         * {@link androidx.camera.core.CameraSelector} then it will result in an
         * IllegalArgumentException thrown when calling bindToLifecycle.
         *
         * <p>The Camera2CameraInfo that has lower index in the map has higher priority. When
         * used by
         * {@link androidx.camera.core.CameraSelector.Builder#addCameraFilter(CameraFilter)}, the
         * available cameras will be filtered by the {@link Camera2Filter} and all other
         * {@link CameraFilter}s by the order they were added. The first camera in the result
         * will be selected if there are multiple cameras left.
         *
         * @param cameraInfos An unmodifiable list of {@link Camera2CameraInfo}s being filtered.
         * @return The output map of camera IDs and their {@link CameraCharacteristics} that
         * match the requirements. Users are expected to create a new map to return with.
         */
        @NonNull
        List<Camera2CameraInfo> filter(@NonNull List<Camera2CameraInfo> cameraInfos);
    }

    // Should not be instantiated.
    private Camera2CameraFilter() {}
}
