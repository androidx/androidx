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
 * - Bug Id: 532825627
 * - Description: Extension modes Bokeh and Face Retouch fail on Camera 0 for Samsung Galaxy A52s
 *   devices due to HAL requesting 0 max buffers.
 * - Device(s): Samsung Galaxy A52s (SM-A528B)
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO(b/270421716): enable when kotlin is supported.
public class ExtensionDisabledQuirk : Quirk {
    public companion object {
        @JvmStatic
        public fun load(): Boolean {
            return "Samsung".equals(Build.BRAND, ignoreCase = true) &&
                "SM-A528B".equals(Build.MODEL, ignoreCase = true)
        }
    }

    /** Returns true if the extension mode should be disabled for the given camera ID. */
    public fun shouldDisableExtension(
        cameraId: String,
        @ExtensionMode.Mode extensionMode: Int,
    ): Boolean {
        if (cameraId == "0") {
            return extensionMode == ExtensionMode.BOKEH ||
                extensionMode == ExtensionMode.FACE_RETOUCH
        }
        return false
    }
}
