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

package androidx.camera.viewfinder.core

import androidx.camera.viewfinder.core.impl.ImplementationModeCompat

/** Provides default values and configurations for viewfinder components. */
object ViewfinderDefaults {
    /**
     * The default [ImplementationMode] chosen based on device compatibility.
     *
     * This value is determined by checking for known device quirks and API level limitations. It
     * will be [ImplementationMode.EMBEDDED] on devices with known issues (such as on API level 24
     * and below), and the higher-performance [ImplementationMode.EXTERNAL] otherwise.
     */
    @JvmStatic
    val implementationMode: ImplementationMode
        get() = ImplementationModeCompat.chooseCompatibleMode()
}
