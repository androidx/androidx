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

package androidx.compose.runtime.internal

import kotlin.jvm.JvmInline

internal expect class AtomicReference<V>(value: V) {
    fun get(): V

    fun set(value: V)

    fun getAndSet(value: V): V

    fun compareAndSet(expect: V, newValue: V): Boolean
}

internal expect class AtomicInt(value: Int) {
    fun get(): Int

    fun set(value: Int)

    fun add(amount: Int): Int

    fun compareAndSet(expect: Int, newValue: Int): Boolean
}

@JvmInline
internal value class AtomicBoolean(private val wrapped: AtomicInt = AtomicInt(0)) {

    constructor(value: Boolean) : this(AtomicInt(if (value) 1 else 0))

    fun get(): Boolean = wrapped.get() != 0

    fun set(value: Boolean) = wrapped.set(if (value) 1 else 0)

    fun getAndSet(newValue: Boolean): Boolean = wrapped.compareAndSet(1, if (newValue) 1 else 0)
}
