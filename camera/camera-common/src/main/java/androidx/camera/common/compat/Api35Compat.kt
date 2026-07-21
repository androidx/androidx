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

package androidx.camera.common.compat

import android.hardware.camera2.CameraCharacteristics
import androidx.annotation.RequiresApi

/** Compatibility wrapper for API 35 ([android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM]) APIs. */
@RequiresApi(35)
internal object Api35Compat {
    /**
     * Returns a list of keys supported by this camera device for querying session characteristics.
     *
     * The list returned may be `null` if the device does not support session characteristics.
     *
     * @param cameraCharacteristics The [CameraCharacteristics] to query.
     * @return The list of keys, or `null` if no session characteristics are supported.
     */
    @JvmStatic
    fun getAvailableSessionCharacteristicsKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CameraCharacteristics.Key<*>>? {
        return cameraCharacteristics.availableSessionCharacteristicsKeys
    }
}
