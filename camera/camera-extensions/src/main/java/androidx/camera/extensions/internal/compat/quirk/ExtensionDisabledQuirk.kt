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

package androidx.camera.extensions.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk
import androidx.camera.extensions.ExtensionMode

/**
 * QuirkSummary
 * - Bug Id: 532825627, 579700310
 * - Description: Some extension modes fail on specific cameras of the following devices:
 *     - Samsung Galaxy A52s (SM-A528B): Bokeh and Face Retouch fail on camera 0 because the HAL
 *       requests 0 max buffers when configuring streams.
 *     - Samsung Galaxy S22 (SM-S901U) and S22 Ultra (SM-S908U1): All extension modes fail on
 *       camera 3. Preview works, but the HAL rejects still capture requests with "Function not
 *       implemented (-38)" and the camera device encounters a fatal error.
 *     - Samsung Galaxy Z Fold5 (SM-F946U1): Auto fails on camera 1. The HAL intermittently rejects
 *       still capture requests with "Broken pipe (-32)" and the camera device encounters a fatal
 *       error.
 * - Device(s): Samsung Galaxy A52s (SM-A528B), Samsung Galaxy S22 (SM-S901U), Samsung Galaxy S22
 *   Ultra (SM-S908U1), Samsung Galaxy Z Fold5 (SM-F946U1)
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO(b/270421716): enable when kotlin is supported.
public class ExtensionDisabledQuirk : Quirk {
    public companion object {
        private const val ALL_EXTENSION_MODES = -1

        /** Maps a device model to the disabled extension modes of each camera ID. */
        private val DISABLED_EXTENSIONS: Map<String, Map<String, Set<Int>>> =
            mapOf(
                "SM-A528B" to mapOf("0" to setOf(ExtensionMode.BOKEH, ExtensionMode.FACE_RETOUCH)),
                "SM-S901U" to mapOf("3" to setOf(ALL_EXTENSION_MODES)),
                "SM-S908U1" to mapOf("3" to setOf(ALL_EXTENSION_MODES)),
                "SM-F946U1" to mapOf("1" to setOf(ExtensionMode.AUTO)),
            )

        @JvmStatic
        public fun load(): Boolean {
            return "Samsung".equals(Build.BRAND, ignoreCase = true) &&
                getDisabledExtensions() != null
        }

        private fun getDisabledExtensions(): Map<String, Set<Int>>? =
            DISABLED_EXTENSIONS.entries
                .firstOrNull { it.key.equals(Build.MODEL, ignoreCase = true) }
                ?.value
    }

    /** The disabled extension modes of each camera ID on the current device. */
    private val disabledExtensions: Map<String, Set<Int>> = getDisabledExtensions().orEmpty()

    /**
     * Returns true if the extension mode should be disabled for the given camera ID.
     *
     * [ExtensionMode.NONE] is never disabled.
     */
    public fun shouldDisableExtension(
        cameraId: String,
        extensionMode: Int,
    ): Boolean {
        if (extensionMode == ExtensionMode.NONE) {
            return false
        }
        val disabledModes = disabledExtensions[cameraId] ?: return false
        return disabledModes.contains(ALL_EXTENSION_MODES) || disabledModes.contains(extensionMode)
    }
}
