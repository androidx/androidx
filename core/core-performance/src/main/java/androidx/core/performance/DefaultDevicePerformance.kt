/*
 * Copyright (C) 2023 The Android Open Source Project
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

package androidx.core.performance

import android.os.Build

/**
 * Reports the media performance class of the device. Contains statically specified values
 * and can be used as a fallback alternative to suppliers with dynamic values.
 */
class DefaultDevicePerformance() : DevicePerformance {
    private val PERFCLASS_11: Int = Build.VERSION_CODES.R
    private val PERFCLASS_12: Int = Build.VERSION_CODES.S
    private val PERFCLASS_13: Int = Build.VERSION_CODES.TIRAMISU
    private val PERFCLASS_NONE: Int = 0

    private val fingerprints: HashMap<String, Int> = hashMapOf(
        // for unit testing, no actual products with these
        "robolectric-BrandX/ProductX/Device30:11" to PERFCLASS_11,
        "robolectric-BrandX/ProductX/Device31:12" to PERFCLASS_12,

        // actual devices in the field
        "OPPO/CPH2025EEA/OP4BA2L1:12" to PERFCLASS_11,
        "OPPO/CPH2207EEA/OP4F0BL1:12" to PERFCLASS_11,
        "OPPO/PENM00/OP4EC1:11" to PERFCLASS_11,
        "OnePlus/OnePlus7TTMO/OnePlus7TTMO:11" to PERFCLASS_11,
        "OnePlus/OnePlus8_BETA/OnePlus8:11" to PERFCLASS_11,
        "Xiaomi/umi_global/umi:11" to PERFCLASS_11,
        "realme/RMX2085/RMX2085L1:11" to PERFCLASS_11,
        "samsung/c1qsqw/c1q:12" to PERFCLASS_11,
        "samsung/o1quew/o1q:12" to PERFCLASS_11,
        "samsung/r0quew/r0q:12" to PERFCLASS_11,
        "samsung/r0sxxx/r0s:12" to PERFCLASS_11,
    )

    override val mediaPerformanceClass = getCalculatedMediaPerformanceClass()

    private fun getMediaPerformanceClassFromFingerprint(): Int {

        val brand = Build.BRAND
        val product = Build.PRODUCT
        val device = Build.DEVICE
        val release = Build.VERSION.RELEASE

        val synthesized = "$brand/$product/$device:$release"
        var pc = fingerprints[synthesized]

        if (pc == null) {
            pc = PERFCLASS_NONE
        }
        return pc
    }

    private fun getMediaPerformanceClassFromProperty(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Build.VERSION.MEDIA_PERFORMANCE_CLASS
        }

        return PERFCLASS_NONE
    }

    private fun isPerformanceClassValid(pc: Int): Boolean = pc >= PERFCLASS_11

    private fun getCalculatedMediaPerformanceClass(): Int {

        // device's declared property takes precedence over our in-library table.
        val mpcViaProperty: Int =
            getMediaPerformanceClassFromProperty()

        if (isPerformanceClassValid(mpcViaProperty)) {
            return mpcViaProperty
        }

        val mpcViaFingerprint: Int =
            getMediaPerformanceClassFromFingerprint()

        if (isPerformanceClassValid(mpcViaFingerprint)) {
            return mpcViaFingerprint
        }

        return PERFCLASS_NONE
    }
}
