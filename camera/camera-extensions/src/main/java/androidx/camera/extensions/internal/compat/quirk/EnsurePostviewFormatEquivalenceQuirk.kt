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
 * <p>QuirkSummary Bug Id: b/409722084 Description: According to the javadoc of {@link
 * CameraExtensionCharacteristics#isPostviewAvailable()}, the formats of the still capture and
 * postview were required to be equivalent on API level 34 devices and are not required to be
 * equivalent from Android V (postview is supported since API level 34). Device(s): All API level 34
 * devices.
 */
public class EnsurePostviewFormatEquivalenceQuirk : Quirk {
    public companion object {
        @JvmStatic
        public fun load(): Boolean {
            return Build.VERSION.SDK_INT == Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        }
    }
}
