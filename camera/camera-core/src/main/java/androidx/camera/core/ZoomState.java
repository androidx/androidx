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

import androidx.annotation.RestrictTo;

/**
 * An interface which contains the zoom related information from a camera.
 *
 * <p>Applications can retrieve an instance via {@link CameraInfo#getZoomState()}.
 */
public interface ZoomState {
    /** Returns the zoom ratio. The value is 1.0 by default. */
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

    /**
     * Returns the intrinsic zoom ratio of the active physical lens.
     *
     * <p>On logical multi-camera devices, the hardware may dynamically transition between physical
     * lenses as the zoom ratio changes via {@link CameraControl#setZoomRatio(float)}. This value
     * reflects the {@link CameraInfo#getIntrinsicZoomRatio() intrinsic zoom ratio} of the physical
     * lens currently capturing frames. Note that requesting a specific zoom ratio does not
     * guarantee that the camera will switch to a particular physical lens (see
     * <a href="https://developer.android.com/media/camera/camera2/multi-camera">Multi-camera API</a>).
     *
     * <p>Applications can observe {@link CameraInfo#getZoomState()} to update UI indicators when
     * the active lens transitions:
     *
     * <pre>{@code
     * cameraInfo.getZoomState().observe(lifecycleOwner, zoomState -> {
     *     float activeRatio = zoomState.getActiveIntrinsicZoomRatio();
     *     if (activeRatio < 1.0f) {
     *         updateZoomIndicator("0.5x");
     *     } else if (activeRatio > 1.0f) {
     *         updateZoomIndicator("3x");
     *     } else {
     *         updateZoomIndicator("1x");
     *     }
     * });
     * }</pre>
     *
     * <p>If the camera is not a logical multi-camera, or on API levels lower than {@link
     * android.os.Build.VERSION_CODES#Q}, this value is always {@code 1.0}.
     */
    // TODO: b/317468002 - Make this public in next alpha
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    default float getActiveIntrinsicZoomRatio() {
        return 1.0f;
    }
}
