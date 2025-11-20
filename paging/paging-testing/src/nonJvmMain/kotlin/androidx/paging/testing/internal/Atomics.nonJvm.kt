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

package androidx.paging.testing.internal

import kotlinx.atomicfu.AtomicBoolean as AtomicFuAtomicBoolean
import kotlinx.atomicfu.AtomicInt as AtomicFuAtomicInt
import kotlinx.atomicfu.AtomicRef as AtomicFuAtomicRef
import kotlinx.atomicfu.atomic

internal actual class AtomicInt actual constructor(initialValue: Int) {
    private var delegate: AtomicFuAtomicInt = atomic(initialValue)
    private var property by delegate

    actual fun get(): Int = property

    actual fun set(value: Int) {
        property = value
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

internal actual class AtomicRef<T> actual constructor(initialValue: T) {
    private var delegate: AtomicFuAtomicRef<T> = atomic(initialValue)
    private var property by delegate

    actual fun get(): T = property

    actual fun set(value: T) {
        property = value
    }

    actual fun getAndSet(value: T): T {
        return delegate.getAndSet(value)
    }
}
