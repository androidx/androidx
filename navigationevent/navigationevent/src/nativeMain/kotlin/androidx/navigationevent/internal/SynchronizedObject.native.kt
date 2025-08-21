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

package androidx.navigationevent.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner

/**
 * Wrapper for platform.posix.PTHREAD_MUTEX_RECURSIVE which is represented as kotlin.Int on darwin
 * platforms and kotlin.UInt on linuxX64 See: https://youtrack.jetbrains.com/issue/KT-41509
 */
internal expect val PTHREAD_MUTEX_RECURSIVE: Int

internal expect class SynchronizedObjectImpl() {
    internal fun lock(): Int

    internal fun unlock(): Int

    internal fun dispose()
}

internal actual class SynchronizedObject actual constructor() {
    private val impl = SynchronizedObjectImpl()

    @Suppress("unused") // The returned Cleaner must be assigned to a property
    @OptIn(ExperimentalNativeApi::class)
    private val cleaner = createCleaner(impl, SynchronizedObjectImpl::dispose)

    fun lock() {
        impl.lock()
    }

    fun unlock() {
        impl.unlock()
    }
}

@OptIn(ExperimentalContracts::class)
internal actual inline fun <T> synchronizedImpl(
    lock: SynchronizedObject,
    crossinline action: () -> T,
): T {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    lock.lock()
    return try {
        action()
    } finally {
        lock.unlock()
    }
}
