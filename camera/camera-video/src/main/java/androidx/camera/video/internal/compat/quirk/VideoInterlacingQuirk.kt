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

package androidx.camera.video.internal.compat.quirk

import android.annotation.SuppressLint
import android.os.Build
import androidx.camera.core.internal.compat.quirk.SurfaceProcessingQuirk
import java.util.Locale

/**
 * QuirkSummary
 * - Bug Id: b/202798966, b/212325319, b/235127608
 * - Description: Quirk denotes the recorded video is interlaced. This issue occurs on devices using
 *   the Exynos 7420 Octa (14 nm) chipset. Enabling the OpenGL pipeline work around this issue.
 * - Device(s): SM-N9208, Samsung Galaxy S6
 */
@SuppressLint("CameraXQuirksClassDetector")
public object VideoInterlacingQuirk : SurfaceProcessingQuirk {

    private val DEVICE_MODELS: List<String>
        get() = listOf("SM-N9208")

    /**
     * A flag that indicates if the device is a Samsung Galaxy S6 series.
     *
     * All Samsung Galaxy S6 models use a product name that starts with 'zeroflte'. While all of
     * these models are believed to have this issue, only the following have been explicitly
     * identified and reported:
     * - Lab devices: SM-G920V(b/212325319), SM-G920P, SM-G920L.
     * - Reported by developers: SM-G920F(b/235127608).
     */
    private val isSamsungS6: Boolean
        get() =
            Build.BRAND.equals("Samsung", ignoreCase = true) &&
                Build.PRODUCT.startsWith("zeroflte", ignoreCase = true)

    @JvmStatic
    public fun load(): Boolean {
        return DEVICE_MODELS.contains(Build.MODEL.uppercase(Locale.getDefault())) || isSamsungS6
    }
}
