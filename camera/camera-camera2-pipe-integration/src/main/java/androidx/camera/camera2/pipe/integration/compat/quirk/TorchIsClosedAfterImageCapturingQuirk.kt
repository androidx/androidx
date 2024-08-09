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

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: 228272227
 * - Description: The Torch is unexpectedly turned off after taking a picture.
 * - Device(s): Redmi 4X, Redmi 5A, Redmi Note 5 (Pro), Mi A1, Mi A2, Mi A2 lite and Redmi 6 Pro.
 */
@SuppressLint("CameraXQuirksClassDetector")
// TODO(b/270421716): enable when kotlin is supported.
public class TorchIsClosedAfterImageCapturingQuirk : Quirk {

    public companion object {
        // List of devices with the issue. See b/228272227.
        private val BUILD_MODELS =
            listOf(
                "mi a1", // Xiaomi Mi A1
                "mi a2", // Xiaomi Mi A2
                "mi a2 lite", // Xiaomi Mi A2 Lite
                "redmi 4x", // Xiaomi Redmi 4X
                "redmi 5a", // Xiaomi Redmi 5A
                "redmi note 5", // Xiaomi Redmi Note 5
                "redmi note 5 pro", // Xiaomi Redmi Note 5 Pro
                "redmi 6 pro", // Xiaomi Redmi 6 Pro
            )

        public fun isEnabled(): Boolean {
            return BUILD_MODELS.contains(Build.MODEL.lowercase())
        }
    }
}
