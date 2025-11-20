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

package androidx.camera.camera2.compat.quirk

import android.os.Build

public object Device {
    public fun isBluDevice(): Boolean = isDeviceFrom("Blu")

    public fun isHuaweiDevice(): Boolean = isDeviceFrom("Huawei")

    public fun isInfinixDevice(): Boolean = isDeviceFrom("Infinix")

    public fun isItelDevice(): Boolean = isDeviceFrom("Itel")

    public fun isJioDevice(): Boolean = isDeviceFrom("Jio")

    public fun isGoogleDevice(): Boolean = isDeviceFrom("Google")

    public fun isMotorolaDevice(): Boolean = isDeviceFrom("Motorola")

    public fun isNokiaDevice(): Boolean = isDeviceFrom("Nokia")

    public fun isOnePlusDevice(): Boolean = isDeviceFrom("OnePlus")

    public fun isOppoDevice(): Boolean = isDeviceFrom("Oppo")

    public fun isPositivoDevice(): Boolean = isDeviceFrom("Positivo")

    public fun isRealmeDevice(): Boolean = isDeviceFrom("Realme")

    public fun isRedmiDevice(): Boolean = isDeviceFrom("Redmi")

    public fun isSamsungDevice(): Boolean = isDeviceFrom("Samsung")

    public fun isSonyDevice(): Boolean = isDeviceFrom("Sony")

    public fun isTecnoDevice(): Boolean = isDeviceFrom("Tecno") || isDeviceFrom("Tecno-mobile")

    public fun isXiaomiDevice(): Boolean = isDeviceFrom("Xiaomi")

    public fun isVivoDevice(): Boolean = isDeviceFrom("Vivo")

    public fun isPocoDevice(): Boolean = isDeviceFrom("Poco")

    private fun isDeviceFrom(vendor: String) =
        Build.MANUFACTURER.equalsCaseInsensitive(vendor) ||
            Build.BRAND.equalsCaseInsensitive(vendor)

    private fun String.equalsCaseInsensitive(other: String?) = equals(other, ignoreCase = true)

    /**
     * There is no clear way to determine whether a device is UniSoc or not. Check the possible
     * properties to turn the results.
     */
    public fun isUniSocChipsetDevice(): Boolean {
        return (Build.VERSION.SDK_INT >= 31 &&
            "Spreadtrum".equals(Build.SOC_MANUFACTURER, ignoreCase = true)) ||
            Build.HARDWARE.lowercase().startsWith("ums") ||
            (isItelDevice() && Build.HARDWARE.lowercase().startsWith("sp"))
    }
}
