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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.camera.core.impl.CameraConfig;
import androidx.camera.core.impl.ExtendedCameraConfigProviderStore;
import androidx.camera.core.impl.Identifier;

import java.util.List;

/**
 * An interface for filtering cameras.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface CameraFilter {
    /**
     * Default identifier of camera filter.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    Identifier DEFAULT_ID = Identifier.create(new Object());

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

    /**
     * Returns the id of this camera filter.
     *
     * <p>A camera filter can be associated with a set of camera configuration options. This
     * means a camera filter's {@link Identifier} can be mapped to a unique {@link CameraConfig}.
     * An example of this is extension modes, where a camera filter can represent an extension
     * mode, and each extension mode adds a set of camera configurations to the camera that
     * supports it. {@link ExtendedCameraConfigProviderStore#getConfigProvider(Object)} allows
     * retrieving the {@link CameraConfig} of an extension mode given the {@link Identifier} of
     * its camera filter.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    default Identifier getIdentifier() {
        return DEFAULT_ID;
    }
}
