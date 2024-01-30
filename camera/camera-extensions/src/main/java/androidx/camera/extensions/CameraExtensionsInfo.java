/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.camera.extensions;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.camera.core.CameraInfo;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * A camera extensions info instance that allows to observe or monitor capture request settings
 * and results for supported camera extensions.
 *
 * <p>Applications can leverage the {@link ExtensionsManager#getCameraExtensionsInfo(CameraInfo)}
 * method to acquire a CameraExtensionsInfo object for observing extension-specific settings and
 * results.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface CameraExtensionsInfo {

    /**
     * Returns whether extension strength is supported for the extensions enabled camera
     * associated with the CameraExtensionsInfo.
     *
     * <p>When extension strength is supported, applications can change the strength setting via
     * {@link CameraExtensionsControl#setExtensionStrength(int)} and observe the strength value
     * changes via the {@link LiveData} object returned by {@link #getExtensionStrength()}.
     *
     * @return {@code true} if extension strength is supported. Otherwise, returns {@code false}.
     */
    default boolean isExtensionStrengthAvailable() {
        return false;
    }

    /**
     * Returns a {@link LiveData} which is allowed to observe the extension strength changes for
     * the extensions enabled camera associated with the CameraExtensionsInfo.
     *
     * <p>The extension strength will range from 0 to 100. The value depends on the following
     * conditions:
     * <ul>
     *     <li>Extension mode is enabled and {@link #isExtensionStrengthAvailable()} returns
     *     {@code true}: The strength value will dynamically change based on the latest
     *     adjustments made within the current extension mode.
     *     <li>Extension mode is enabled but {@link #isExtensionStrengthAvailable()} returns {@code
     *     false}: The strength value will default to its maximum setting of 100.
     *     <li>No extension mode is enabled: The strength value will be set to its minimum of 0.
     * </ul>
     *
     * @return a {@link LiveData} of {@link Integer} type to observe the extension strength changes.
     */
    @NonNull
    default LiveData<Integer> getExtensionStrength() {
        return new MutableLiveData<>(100);
    }

    /**
     * Returns a {@link LiveData} which is allowed to observe the extension type changes for
     * the extensions enabled camera associated with the CameraExtensionsInfo.
     *
     * <p>The initial value will be equal to the extension type the session was started with. The
     * current extension type may change over time. For example, when the extension mode is
     * {@link ExtensionMode#AUTO}, the current extension type may change to the
     * {@link ExtensionMode#NIGHT} or {@link ExtensionMode#HDR} processor depending on the
     * current lighting conditions or environment.
     *
     * @return a {@link LiveData} of {@link Integer} type to observe the extension type changes.
     */
    @NonNull
    default LiveData<Integer> getCurrentExtensionType() {
        return new MutableLiveData<>(ExtensionMode.NONE);
    }
}
