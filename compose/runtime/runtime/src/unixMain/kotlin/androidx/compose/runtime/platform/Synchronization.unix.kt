/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.runtime.platform

import kotlinx.cinterop.Arena
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import platform.posix.pthread_cond_broadcast
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait
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
 * Wrapper for `platform.posix.PTHREAD_MUTEX_RECURSIVE`, which is represented as `kotlin.Int` on
 * Apple platforms and `kotlin.UInt` on linuxX64.
 *
 * See: [KT-41509](https://youtrack.jetbrains.com/issue/KT-41509)
 */
internal expect val PTHREAD_MUTEX_RECURSIVE: Int

@OptIn(ExperimentalForeignApi::class)
internal actual class NativeMonitor {
    private val arena: Arena = Arena()
    private val cond: pthread_cond_t = arena.alloc()
    private val mutex: pthread_mutex_t = arena.alloc()
    private val attr: pthread_mutexattr_t = arena.alloc()

    init {
        require(pthread_cond_init(cond.ptr, null) == 0)
        require(pthread_mutexattr_init(attr.ptr) == 0)
        require(pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_RECURSIVE) == 0)
        require(pthread_mutex_init(mutex.ptr, attr.ptr) == 0)
    }

    actual fun enter() = require(pthread_mutex_lock(mutex.ptr) == 0)

    actual fun exit() = require(pthread_mutex_unlock(mutex.ptr) == 0)

    actual fun wait() = require(pthread_cond_wait(cond.ptr, mutex.ptr) == 0)

    actual fun notifyAll() = require(pthread_cond_broadcast(cond.ptr) == 0)

    actual fun dispose() {
        pthread_cond_destroy(cond.ptr)
        pthread_mutex_destroy(mutex.ptr)
        pthread_mutexattr_destroy(attr.ptr)
        arena.clear()
    }
}
