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

package androidx.compose.ui.text.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Throws an [IllegalStateException] with the specified [message]. This function is guaranteed to
 * not be inlined, which reduces the amount of code generated at the call site. This code size
 * reduction helps performance by not pre-caching instructions that will rarely/never be executed.
 */
internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

/**
 * Throws an [IllegalStateException] with the specified [message]. This function is guaranteed to
 * not be inlined, which reduces the amount of code generated at the call site. This code size
 * reduction helps performance by not pre-caching instructions that will rarely/never be executed.
 *
 * This function returns [Nothing] to tell the compiler it's a terminating branch in the code,
 * making it suitable for use in a `when` statement or when doing a `null` check to force a smart
 * cast to non-null (see [checkPreconditionNotNull].
 */
internal fun throwIllegalStateExceptionForNullCheck(message: String): Nothing {
    throw IllegalStateException(message)
}

/**
 * Throws an [IllegalArgumentException] with the specified [message]. This function is guaranteed to
 * not be inlined, which reduces the amount of code generated at the call site. This code size
 * reduction helps performance by not pre-caching instructions that will rarely/never be executed.
 */
internal fun throwIllegalArgumentException(message: String) {
    throw IllegalArgumentException(message)
}

/**
 * Throws an [IllegalArgumentException] with the specified [message]. This function is guaranteed to
 * not be inlined, which reduces the amount of code generated at the call site. This code size
 * reduction helps performance by not pre-caching instructions that will rarely/never be executed.
 *
 * This function returns [Nothing] to tell the compiler it's a terminating branch in the code,
 * making it suitable for use in a `when` statement or when doing a `null` check to force a smart
 * cast to non-null (see [requirePreconditionNotNull].
 */
internal fun throwIllegalArgumentExceptionForNullCheck(message: String): Nothing {
    throw IllegalArgumentException(message)
}

/**
 * Like Kotlin's [check] but without the `toString()` call on the output of the lambda, and a
 * non-inline throw. This implementation generates less code at the call site, which can help
 * performance by not loading instructions that will rarely/never execute.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalStateException(lazyMessage())
    }
}

/**
 * Like Kotlin's [checkNotNull] but without the `toString()` call on the output of the lambda, and a
 * non-inline throw. This implementation generates less code at the call site, which can help
 * performance by not loading instructions that will rarely/never execute.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> checkPreconditionNotNull(value: T?, lazyMessage: () -> String): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalStateExceptionForNullCheck(lazyMessage())
    }

    return value
}

/**
 * Like Kotlin's [require] but without the `toString()` call on the output of the lambda, and a
 * non-inline throw. This implementation generates less code at the call site, which can help
 * performance by not loading instructions that will rarely/never execute.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class) // same opt-in as using Kotlin's require()
internal inline fun requirePrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalArgumentException(lazyMessage())
    }
}

/**
 * Like Kotlin's [requireNotNull] but without the `toString()` call on the output of the lambda, and
 * a non-inline throw. This implementation generates less code at the call site, which can help
 * performance by not loading instructions that will rarely/never execute.
 */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> requirePreconditionNotNull(value: T?, lazyMessage: () -> String): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalArgumentExceptionForNullCheck(lazyMessage())
    }

    return value
}
