/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirksLoader.loadQuirks
import androidx.camera.core.impl.Quirk

/**
 * Tests version of main/.../DeviceQuirks.java, which provides device specific quirks, used for
 * device specific workarounds.
 *
 * In main/.../DeviceQuirks, Device quirks are loaded the first time a device workaround is
 * encountered, and remain in memory until the process is killed. When running tests, this means
 * that the same device quirks are used for all the tests. This causes an issue when tests modify
 * device properties (using Robolectric for instance). Instead of force-reloading the device
 * quirks in every test that uses a device workaround, this class internally reloads the quirks
 * every time a device workaround is needed.
 */
@RequiresApi(21)
object DeviceQuirks {
    /**
     * Retrieves a specific device [Quirk] instance given its type.
     *
     * @param quirkClass The type of device quirk to retrieve.
     * @return A device [Quirk] instance of the provided type, or `null` if it isn't
     * found.
     */
    operator fun <T : Quirk?> get(quirkClass: Class<T>): T? {
        val quirks = loadQuirks()
        for (quirk in quirks) {
            if (quirk.javaClass == quirkClass) {
                @Suppress("UNCHECKED_CAST")
                return quirk as T
            }
        }
        return null
    }

    /**
     * Retrieves all device [Quirk] instances that are or inherit the given type.
     *
     * @param quirkClass The super type of device quirk to retrieve.
     * @return A device [Quirk] list of the provided type. An empty list is returned if it
     * isn't found.
     */
    fun <T : Quirk?> getAll(quirkClass: Class<T>): List<T> {
        val quirks = loadQuirks()
        val list: MutableList<T> = ArrayList()
        for (quirk in quirks) {
            if (quirkClass.isAssignableFrom(quirk.javaClass)) {
                @Suppress("UNCHECKED_CAST")
                list.add(quirk as T)
            }
        }
        return list
    }
}
