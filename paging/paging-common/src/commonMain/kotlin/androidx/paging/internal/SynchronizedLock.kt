/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.paging.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal expect class SynchronizedLock() {

    /**
     * It's not possible to specify a `contract` for an expect function, see
     * https://youtrack.jetbrains.com/issue/KT-29963.
     *
     * Please use [SynchronizedLock.withLock] function, where the `contract` is actually specified.
     */
    inline fun <T> withLockImpl(block: () -> T): T
}

@OptIn(ExperimentalContracts::class)
// Workaround for applying callsInPlace to expect fun no longer works.
// See: https://youtrack.jetbrains.com/issue/KT-29963
@Suppress("LEAKED_IN_PLACE_LAMBDA", "WRONG_INVOCATION_KIND", "BanInlineOptIn")
internal inline fun <T> SynchronizedLock.withLock(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return withLockImpl(block)
}
