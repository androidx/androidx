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

package androidx.room3.concurrent

import kotlin.concurrent.atomics.AtomicInt as KotlinAtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndDecrement
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.concurrent.atomics.incrementAndFetch

@OptIn(ExperimentalAtomicApi::class)
public actual class AtomicInt actual constructor(initialValue: Int) {
    private val delegate: KotlinAtomicInt = KotlinAtomicInt(initialValue)

    public actual fun get(): Int = delegate.load()

    public actual fun set(value: Int) {
        delegate.store(value)
    }

    public actual fun compareAndSet(expect: Int, update: Int): Boolean =
        delegate.compareAndSet(expect, update)

    public actual fun incrementAndGet(): Int = delegate.incrementAndFetch()

    public actual fun getAndIncrement(): Int = delegate.fetchAndIncrement()

    public actual fun decrementAndGet(): Int = delegate.fetchAndDecrement()
}

@OptIn(ExperimentalAtomicApi::class)
public actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    private val delegate: KotlinAtomicInt = KotlinAtomicInt(toInt(initialValue))

    public actual fun get(): Boolean = delegate.load() == 1

    public actual fun compareAndSet(expect: Boolean, update: Boolean): Boolean =
        delegate.compareAndSet(toInt(expect), toInt(update))

    private fun toInt(value: Boolean) = if (value) 1 else 0
}
