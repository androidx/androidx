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

package androidx.camera.camera2.internal

import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM
import android.hardware.camera2.params.DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM
import android.hardware.camera2.params.DynamicRangeProfiles.HDR10
import android.hardware.camera2.params.DynamicRangeProfiles.HDR10_PLUS
import android.hardware.camera2.params.DynamicRangeProfiles.HLG10
import android.hardware.camera2.params.DynamicRangeProfiles.STANDARD

val HLG10_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(longArrayOf(HLG10, 0, 0))
}

val HLG10_CONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, HLG10, LATENCY_NONE
        )
    )
}

val HLG10_SDR_CONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, HLG10 or STANDARD, LATENCY_NONE
        )
    )
}

val HLG10_HDR10_CONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, HLG10 or HDR10, LATENCY_NONE,
            HDR10, HDR10 or HLG10, LATENCY_NONE
        )
    )
}

val HDR10_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, CONSTRAINTS_NONE, LATENCY_NONE, // HLG is mandated
            HDR10, CONSTRAINTS_NONE, LATENCY_NONE
        )
    )
}

val HDR10_PLUS_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, CONSTRAINTS_NONE, LATENCY_NONE, // HLG is mandated
            HDR10_PLUS, CONSTRAINTS_NONE, LATENCY_NONE
        )
    )
}

val HDR10_HDR10_PLUS_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, CONSTRAINTS_NONE, LATENCY_NONE, // HLG is mandated
            HDR10, CONSTRAINTS_NONE, LATENCY_NONE,
            HDR10_PLUS, CONSTRAINTS_NONE, LATENCY_NONE
        )
    )
}

val DOLBY_VISION_10B_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, CONSTRAINTS_NONE, LATENCY_NONE, // HLG is mandated
            DOLBY_VISION_10B_HDR_OEM, CONSTRAINTS_NONE, LATENCY_NONE
        )
    )
}

val DOLBY_VISION_10B_UNCONSTRAINED_SLOW by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, CONSTRAINTS_NONE, LATENCY_NONE, // HLG is mandated
            DOLBY_VISION_10B_HDR_OEM, CONSTRAINTS_NONE, LATENCY_NON_ZERO
        )
    )
}

val DOLBY_VISION_8B_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            DOLBY_VISION_8B_HDR_OEM, CONSTRAINTS_NONE, LATENCY_NONE
        )
    )
}

val DOLBY_VISION_8B_SDR_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            DOLBY_VISION_8B_HDR_OEM, DOLBY_VISION_8B_HDR_OEM or STANDARD, LATENCY_NONE
        )
    )
}

val DOLBY_VISION_8B_UNCONSTRAINED_HLG10_UNCONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, CONSTRAINTS_NONE, LATENCY_NONE,
            DOLBY_VISION_8B_HDR_OEM, CONSTRAINTS_NONE, LATENCY_NONE,
        )
    )
}

val DOLBY_VISION_CONSTRAINED by lazy {
    DynamicRangeProfiles(
        longArrayOf(
            HLG10, HLG10, LATENCY_NONE, // HLG is mandated
            DOLBY_VISION_10B_HDR_OEM, DOLBY_VISION_10B_HDR_OEM or DOLBY_VISION_8B_HDR_OEM,
            LATENCY_NONE,
            DOLBY_VISION_8B_HDR_OEM, DOLBY_VISION_8B_HDR_OEM or DOLBY_VISION_10B_HDR_OEM,
            LATENCY_NONE
        )
    )
}

const val LATENCY_NONE = 0L
private const val LATENCY_NON_ZERO = 3L
private const val CONSTRAINTS_NONE = 0L