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

package androidx.camera.compose.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.SessionProcessor

/**
 * A quirk where [android.view.SurfaceView] (and [android.view.SurfaceControl] backed by
 * [android.view.SurfaceView]) is not supported in a Camera2
 * [android.hardware.camera2.CameraExtensionSession] on Android 13 (API 33) and below.
 *
 * QuirkSummary
 * - Bug Id: b/565668575
 * - Description: In Android 13 (API 33) and below,
 *   `CameraExtensionUtils.getRepeatingRequestSurface()` only accepts a repeating request surface if
 *   its detected surface format is `ImageFormat.PRIVATE` (34) or `PixelFormat.RGBA_8888` (1, the
 *   default format of a `SurfaceTexture`, which is used by `TextureView`). Because `SurfaceView`
 *   (and `SurfaceControl` created via `SurfaceControl.Builder`) initializes its surface with
 *   `PixelFormat.RGB_565` (4) or `PixelFormat.RGBX_8888` (2) by default before the camera connects,
 *   `CameraExtensionUtils.getRepeatingRequestSurface()` returns `null` and
 *   `CameraExtensionSessionImpl.createCameraExtensionSession()` throws `IllegalArgumentException:
 *   One or more unsupported output surfaces found!`. In Android 14 (API 34), the framework fixed
 *   this by checking `HardwareBuffer.USAGE_COMPOSER_OVERLAY` in `getRepeatingRequestSurface()`.
 * - Device(s): All devices running Android 13 (API 33) and below when Camera2 Extensions is
 *   enabled. Note that CameraX only enables Camera2 Extensions on API 33 and above, so in practice
 *   only API 33 devices are affected.
 *
 * Keep in sync with the same quirk in `androidx.camera.view.internal.compat.quirk`.
 */
// TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
@SuppressLint("CameraXQuirksClassDetector")
internal class SurfaceViewNotSupportedInExtensionSessionQuirk : Quirk {
    /**
     * Returns `true` if [android.view.SurfaceView] is not supported for the given [CameraConfig],
     * i.e. when the configured [SessionProcessor] creates a Camera2
     * [android.hardware.camera2.CameraExtensionSession]
     * ([SessionProcessor.TYPE_CAMERA2_EXTENSION]).
     */
    fun isSurfaceViewNotSupported(cameraConfig: CameraConfig): Boolean {
        return cameraConfig.getSessionProcessor(null)?.implementationType?.first ==
            SessionProcessor.TYPE_CAMERA2_EXTENSION
    }

    companion object {
        @JvmStatic
        fun load(): Boolean {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU
        }
    }
}
