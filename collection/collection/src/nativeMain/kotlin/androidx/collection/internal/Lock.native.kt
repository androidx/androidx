/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.collection.internal

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.native.internal.createCleaner

/**
 * Wrapper for platform.posix.PTHREAD_MUTEX_RECURSIVE which is represented as kotlin.Int on darwin
 * platforms and kotlin.UInt on linuxX64 See: // https://youtrack.jetbrains.com/issue/KT-41509
 */
internal expect val PTHREAD_MUTEX_RECURSIVE: Int

internal expect class LockImpl() {
    internal fun lock()

    internal fun unlock()

    internal fun destroy()
}

internal actual class Lock actual constructor() {

    private val lockImpl = LockImpl()

    // unused - The returned Cleaner must be assigned to a property
    // TODO(/365786168) Replace with kotlin.native.ref.createCleaner, after kotlin bump to 1.9+
    @Suppress("unused", "DEPRECATION")
    @OptIn(ExperimentalStdlibApi::class)
    private val cleaner = createCleaner(lockImpl, LockImpl::destroy)

    actual inline fun <T> synchronizedImpl(block: () -> T): T {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

        lock()
        return try {
            block()
        } finally {
            unlock()
        }
    }

    fun lock() {
        lockImpl.lock()
    }

    fun unlock() {
        lockImpl.unlock()
    }
}
