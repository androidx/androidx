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

package androidx.camera.camera2.pipe.integration.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.camera2.pipe.integration.compat.quirk.Device.isInfinixDevice
import androidx.camera.core.impl.Quirk
import java.util.Locale

/**
 * Quirk needed on devices where not closing capture session before creating a new capture session
 * can lead to undesirable behaviors:
 * - CameraDevice.close() call might stall indefinitely
 * - Crashes in the camera HAL
 *
 * QuirkSummary
 * - Bug Id: 277675483, 282871038
 * - Description: Instructs CameraPipe to close the capture session before creating a new one to
 *   avoid undesirable behaviors
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class CloseCaptureSessionOnDisconnectQuirk : Quirk {

    public companion object {
        private val androidTOrNewerSm8150Devices =
            mapOf(
                "google" to setOf("pixel 4", "pixel 4 xl"),
                "samsung" to setOf("sm-g770f"),
            )

        @JvmStatic
        public fun isEnabled(): Boolean {
            if (CameraQuirks.isImmediateSurfaceReleaseAllowed()) {
                // If we can release Surfaces immediately, we'll finalize the session when the
                // camera graph is closed (through FinalizeSessionOnCloseQuirk), and thus we won't
                // need to explicitly close the capture session.
                return false
            }
            return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1) {
                // TODO(b/277675483): Older devices (Android version <= 8.1.0) seem to have a higher
                //  chance of encountering an issue where not closing the capture session would lead
                //  to CameraDevice.close() stalling indefinitely. This version check might need to
                //  be further fine-turned down the line.
                true
            } else if (Build.HARDWARE == "samsungexynos7870") {
                // TODO(b/282871038): On some platforms, not closing the capture session before
                //  switching to a new capture session may trigger camera HAL crashes. Add more
                //  hardware platforms here when they're identified.
                true
            } else if (Build.MODEL.lowercase(Locale.getDefault()).startsWith("cph")) {
                // For CPH devices, the shutdown sequence oftentimes triggers ANR for the test app.
                // As a result, we need to close the capture session to stop the captures, then
                // release the Surfaces by FinalizeSessionOnCloseQuirk.
                true
            } else if (
                (Build.HARDWARE.equals("qcom", ignoreCase = true) &&
                    Build.VERSION.SDK_INT <= Build.VERSION_CODES.S) ||
                    androidTOrNewerSm8150Devices[Build.BRAND.lowercase(Locale.getDefault())]
                        ?.contains(Build.MODEL.lowercase(Locale.getDefault())) == true
            ) {
                // On qcom platforms from a certain era, switching capture sessions without closing
                // the prior session then setting the repeating request immediately, puts the camera
                // HAL in a bad state where it only produces a few frames before going into an
                // unrecoverable error. See b/316048171 for context.
                true
            } else {
                // For Infinix devices, there is a service that actively kills apps that use
                // significant memory, including the _foreground_ test applications. Closing the
                // capture session ensures that we finalize every CameraGraph session, slightly
                // lowering the peak memory.
                isInfinixDevice()
            }
        }
    }
}
