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

package androidx.benchmark

import androidx.annotation.RestrictTo
import org.jetbrains.annotations.VisibleForTesting

/** Provides information about Android Studio Device mirroring */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object DeviceMirroring {
    /**
     * Whether Android Studio Device Mirroring is active by querying `dumpsys SurfaceFlinger`.
     * Device Mirroring presents to an additional (virtual) display, which can impact performance.
     *
     * This check is fairly expensive and should be performed only when needed.
     *
     * @throws IllegalStateException If the status cannot be determined.
     */
    fun isAndroidStudioDeviceMirroringActive(): Boolean {
        if (isAndroidStudioDeviceMirroringActiveOverride != null) {
            return isAndroidStudioDeviceMirroringActiveOverride!!
        }
        val dumpsysOutput = Shell.executeScriptCaptureStdout("dumpsys SurfaceFlinger --displays")
        return hasDisplayForStudioDeviceMirroringInSurfaceFlingerDump(dumpsysOutput)
    }

    /**
     * Whether a `dumpsys SurfaceFlinger` output has a registered display for Studio Device
     * Mirroring.
     */
    fun hasDisplayForStudioDeviceMirroringInSurfaceFlingerDump(dumpsysOutput: String): Boolean =
        dumpsysOutput.contains("studio.screen.sharing")

    object Error {
        const val ID = "DEVICE-MIRRORING"
        const val SUMMARY = "Android Studio Device Mirroring is active"
        const val MESSAGE =
            """
            This device's screen is being mirrored in Android Studio. Device mirroring can
            impact performance since the device has to send frames to an extra display.
            End the device mirroring session temporarily in the "Running Devices" tab in
            Android Studio, or disable device mirroring in Android Studio's settings.
        """
    }

    @get:VisibleForTesting
    @set:VisibleForTesting
    var isAndroidStudioDeviceMirroringActiveOverride: Boolean? = null
}
