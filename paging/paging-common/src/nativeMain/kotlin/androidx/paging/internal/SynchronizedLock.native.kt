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

@file:OptIn(ExperimentalForeignApi::class)

package androidx.paging.internal

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.createCleaner
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * Wrapper for platform.posix.PTHREAD_MUTEX_RECURSIVE which is represented as kotlin.Int on darwin
 * platforms and kotlin.UInt on linuxX64 See: // https://youtrack.jetbrains.com/issue/KT-41509
 */
internal expect val PTHREAD_MUTEX_RECURSIVE: Int

internal expect class SynchronizedLockNativeImpl() {
    internal fun lock(): Int

    internal fun unlock(): Int

    internal fun destroy()
}

internal actual class SynchronizedLock actual constructor() {

    private val delegate = SynchronizedLockNativeImpl()

    actual inline fun <T> withLockImpl(block: () -> T): T {
        try {
            delegate.lock()
            return block()
        } finally {
            delegate.unlock()
        }
    }

    @Suppress("unused") // The returned Cleaner must be assigned to a property
    @ExperimentalNativeApi
    private val cleaner = createCleaner(delegate, SynchronizedLockNativeImpl::destroy)
}
