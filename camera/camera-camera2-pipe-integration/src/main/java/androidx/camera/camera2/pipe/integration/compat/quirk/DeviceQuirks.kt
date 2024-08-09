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

import androidx.camera.core.Logger
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.QuirkSettingsHolder
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor

/**
 * Provider of device specific quirks, which are used for device specific workarounds.
 *
 * Device specific quirks depend on device properties, including the manufacturer
 * ([android.os.Build.MANUFACTURER]), model ([android.os.Build.MODEL]) and OS level
 * ([android.os.Build.VERSION.SDK_INT]).
 *
 * Device specific quirks are lazily loaded, i.e. They are loaded the first time they're needed.
 */
public object DeviceQuirks {
    private const val TAG = "DeviceQuirks"

    /** Returns all device specific quirks loaded on the current device. */
    @Volatile public lateinit var all: Quirks

    init {
        // Direct executor will initialize quirks immediately, guaranteeing it's never null.
        QuirkSettingsHolder.instance().observe(directExecutor()) { quirkSettings ->
            all = Quirks(DeviceQuirksLoader.loadQuirks(quirkSettings))
            Logger.d(TAG, "camera2-pipe-integration DeviceQuirks = " + Quirks.toString(all))
        }
    }

    /**
     * Retrieves a specific device [Quirk] instance given its type.
     *
     * @param quirkClass The type of device quirk to retrieve.
     * @return A device [Quirk] instance of the provided type, or `null` if it isn't found.
     */
    public operator fun <T : Quirk?> get(quirkClass: Class<T>): T? {
        return all.get(quirkClass)
    }
}
