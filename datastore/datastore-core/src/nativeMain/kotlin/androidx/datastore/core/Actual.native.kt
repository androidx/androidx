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

package androidx.datastore.core

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Common IOException mapped to a custom exception class in native code.
 */
public actual open class IOException actual constructor(message: String?, cause: Throwable?) :
    Exception(message, cause) {
    actual constructor(message: String?) : this(message, null)
}

internal actual class AtomicInt actual constructor(initialValue: Int) {
    private var delegate: kotlinx.atomicfu.AtomicInt = atomic(initialValue)
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
    private var delegate: kotlinx.atomicfu.AtomicBoolean = atomic(initialValue)
    private var property by delegate

    actual fun get(): Boolean = property

    actual fun set(value: Boolean) {
        property = value
    }
}

internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
