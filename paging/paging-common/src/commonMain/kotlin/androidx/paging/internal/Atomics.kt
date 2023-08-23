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

package androidx.paging.internal

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal expect class CopyOnWriteArrayList<T>() : Iterable<T> {
    fun add(value: T): Boolean
    fun remove(value: T): Boolean
}

internal expect class ReentrantLock constructor() {
    fun lock()
    fun unlock()
}

internal expect class AtomicInt {
    constructor(initialValue: Int)

    fun getAndIncrement(): Int
    fun incrementAndGet(): Int
    fun decrementAndGet(): Int
    fun get(): Int
}

internal expect class AtomicBoolean {
    constructor(initialValue: Boolean)

    fun get(): Boolean
    fun set(value: Boolean)
    fun compareAndSet(expect: Boolean, update: Boolean): Boolean
}

@OptIn(ExperimentalContracts::class)
@Suppress("BanInlineOptIn") // b/296638070
internal inline fun <T> ReentrantLock.withLock(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    try {
        lock()
        return block()
    } finally {
        unlock()
    }
}
