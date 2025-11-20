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
package androidx.camera.viewfinder.core.impl.quirk

import androidx.annotation.VisibleForTesting
import androidx.camera.viewfinder.core.impl.quirk.DeviceQuirksLoader.loadQuirks

/**
 * Provider of device specific quirks for the view module, which are used for device specific
 * workarounds.
 *
 * Device specific quirks depend on device properties, including the manufacturer
 * ([android.os.Build.MANUFACTURER]), model ([android.os.Build.MODEL]) and OS level
 * ([android.os.Build.VERSION.SDK_INT]).
 *
 * Device specific quirks are lazily loaded, i.e. They are loaded the first time they're needed.
 */
internal object DeviceQuirks {
    private val QUIRKS: Quirks = Quirks(loadQuirks())

    /**
     * Retrieves a specific device [Quirk] instance given its type.
     *
     * @return A device [Quirk] instance of the provided type, or `null` if it isn't found.
     */
    inline fun <reified T : Quirk> get(): T? = QUIRKS.get<T>()

    /**
     * Returns whether this collection of quirks contains a quirk with the provided type.
     *
     * This checks whether the provided quirk type is the exact class, a superclass, or a
     * superinterface of any of the contained quirks, and will return true in all cases.
     *
     * @return `true` if this container contains a quirk with the given type, `false` otherwise.
     */
    inline fun <reified T : Quirk> contains(): Boolean = QUIRKS.contains<T>()

    @VisibleForTesting
    fun reload() {
        QUIRKS.reset(loadQuirks())
    }
}
