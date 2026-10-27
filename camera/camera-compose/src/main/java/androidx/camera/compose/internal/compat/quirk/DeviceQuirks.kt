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

import androidx.camera.core.Logger
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.QuirkSettingsHolder
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor

/**
 * Provider of device specific quirks for the camera-compose module, which are used for device
 * specific workarounds.
 *
 * Device specific quirks depend on device properties, including the manufacturer
 * ([android.os.Build.MANUFACTURER]), model ([android.os.Build.MODEL]) and OS level
 * ([android.os.Build.VERSION.SDK_INT]).
 *
 * Device specific quirks are loaded the first time they're needed and reloaded whenever
 * [androidx.camera.core.impl.QuirkSettings] changes.
 */
internal object DeviceQuirks {
    private const val TAG = "DeviceQuirks"

    @Volatile private lateinit var quirks: Quirks

    init {
        // Direct executor will initialize quirks immediately, guaranteeing it's initialized.
        QuirkSettingsHolder.instance().observe(directExecutor()) { quirkSettings ->
            quirks = Quirks(DeviceQuirksLoader.loadQuirks(quirkSettings))
            Logger.d(TAG, "compose DeviceQuirks = ${Quirks.toString(quirks)}")
        }
    }

    /**
     * Retrieves a specific device [Quirk] instance given its type.
     *
     * @param quirkClass The type of device quirk to retrieve.
     * @return A device [Quirk] instance of the provided type, or `null` if it isn't found.
     */
    fun <T : Quirk> get(quirkClass: Class<T>): T? {
        return quirks.get(quirkClass)
    }
}
