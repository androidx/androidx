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

/**
 * Compatibility wrapper for [CameraCharacteristics] APIs introduced in API level 29 (Android Q).
 */
@RequiresApi(29)
internal object Api29Compat {
    /**
     * Returns a list of keys in [CameraCharacteristics] that require camera permission to retrieve.
     *
     * For applications targeting Android Q or higher, some keys in [CameraCharacteristics] require
     * camera permission. Querying these keys without permission will return `null`.
     *
     * This method delegates to [CameraCharacteristics.getKeysNeedingPermission].
     *
     * @param cameraCharacteristics The camera characteristics to query.
     * @return The list of keys that require camera permission.
     */
    @JvmStatic
    fun getKeysNeedingPermission(
        cameraCharacteristics: CameraCharacteristics
    ): List<CameraCharacteristics.Key<*>> {
        return cameraCharacteristics.keysNeedingPermission
    }
}
