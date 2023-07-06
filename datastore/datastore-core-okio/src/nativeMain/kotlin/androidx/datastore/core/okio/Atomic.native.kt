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

package androidx.datastore.core.okio

import kotlinx.atomicfu.AtomicBoolean as AtomicFuAtomicBoolean
import kotlinx.atomicfu.AtomicInt as AtomicFuAtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

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
}

internal actual class Synchronizer {
    /**
     * This is public to allow inlining withLock. Since we use it from common, for all
     * intents and purposes, delegate is not visible. So it is cheaper to do this instead
     * of forcing an object creation to call withLock.
     */
    val delegate = SynchronizedObject()
    actual inline fun<T> withLock(crossinline block: () -> T): T {
        return synchronized(delegate, block)
    }
}
