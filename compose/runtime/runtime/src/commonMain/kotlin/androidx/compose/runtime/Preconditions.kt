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

package androidx.compose.runtime

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// This function exists so we do *not* inline the throw. It keeps
// the call site much smaller and since it's the slow path anyway,
// we don't mind the extra function call
internal fun throwIllegalArgumentException(message: String) {
    throw IllegalArgumentException(message)
}

// Like Kotlin's require() but without the .toString() call
@Suppress("BanInlineOptIn") // same opt-in as using Kotlin's require()
@OptIn(ExperimentalContracts::class)
internal inline fun requirePrecondition(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }
    if (!value) {
        throwIllegalArgumentException(lazyMessage())
    }
}

// See above
internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

// Like Kotlin's check() but without the .toString() call
@Suppress("BanInlineOptIn") // same opt-in as using Kotlin's check()
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean, lazyMessage: () -> String) {
    contract {
        returns() implies value
    }
    if (!value) {
        throwIllegalStateException(lazyMessage())
    }
}

@Suppress("BanInlineOptIn", "NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean) {
    contract {
        returns() implies value
    }
    if (!value) {
        throwIllegalStateException("Check failed.")
    }
}
