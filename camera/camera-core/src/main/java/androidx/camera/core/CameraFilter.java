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

package androidx.camera.core;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * An interface for filtering cameras.
 */
@ExperimentalCameraFilter
public interface CameraFilter {
    /**
     * Filters a list of {@link CameraInfo}s and returns those matching the requirements.
     *
     * <p>If the output list contains CameraInfos not in the input list, when used by a
     * {@link androidx.camera.core.CameraSelector} then it will result in an
     * IllegalArgumentException thrown when calling bindToLifecycle.
     *
     * <p>The CameraInfo that has lower index in the list has higher priority. When used by
     * {@link androidx.camera.core.CameraSelector.Builder#addCameraFilter(CameraFilter)}, the
     * available cameras will be filtered by all {@link CameraFilter}s by the order they were
     * added. The first camera in the result will be selected if there are multiple cameras left.
     *
     * @param cameraInfos An unmodifiable list of {@link CameraInfo}s being filtered.
     * @return The output list of {@link CameraInfo}s that match the requirements. Users are
     * expected to create a new list to return with.
     */
    @NonNull
    List<CameraInfo> filter(@NonNull List<CameraInfo> cameraInfos);
}
