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
import androidx.camera.camera2.pipe.CameraGraph.RepeatingRequestRequirementsBeforeCapture.CompletionBehavior.AT_LEAST
import androidx.camera.camera2.pipe.CameraGraph.RepeatingRequestRequirementsBeforeCapture.CompletionBehavior.EXACT
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.compat.Camera2Quirks.Companion.SHOULD_WAIT_FOR_REPEATING_DEVICE_MAP
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
internal class Camera2Quirks
@Inject
constructor(
    private val metadataProvider: Camera2MetadataProvider,
    private val cameraPipeFlags: CameraPipe.Flags,
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
    internal fun shouldWaitForRepeatingRequestStartOnDisconnect(
        graphConfig: CameraGraph.Config
    ): Boolean {
        val isStrictModeOn = cameraPipeFlags.strictModeEnabled

        // First, check for overrides.
        graphConfig.flags.awaitRepeatingRequestOnDisconnect?.let {
            return !isStrictModeOn && it
        }

        // Then we verify whether we need this quirk based on hardware level.
        return !isStrictModeOn &&
            metadataProvider.awaitCameraMetadata(graphConfig.camera).isHardwareLevelLegacy
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
    internal fun shouldCreateEmptyCaptureSessionBeforeClosing(cameraId: CameraId): Boolean {
        return Build.VERSION.SDK_INT in (Build.VERSION_CODES.N..Build.VERSION_CODES.P) &&
            !cameraPipeFlags.strictModeEnabled &&
            metadataProvider.awaitCameraMetadata(cameraId).isHardwareLevelLegacy
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
        !cameraPipeFlags.strictModeEnabled &&
            metadataProvider.awaitCameraMetadata(cameraId).isHardwareLevelLegacy

    /**
     * A quirk that closes the camera devices before creating a new capture session. This is needed
     * on certain devices where creating a capture session directly may lead to deadlocks, NPEs or
     * other undesirable behaviors. When [shouldCreateEmptyCaptureSessionBeforeClosing] is also
     * required, a regular camera device closure would then be expanded to:
     * 1. Close the camera device.
     * 2. Open the camera device.
     * 3. Create an empty capture session.
     * 4. Close the capture session.
     * 5. Close the camera device.
     * - Bug(s): b/237341513, b/359062845, b/342263275, b/379347826, b/359062845
     * - Device(s): Camera devices on hardware level LEGACY
     * - API levels: 23 (M) – 31 (S_V2)
     */
    internal fun shouldCloseCameraBeforeCreatingCaptureSession(cameraId: CameraId): Boolean {
        val isStrictModeEnabled = cameraPipeFlags.strictModeEnabled
        val isLegacyDevice =
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
                metadataProvider.awaitCameraMetadata(cameraId).isHardwareLevelLegacy
        val isQuirkyDevice =
            "motorola".equals(Build.BRAND, ignoreCase = true) &&
                "moto e20".equals(Build.MODEL, ignoreCase = true) &&
                cameraId.value == "1"
        return !isStrictModeEnabled && (isLegacyDevice || isQuirkyDevice)
    }

    /**
     * Returns the number of repeating requests frames before capture for quirks.
     *
     * This kind of quirk behavior requires waiting for a certain number of repeating requests to
     * complete before allowing (single) capture requests to be issued. This is needed on some
     * devices where issuing a capture request too early might cause it to fail prematurely or cause
     * some other problem. A value of zero is returned when not required.
     * - Bug(s): b/287020251, b/289284907
     * - Device(s): See [SHOULD_WAIT_FOR_REPEATING_DEVICE_MAP]
     * - API levels: Before 34 (U)
     */
    internal fun getRepeatingRequestFrameCountForCapture(graphConfigFlags: CameraGraph.Flags): Int {
        if (cameraPipeFlags.strictModeEnabled) {
            return 0
        }

        val requirements = graphConfigFlags.awaitRepeatingRequestBeforeCapture

        var frameCount = 0

        if (
            SHOULD_WAIT_FOR_REPEATING_DEVICE_MAP[Build.MANUFACTURER]?.contains(Build.DEVICE) ==
                true && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            frameCount = max(frameCount, 10)
        }

        frameCount =
            when (requirements.completionBehavior) {
                AT_LEAST -> max(frameCount, requirements.repeatingFramesToComplete.toInt())
                EXACT -> requirements.repeatingFramesToComplete.toInt()
            }

        return frameCount
    }

    companion object {
        private val SHOULD_WAIT_FOR_REPEATING_DEVICE_MAP =
            mapOf("Google" to setOf("oriole", "raven", "bluejay", "panther", "cheetah", "lynx"))
    }
}
