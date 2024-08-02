/*
 * Copyright 2024 The Android Open Source Project
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

import android.os.Build

object Device {
    fun isBluDevice() = isDeviceFrom("Blu")

    fun isHuaweiDevice() = isDeviceFrom("Huawei")

    fun isInfinixDevice() = isDeviceFrom("Infinix")

    fun isItelDevice() = isDeviceFrom("Itel")

    fun isJioDevice() = isDeviceFrom("Jio")

    fun isGoogleDevice() = isDeviceFrom("Google")

    fun isMotorolaDevice() = isDeviceFrom("Motorola")

    fun isOnePlusDevice() = isDeviceFrom("OnePlus")

    fun isOppoDevice() = isDeviceFrom("Oppo")

    fun isPositivoDevice() = isDeviceFrom("Positivo")

    fun isRedmiDevice() = isDeviceFrom("Redmi")

    fun isSamsungDevice() = isDeviceFrom("Samsung")

    fun isXiaomiDevice() = isDeviceFrom("Xiaomi")

    fun isVivoDevice() = isDeviceFrom("Vivo")

    private fun isDeviceFrom(vendor: String) =
        Build.MANUFACTURER.equalsCaseInsensitive(vendor) ||
            Build.BRAND.equalsCaseInsensitive(vendor)

    private fun String.equalsCaseInsensitive(other: String?) = equals(other, ignoreCase = true)
}
