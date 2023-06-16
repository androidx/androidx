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

package androidx.window.area.utils

import android.os.Build
import androidx.window.area.WindowAreaCapability.Operation
import java.util.Locale

/**
 * Object to provide util methods for non-standard behaviors around the
 * [Operation.OPERATION_PRESENT_ON_AREA] operation that may exist on certain devices or certain
 * vendor API levels. This currently includes the ability for the feature to be available before the
 * original Vendor API level that it was developed in (vendorApiLevel 3). This object can expand to
 * include any differences that have to be taken into account that vary from the standard behavior.
 */
internal object PresentationCompatUtils {

    // Presentation is supported on select Samsung devices with OneUi 5.1.1 or higher
    private const val samsungOneUi511 = 140500
    // Samsung devices that support presentation on vendorApiLevel 1
    private val SUPPORTED_DEVICES_SAMSUNG = listOf(
        "sm-f907",
        "scv44",
        "sm-w2020",
        "sm-f916",
        "scg05",
        "sm-w2021",
        "sm-f926",
        "sc-55b",
        "scg11",
        "sm-w2022",
        "sm-f936",
        "sc-55c",
        "scg16",
        "sm-w9023",
        "sm-f946",
        "sc-55d",
        "scg22",
        "sm-w9024")

    // Oppo devices that support presentation on vendorApiLevel 1
    private val SUPPORTED_DEVICES_OPPO = listOf(
        "pgu110"
    )

    /**
     * Returns if the feature is supported before the planned vendorApiLevel of 3.
     */
    fun doesSupportPresentationBeforeVendorApi3(): Boolean {
        return when (Build.BRAND.lowercase(Locale.US)) {
            "samsung" -> {
                isSupportedOneUiVersion() && SUPPORTED_DEVICES_SAMSUNG.any {
                    Build.MODEL.lowercase(
                        Locale.US
                    ).startsWith(it, ignoreCase = true)
                }
            }
            "oppo" -> SUPPORTED_DEVICES_OPPO.any {
                Build.MODEL.lowercase(Locale.US).startsWith(it, ignoreCase = true)
            }
            else -> false
        }
    }

    private fun isSupportedOneUiVersion(): Boolean {
        return try {
            val semPlatformIntField = Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
            semPlatformIntField.getInt(null) >= samsungOneUi511
        } catch (ex: NoSuchFieldException) {
            false
        } catch (ex: IllegalAccessException) {
            false
        }
    }
}
