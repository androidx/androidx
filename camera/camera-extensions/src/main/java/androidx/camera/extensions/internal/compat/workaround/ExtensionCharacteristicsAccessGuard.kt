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

package androidx.camera.extensions.internal.compat.workaround

import android.hardware.camera2.CameraExtensionCharacteristics
import androidx.camera.extensions.internal.compat.quirk.AvoidCaptureProcessProgressAvailabilityCheckQuirk
import androidx.camera.extensions.internal.compat.quirk.AvoidPostviewAvailabilityCheckQuirk
import androidx.camera.extensions.internal.compat.quirk.DeviceQuirks

/** A workaround for safely accessing the [CameraExtensionCharacteristics] related information. */
public class ExtensionCharacteristicsAccessGuard {
    private val avoidPostviewAvailabilityCheckQuirk: AvoidPostviewAvailabilityCheckQuirk? =
        DeviceQuirks.get(AvoidPostviewAvailabilityCheckQuirk::class.java)
    private val avoidCaptureProcessProgressAvailabilityCheckQuirk:
        AvoidCaptureProcessProgressAvailabilityCheckQuirk? =
        DeviceQuirks.get(AvoidCaptureProcessProgressAvailabilityCheckQuirk::class.java)

    /**
     * Returns true if [CameraExtensionCharacteristics.isPostviewAvailable] can be safely invoked.
     * Otherwise, returns false.
     */
    public fun allowPostviewAvailabilityCheck(): Boolean =
        avoidPostviewAvailabilityCheckQuirk == null

    /**
     * Returns true if [CameraExtensionCharacteristics.isCaptureProcessProgressAvailable] can be
     * safely invoked. Otherwise, returns false.
     */
    public fun allowCaptureProcessProgressAvailabilityCheck(): Boolean =
        avoidCaptureProcessProgressAvailabilityCheckQuirk == null
}
