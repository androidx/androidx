/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class Camera2Quirks @Inject constructor(
    private val metadataProvider: Camera2MetadataProvider,
) {
    /**
     * A quirk that waits for the last repeating capture request to start before stopping the
     * current capture session. This is an issue in the Android camera framework where recreating
     * a capture session too quickly can cause it to deadlock itself (stuck in its idle state),
     * preventing us from successfully recreating a capture session.
     *
     * - Bug(s): b/146773463, b/267557892
     * - Device(s): Camera devices on hardware level LEGACY
     * - API levels: All
     */
    internal fun shouldWaitForRepeatingRequest(graphConfig: CameraGraph.Config): Boolean {
        // First, check for overrides.
        graphConfig.flags.quirkWaitForRepeatingRequestOnDisconnect?.let { return it }

        // Then we verify whether we need this quirk based on hardware level.
        val level = metadataProvider.awaitCameraMetadata(graphConfig.camera)[
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
        return level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    }

    /**
     * A quirk that creates a blank capture session before closing the camera. This is an issue in
     * the Android camera framework where it doesn't disconnect the current Surfaces when the camera
     * device is closed. For this reason, we create a blank capture session, and during which, the
     * camera framework would disconnect the Surfaces. Another key thing to note is we also need to
     * wait for the capture session to be configured, since the Surface disconnect calls are done
     * almost at the very end of session configuration.
     *
     * - Bug(s): b/128600230, b/267559562
     * - Device(s): Camera devices on hardware level LEGACY
     * - API levels: 24 (N) – 28 (P)
     */
    internal fun shouldCreateCaptureSessionBeforeClosing(cameraId: CameraId): Boolean {
        if (Build.VERSION.SDK_INT !in (Build.VERSION_CODES.N..Build.VERSION_CODES.P)) {
            return false
        }
        val level = metadataProvider.awaitCameraMetadata(cameraId)[
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
        return level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    }

    /**
     * A quirk that waits for [android.hardware.camera2.CameraDevice.StateCallback.onClosed] to
     * come back before finalizing the current session during camera close. This is needed because
     * on legacy camera devices, releasing a Surface while camera frames are still being produced
     * would trigger crashes.
     *
     * - Bug(s): b/130759707
     * - Device(s): Camera devices on hardware level LEGACY
     * - API levels: All
     */
    internal fun shouldWaitForCameraDeviceOnClosed(cameraId: CameraId): Boolean {
        val level = metadataProvider.awaitCameraMetadata(cameraId)[
            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
        return level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
    }
}