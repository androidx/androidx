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

package androidx.camera.camera2.pipe.integration.compat.workaround

import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.FlashAvailabilityBufferUnderflowQuirk
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import java.nio.BufferUnderflowException

/**
 * A workaround for devices which may throw a [BufferUnderflowException] when checking flash
 * availability.
 *
 * @param allowRethrowOnError whether exceptions can be rethrown on devices that are not known to be
 *   problematic. If `false`, these devices will be logged as an error instead.
 * @return the value of [CameraCharacteristics.FLASH_INFO_AVAILABLE] if it is contained in the
 *   characteristics, or `false` if it is not or a [BufferUnderflowException] is thrown while
 *   checking.
 * @see FlashAvailabilityBufferUnderflowQuirk
 */
public fun CameraProperties.isFlashAvailable(allowRethrowOnError: Boolean = false): Boolean {
    val flashAvailable =
        try {
            metadata[CameraCharacteristics.FLASH_INFO_AVAILABLE]
        } catch (e: BufferUnderflowException) {
            if (DeviceQuirks[FlashAvailabilityBufferUnderflowQuirk::class.java] != null) {
                Log.debug {
                    "Device is known to throw an exception while checking flash availability. Flash" +
                        " is not available. [Manufacturer: ${Build.MANUFACTURER}, Model:" +
                        " ${Build.MODEL}, API Level: ${Build.VERSION.SDK_INT}]."
                }
            } else {
                Log.error(e) {
                    "Exception thrown while checking for flash availability on device not known to " +
                        "throw exceptions during this check. Please file an issue at " +
                        "https://issuetracker.google.com/issues/new?component=618491&template=1257717" +
                        " with this error message [Manufacturer: ${Build.MANUFACTURER}, Model:" +
                        " ${Build.MODEL}, API Level: ${Build.VERSION.SDK_INT}]. Flash is not available."
                }
            }

            if (allowRethrowOnError) {
                throw e
            } else {
                false
            }
        }
    if (flashAvailable == null) {
        Log.warn {
            "Characteristics did not contain key FLASH_INFO_AVAILABLE. Flash is not available."
        }
    }
    return flashAvailable ?: false
}
