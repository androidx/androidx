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

// @file:OptIn(ExperimentalForeignApi::class)

package androidx.paging.internal

import kotlinx.atomicfu.AtomicBoolean as AtomicFuAtomicBoolean
import kotlinx.atomicfu.AtomicInt as AtomicFuAtomicInt
import kotlinx.atomicfu.atomic

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
    private val lock = SynchronizedLock()

    actual override fun iterator(): Iterator<T> {
        return data.iterator()
    }

    actual fun add(value: T) =
        lock.withLock {
            data = data + value
            true
        }

    actual fun remove(value: T): Boolean =
        lock.withLock {
            val newList = data.toMutableList()
            val result = newList.remove(value)
            data = newList
            result
        }
}
