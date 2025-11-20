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

package androidx.camera.extensions.internal.compat.quirk

import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * <p>QuirkSummary Bug Id: b/425225593 Description: accessing the
 * [CameraExtensionCharacteristics#isPostviewAvailable] API causes the NoSuchMethodError in the OEM
 * implementation and then make the extensions malfunction. This requires CameraX implementation to
 * totally avoid to access the API and directly return false. Device(s): Xiaomi 15 devices.
 */
public class AvoidPostviewAvailabilityCheckQuirk : Quirk {
    public companion object {
        @JvmStatic
        public fun load(): Boolean {
            return "Xiaomi".equals(Build.BRAND, ignoreCase = true) &&
                "dada".equals(Build.DEVICE, ignoreCase = true) // Xiaomi 15
        }
    }
}
