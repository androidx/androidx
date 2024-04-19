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

package androidx.compose.ui.geometry

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// Masks all float values that are infinity or NaN (i.e. any non-finite value)
internal const val FloatNonFiniteMask = 0x7fffffffL

// Any value greater than this is a NaN
internal const val FloatInfinityBase = 0x7f800000L

// Same as Offset/Size.Unspecified.packedValue, but avoids a getstatic
internal const val UnspecifiedPackedFloats = 0x7fc00000_7fc00000L // NaN_NaN

// This function exists so we do *not* inline the throw. It keeps
// the call site much smaller and since it's the slow path anyway,
// we don't mind the extra function call
internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

// Like Kotlin's require() but without the .toString() call
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }
    if (!value) {
        throwIllegalStateException(lazyMessage())
    }
}
