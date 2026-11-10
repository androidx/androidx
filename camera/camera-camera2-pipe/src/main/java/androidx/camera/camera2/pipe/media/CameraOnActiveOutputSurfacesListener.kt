/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.pipe.media

import android.view.Surface
import androidx.annotation.RestrictTo

/**
 * Receives active output [Surface]s for a multi-resolution capture frame.
 *
 * Mirrors [android.hardware.camera2.MultiResolutionImageReader.OnActiveOutputSurfacesListener]
 * without requiring API 37 at class load time.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface CameraOnActiveOutputSurfacesListener {
    /**
     * Reports the active output [Surface]s for a capture frame.
     *
     * @param activeOutputSurfaces active [Surface]s receiving images for this frame
     * @param timestamp capture timestamp in nanoseconds (see [android.media.Image.getTimestamp])
     * @param frameNumber capture frame number (see
     *   [android.hardware.camera2.CaptureResult.getFrameNumber])
     */
    public fun onActiveOutputSurfaces(
        activeOutputSurfaces: List<Surface>,
        timestamp: Long,
        frameNumber: Long,
    )
}
