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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.ui.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// This function exists so we do *not* inline the throw. It keeps
// the call site much smaller and since it's the slow path anyway,
// we don't mind the extra function call
internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

internal fun throwIllegalStateExceptionForNullCheck(message: String): Nothing {
    throw IllegalStateException(message)
}

internal fun throwIllegalArgumentException(message: String) {
    throw IllegalArgumentException(message)
}

internal fun throwIllegalArgumentExceptionForNullCheck(message: String): Nothing {
    throw IllegalArgumentException(message)
}

internal fun throwUnsupportedOperationException(message: String) {
    throw UnsupportedOperationException(message)
}

internal fun throwIndexOutOfBoundsException(message: String) {
    throw IndexOutOfBoundsException(message)
}

// Like Kotlin's check() but without the .toString() call and
// a non-inline throw
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalStateException(lazyMessage())
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalStateException("Check failed.")
    }
}

// Like Kotlin's checkNotNull() but without the .toString() call and
// a non-inline throw
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> checkPreconditionNotNull(value: T?, lazyMessage: () -> String): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalStateExceptionForNullCheck(lazyMessage())
    }

    return value
}

// Like Kotlin's checkNotNull() but with a non-inline throw
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> checkPreconditionNotNull(value: T?): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalStateExceptionForNullCheck("Required value was null.")
    }

    return value
}

// Like Kotlin's require() but without the .toString() call
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class) // same opt-in as using Kotlin's require()
internal inline fun requirePrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalArgumentException(lazyMessage())
    }
}

// Like Kotlin's requireNotNull() but without the .toString() call and
// a non-inline throw
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> requirePreconditionNotNull(value: T?, lazyMessage: () -> String): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalArgumentExceptionForNullCheck(lazyMessage())
    }

    return value
}
