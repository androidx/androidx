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

import androidx.annotation.Nullable;
import androidx.camera.core.CameraInfo;
import androidx.lifecycle.LiveData;

/**
 * A camera extensions info instance that allows to observe or monitor capture request settings
 * and results for supported camera extensions.
 *
 * <p>Applications can leverage the {@link ExtensionsManager#getCameraExtensionsInfo(CameraInfo)}
 * method to acquire a CameraExtensionsInfo object for observing extension-specific settings and
 * results.
 */
public interface CameraExtensionsInfo {

    /**
     * Returns whether extension strength is supported for the extensions-enabled camera that is
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
     * Returns a {@link LiveData} which observes the extension strength changes for the
     * extensions-enabled camera that is associated with the CameraExtensionsInfo.
     *
     * <p>This is only available when {@link #isExtensionStrengthAvailable()} returns {@code true
     * }. When this is supported, the extension strength value will range from 0 to 100 and will
     * dynamically change based on the latest adjustments made within the current extension mode.
     *
     * @return a {@link LiveData} of {@link Integer} type to observe the extension strength
     * changes when {@link #isExtensionStrengthAvailable()} returns {@code true}. Otherwise,
     * returns {@code null}.
     */
    @Nullable
    default LiveData<Integer> getExtensionStrength() {
        return null;
    }

    /**
     * Returns whether reporting the currently active extension mode is supported for the
     * extensions-enabled camera that is associated with the CameraExtensionsInfo.
     *
     * <p>When current extension mode is supported, applications can observe the current extension
     * value changes via the {@link LiveData} object returned by {@link #getCurrentExtensionMode()}.
     *
     * @return {@code true} if current extension mode is supported. Otherwise, returns {@code
     * false}.
     */
    default boolean isCurrentExtensionModeAvailable() {
        return false;
    }

    /**
     * Returns a {@link LiveData} which observes the extension mode changes for the
     * extensions-enabled camera that is associated with the CameraExtensionsInfo.
     *
     * <p>This is only available when {@link #isCurrentExtensionModeAvailable()} returns {@code
     * true}. When this is supported, the initial value will be equal to the extension mode the
     * session was started with. Then, the current extension mode may change over time. For
     * example, when the extension mode is {@link ExtensionMode#AUTO}, the current extension mode
     * may change to the {@link ExtensionMode#NIGHT} or {@link ExtensionMode#HDR} depending on
     * the current lighting conditions or environment.
     *
     * @return a {@link LiveData} of {@link Integer} type to observe the extension mode changes
     * when {@link #isCurrentExtensionModeAvailable()} returns {@code true}. Otherwise, returns
     * {@code null}.
     */
    @Nullable
    default LiveData<Integer> getCurrentExtensionMode() {
        return null;
    }
}
