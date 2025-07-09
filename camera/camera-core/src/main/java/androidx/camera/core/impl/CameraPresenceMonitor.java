/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.impl;

import androidx.camera.core.InitializationException;

import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * An interface for components, like a {@link CameraFactory}, that need to be notified of the
 * complete set of raw camera IDs from the hardware.
 */
public interface CameraPresenceMonitor {
    /**
     * Called when the list of available camera IDs has been updated.
     *
     * @param cameraIds The complete, ordered list of camera IDs currently reported by the
     * hardware. The order may be significant.
     */
    void onCameraIdsUpdated(@NonNull List<String> cameraIds) throws InitializationException;
}
