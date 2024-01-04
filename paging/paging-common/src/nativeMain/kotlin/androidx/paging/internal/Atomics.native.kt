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

import kotlin.native.internal.createCleaner
import kotlinx.atomicfu.AtomicBoolean as AtomicFuAtomicBoolean
import kotlinx.atomicfu.AtomicInt as AtomicFuAtomicInt
import kotlinx.atomicfu.atomic
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
 * Wrapper for platform.posix.PTHREAD_MUTEX_RECURSIVE which
 * is represented as kotlin.Int on darwin platforms and kotlin.UInt on linuxX64
 * See: // https://youtrack.jetbrains.com/issue/KT-41509
 */
internal expect val PTHREAD_MUTEX_RECURSIVE: Int

@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual class ReentrantLock actual constructor() {

    private val resources = Resources()

    @Suppress("unused") // The returned Cleaner must be assigned to a property
    @ExperimentalStdlibApi
    private val cleaner = createCleaner(resources, Resources::destroy)

    actual fun lock() {
        pthread_mutex_lock(resources.mutex.ptr)
    }

    actual fun unlock() {
        pthread_mutex_unlock(resources.mutex.ptr)
    }

    private class Resources {
        private val arena = Arena()
        private val attr: pthread_mutexattr_t = arena.alloc()
        val mutex: pthread_mutex_t = arena.alloc()

        init {
            pthread_mutexattr_init(attr.ptr)
            pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_RECURSIVE)
            pthread_mutex_init(mutex.ptr, attr.ptr)
        }

        fun destroy() {
            pthread_mutex_destroy(mutex.ptr)
            pthread_mutexattr_destroy(attr.ptr)
            arena.clear()
        }
    }
}

internal actual class AtomicInt actual constructor(initialValue: Int) {
    private var delegate: AtomicFuAtomicInt = atomic(initialValue)
    private var property by delegate

    actual fun getAndIncrement(): Int {
        return delegate.getAndIncrement()
    }

    actual fun decrementAndGet(): Int {
        return delegate.decrementAndGet()
    }

    actual fun get(): Int = property

    actual fun incrementAndGet(): Int {
        return delegate.incrementAndGet()
    }
}

internal actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    private var delegate: AtomicFuAtomicBoolean = atomic(initialValue)
    private var property by delegate

    actual fun get(): Boolean = property

    actual fun set(value: Boolean) {
        property = value
    }

    actual fun compareAndSet(expect: Boolean, update: Boolean): Boolean {
        return delegate.compareAndSet(expect, update)
    }
}

internal actual class CopyOnWriteArrayList<T> : Iterable<T> {
    private var data: List<T> = emptyList()
    private val lock = ReentrantLock()
    override fun iterator(): Iterator<T> {
        return data.iterator()
    }

    actual fun add(value: T) = lock.withLock {
        data = data + value
        true
    }

    actual fun remove(value: T): Boolean = lock.withLock {
        val newList = data.toMutableList()
        val result = newList.remove(value)
        data = newList
        result
    }
}
