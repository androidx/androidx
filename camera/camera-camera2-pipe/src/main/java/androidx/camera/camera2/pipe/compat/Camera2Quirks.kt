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

import android.os.Build
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Camera2Quirks
@Inject
constructor(
    private val metadataProvider: Camera2MetadataProvider,
) {
    /**
     * A quirk that waits for the last repeating capture request to start before stopping the
     * current capture session. This is an issue in the Android camera framework where recreating a
     * capture session too quickly can cause it to deadlock itself (stuck in its idle state),
     * preventing us from successfully recreating a capture session.
     * - Bug(s): b/146773463, b/267557892
     * - Device(s): Camera devices on hardware level LEGACY
     * - API levels: All
     */
    internal fun shouldWaitForRepeatingRequest(graphConfig: CameraGraph.Config): Boolean {
        // First, check for overrides.
        graphConfig.flags.quirkWaitForRepeatingRequestOnDisconnect?.let {
            return it
        }

        // Then we verify whether we need this quirk based on hardware level.
        return metadataProvider.awaitCameraMetadata(graphConfig.camera).isHardwareLevelLegacy
    }

    /**
     * A quirk that creates a blank capture session before closing the camera. This is an issue in
     * the Android camera framework where it doesn't disconnect the current Surfaces when the camera
     * device is closed. For this reason, we create a blank capture session, and during which, the
     * camera framework would disconnect the Surfaces. Another key thing to note is we also need to
     * wait for the capture session to be configured, since the Surface disconnect calls are done
     * almost at the very end of session configuration.
     * - Bug(s): b/128600230, b/267559562
     * - Device(s): Camera devices on hardware level LEGACY
     * - API levels: 24 (N) – 28 (P)
     */
    internal fun shouldCreateCaptureSessionBeforeClosing(cameraId: CameraId): Boolean {
        if (Build.VERSION.SDK_INT !in (Build.VERSION_CODES.N..Build.VERSION_CODES.P)) {
            return false
        }
        return metadataProvider.awaitCameraMetadata(cameraId).isHardwareLevelLegacy
    }

    /**
     * A quirk that waits for [android.hardware.camera2.CameraDevice.StateCallback.onClosed] to come
     * back before finalizing the current session during camera close. This is needed because on
     * legacy camera devices, releasing a Surface while camera frames are still being produced would
     * trigger crashes.
     * - Bug(s): b/130759707
     * - Device(s): Camera devices on hardware level LEGACY
     * - API levels: All
     */
    internal fun shouldWaitForCameraDeviceOnClosed(cameraId: CameraId): Boolean =
        metadataProvider.awaitCameraMetadata(cameraId).isHardwareLevelLegacy

    companion object {
        private val SHOULD_WAIT_FOR_REPEATING_DEVICE_MAP =
            mapOf(
                "Google" to setOf("oriole", "raven", "bluejay", "panther", "cheetah", "lynx"),
            )

        /**
         * A quirk that waits for a certain number of repeating requests to complete before allowing
         * (single) capture requests to be issued. This is needed on some devices where issuing a
         * capture request too early might cause it to fail prematurely.
         * - Bug(s): b/287020251, b/289284907
         * - Device(s): See [SHOULD_WAIT_FOR_REPEATING_DEVICE_MAP]
         * - API levels: Before 34 (U)
         */
        internal fun shouldWaitForRepeatingBeforeCapture(): Boolean {
            return SHOULD_WAIT_FOR_REPEATING_DEVICE_MAP[Build.MANUFACTURER]?.contains(
                Build.DEVICE
            ) == true && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }

        /**
         * A quirk that calls CameraExtensionCharacteristics before opening an Extension session.
         * This is an issue in the Android camera framework where Camera2 has a global variable
         * recording if advanced extensions are supported or not, and the variable is updated the
         * first time CameraExtensionCharacteristics are queried. If CameraExtensionCharacteristics
         * are not queried and therefore the variable is not set, Camera2 will fall back to basic
         * extensions, even if they are not supported, causing the session creation to fail.
         * - Bug(s): b/293473614
         * - Device(s): All devices that support advanced extensions
         * - API levels: Before 34 (U)
         */
        internal fun shouldGetExtensionCharacteristicsBeforeSession(): Boolean {
            return Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }
    }
}
