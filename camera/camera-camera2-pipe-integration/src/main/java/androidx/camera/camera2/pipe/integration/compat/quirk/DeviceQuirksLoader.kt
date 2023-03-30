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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.compat.quirk

import androidx.annotation.RequiresApi
import androidx.camera.core.impl.Quirk

/**
 * Loads all device specific quirks required for the current device.
 */
object DeviceQuirksLoader {

    /**
     * Goes through all defined device-specific quirks, and returns those that should be loaded
     * on the current device.
     */
    fun loadQuirks(): List<Quirk> {
        val quirks: MutableList<Quirk> = mutableListOf()

        // Load all device specific quirks.
        if (InvalidVideoProfilesQuirk.load()) {
            quirks.add(InvalidVideoProfilesQuirk())
        }

        return quirks
    }
}