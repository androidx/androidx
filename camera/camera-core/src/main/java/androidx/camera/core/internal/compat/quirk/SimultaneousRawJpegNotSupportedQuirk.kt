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

package androidx.camera.core.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.impl.Quirk

/**
 * QuirkSummary
 * - Bug Id: b/549577267, b/546416253
 * - Description: Quirk required to disable simultaneous RAW + JPEG capture on devices where the
 *   camera HAL fails to configure concurrent maximum-resolution RAW and JPEG streams.
 * - Device(s): OPPO Reno10 Pro 5G (cph2525 / op56dbl1)
 *
 * TODO(b/270421716): enable CameraXQuirksClassDetector lint check when kotlin is supported.
 */
@SuppressLint("CameraXQuirksClassDetector")
public class SimultaneousRawJpegNotSupportedQuirk : Quirk {

    public companion object {
        @JvmStatic public fun load(): Boolean = isOppoReno10Pro()

        private fun isOppoReno10Pro(): Boolean =
            "op56dbl1".equals(Build.DEVICE, ignoreCase = true) ||
                "cph2525".equals(Build.MODEL, ignoreCase = true)
    }
}
