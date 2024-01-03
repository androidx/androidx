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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.datastore.core

import androidx.annotation.RestrictTo
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Common IOException mapped to java.io.Exception in jvm code.
 */
actual typealias IOException = java.io.IOException

internal actual class AtomicInt {
    private val delegate: AtomicInteger
    actual constructor(initialValue: Int) {
        delegate = AtomicInteger(initialValue)
    }

    actual fun getAndIncrement(): Int = delegate.getAndIncrement()
    actual fun decrementAndGet(): Int = delegate.decrementAndGet()
    actual fun get(): Int = delegate.get()
    actual fun incrementAndGet(): Int = delegate.incrementAndGet()
}

internal actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    private val delegate: java.util.concurrent.atomic.AtomicBoolean = AtomicBoolean(initialValue)

    actual fun get(): Boolean = delegate.get()

    actual fun set(value: Boolean) {
        delegate.set(value)
    }
}

internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
