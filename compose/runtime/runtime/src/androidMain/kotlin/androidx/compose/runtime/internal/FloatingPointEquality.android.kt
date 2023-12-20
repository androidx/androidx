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

package androidx.compose.runtime.internal

import android.os.Build

/**
 * Returns `true` if the receiver is an expression of [Float.NaN]. There is not one single encoding
 * of `NaN`. A float encodes NaN if its exponent bits (the 8 most significant bits which follow the
 * single most significant bit in the float, which encodes the sign) are all set to `1`, and the
 * significand bits (the 23 least significant bits) are not all zeros.
 */
internal val Float.isNan: Boolean
    get() = (this.toRawBits() and 0x7FFFFFFF) > 0x7F800000

/**
 * Returns `true` if the receiver is an expression of [Double.NaN]. There is not one single encoding
 * of `NaN`. A double encodes NaN if its exponent bits (the 11 most significant bits which follow
 * the single most significant bit in the double, which encodes the sign) are all set to `1`, and
 * the significand bits (the 53 least significant bits) are not all zeros.
 */
internal val Double.isNan: Boolean
    get() = (this.toRawBits() and 0x7FFFFFFFFFFFFFFF) > 0x7FF0000000000000

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Float.equalsWithNanFix(other: Float): Boolean =
    if (Build.VERSION.SDK_INT >= 23) {
        this == other
    } else {
        !this.isNan && !other.isNan && this == other
    }

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun Double.equalsWithNanFix(other: Double): Boolean =
    if (Build.VERSION.SDK_INT >= 23) {
        this == other
    } else {
        !this.isNan && !other.isNan && this == other
    }
