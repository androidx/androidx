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

import kotlin.native.ref.createCleaner
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
import kotlinx.atomicfu.*
import kotlinx.atomicfu.AtomicInt
import platform.posix.pthread_cond_destroy
import platform.posix.pthread_cond_init
import platform.posix.pthread_cond_signal
import platform.posix.pthread_cond_t
import platform.posix.pthread_cond_wait

/**
 * Wrapper for `platform.posix.PTHREAD_MUTEX_ERRORCHECK` which is represented as `kotlin.Int` on darwin
 * platforms and `kotlin.UInt` on linuxX64.
 *
 * See: [KT-41509](https://youtrack.jetbrains.com/issue/KT-41509)
 */
internal expect val PTHREAD_MUTEX_ERRORCHECK: Int

/**
 * A synchronized object that provides thread-safe locking and unlocking operations.
 *
 * `SynchronizedObject` from `kotlinx-atomicfu` library was used before.
 * However, it is still [experimental](https://github.com/Kotlin/kotlinx-atomicfu?tab=readme-ov-file#locks)
 * and has [a performance problem](https://github.com/Kotlin/kotlinx-atomicfu/issues/412)
 * that seriously affects Compose.
 *
 * This implementation is optimized for a non-contention case
 * (that is the case for the current state of Compose for iOS), so it does not create a posix mutex
 * when there is no contention: using a posix mutex has its own performance overheads.
 * On the other hand, it does not just spin lock in case of contention,
 * protecting from an occasional battery drain.
 */
internal actual class SynchronizedObject actual constructor() {

    companion object {
        private const val NO_OWNER = -1L
    }

    private val owner: AtomicLong = atomic(NO_OWNER)
    private var reEnterCount: Int = 0
    private val waiters: AtomicInt = atomic(0)

    private val monitorWrapper: MonitorWrapper by lazy { MonitorWrapper() }
    private val monitor: NativeMonitor get() = monitorWrapper.monitor

    fun lock() {
        if (owner.value == currentThreadId()) {
            reEnterCount += 1
        } else if (waiters.incrementAndGet() > 1) {
            waitForUnlockAndLock()
        } else {
            if (!owner.compareAndSet(NO_OWNER, currentThreadId())) {
                waitForUnlockAndLock()
            }
        }
    }

    private fun waitForUnlockAndLock() {
        withMonitor(monitor) {
            while (!owner.compareAndSet(NO_OWNER, currentThreadId())) {
                wait()
            }
        }
    }

    fun unlock() {
        require (owner.value == currentThreadId())
        if (reEnterCount > 0) {
            reEnterCount -= 1
        } else {
            owner.value = NO_OWNER
            if (waiters.decrementAndGet() > 0) {
                withMonitor(monitor) {
                    notify()
                }
            }
        }
    }

    private inline fun withMonitor(monitor: NativeMonitor, block: NativeMonitor.() -> Unit) {
        monitor.run {
            enter()
            return try {
                block()
            } finally {
                exit()
            }
        }
    }

    private class MonitorWrapper {
        val monitor: NativeMonitor = NativeMonitor()
        val cleaner = createCleaner(monitor, NativeMonitor::dispose)
    }

    @OptIn(ExperimentalForeignApi::class)
    private class NativeMonitor {
        private val arena: Arena = Arena()
        private val cond: pthread_cond_t = arena.alloc()
        private val mutex: pthread_mutex_t = arena.alloc()
        private val attr: pthread_mutexattr_t = arena.alloc()

        init {
            require (pthread_cond_init(cond.ptr, null) == 0)
            require(pthread_mutexattr_init(attr.ptr) == 0)
            require (pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_ERRORCHECK) == 0)
            require(pthread_mutex_init(mutex.ptr, attr.ptr) == 0)
        }

        fun enter() = require(pthread_mutex_lock(mutex.ptr) == 0)

        fun exit() = require(pthread_mutex_unlock(mutex.ptr) == 0)

        fun wait() = require(pthread_cond_wait(cond.ptr, mutex.ptr) == 0)

        fun notify() = require (pthread_cond_signal(cond.ptr) == 0)

        fun dispose() {
            pthread_cond_destroy(cond.ptr)
            pthread_mutex_destroy(mutex.ptr)
            pthread_mutexattr_destroy(attr.ptr)
            arena.clear()
        }
    }
}

@PublishedApi
@Suppress("NON_PUBLIC_CALL_FROM_PUBLIC_INLINE")
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R {
    lock.run {
        lock()
        return try {
            block()
        } finally {
            unlock()
        }
    }
}