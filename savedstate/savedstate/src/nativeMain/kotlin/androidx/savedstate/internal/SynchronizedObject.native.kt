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
package androidx.savedstate.internal

import kotlin.native.internal.createCleaner
import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_unlock
import platform.posix.pthread_mutexattr_destroy
import platform.posix.pthread_mutexattr_init
import platform.posix.pthread_mutexattr_settype
import platform.posix.pthread_mutexattr_t

/**
 * Wrapper for platform.posix.PTHREAD_MUTEX_RECURSIVE which is represented as kotlin.Int on darwin
 * platforms and kotlin.UInt on linuxX64 See: // https://youtrack.jetbrains.com/issue/KT-41509
 */
internal expect val PTHREAD_MUTEX_RECURSIVE: Int

internal actual class SynchronizedObject actual constructor() {

    private val resource = Resource()

    @Suppress("unused") // The returned Cleaner must be assigned to a property
    @OptIn(ExperimentalStdlibApi::class)
    private val cleaner = createCleaner(resource, Resource::dispose)

    fun lock() {
        resource.lock()
    }

    fun unlock() {
        resource.unlock()
    }

    @OptIn(ExperimentalForeignApi::class)
    private class Resource {
        private val arena: Arena = Arena()
        private val attr: pthread_mutexattr_t = arena.alloc()
        private val mutex: pthread_mutex_t = arena.alloc()

        init {
            pthread_mutexattr_init(attr.ptr)
            pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_RECURSIVE)
            pthread_mutex_init(mutex.ptr, attr.ptr)
        }

        fun lock(): Int = pthread_mutex_lock(mutex.ptr)

        fun unlock(): Int = pthread_mutex_unlock(mutex.ptr)

        fun dispose() {
            pthread_mutex_destroy(mutex.ptr)
            pthread_mutexattr_destroy(attr.ptr)
            arena.clear()
        }
    }
}

internal actual inline fun <T> synchronizedImpl(
    lock: SynchronizedObject,
    crossinline action: () -> T
): T {
    lock.lock()
    return try {
        action()
    } finally {
        lock.unlock()
    }
}
