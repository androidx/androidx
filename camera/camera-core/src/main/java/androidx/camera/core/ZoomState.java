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

import androidx.annotation.RequiresApi;

/**
 * An interface which contains the zoom related information from a camera.
 *
 * <p>Applications can retrieve an instance via {@link CameraInfo#getZoomState()}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface ZoomState {
    /**
     * Returns the zoom ratio. The value is 1.0 by default.
     */
    float getZoomRatio();

    /** Returns the maximum zoom ratio. */
    float getMaxZoomRatio();

    /**
     * Returns the minimum zoom ratio.
     *
     * <p>Typically 1.0, but can be less than 1.0 if the camera device supports zoom-out (only on
     * android 11 or later).
     */
    float getMinZoomRatio();

    /**
     * Returns the linearZoom which is in range [0..1].
     *
     * <p>LinearZoom 0 represents the minimum zoom while linearZoom 1.0 represents the maximum zoom.
     */
    float getLinearZoom();
}
