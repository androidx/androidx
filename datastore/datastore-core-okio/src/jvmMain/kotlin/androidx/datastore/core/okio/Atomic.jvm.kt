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

import java.util.concurrent.atomic.AtomicBoolean as JavaAtomicBoolean
import java.util.concurrent.atomic.AtomicInteger as JavaAtomicInteger

internal actual class AtomicInt {
    private val delegate: JavaAtomicInteger
    actual constructor(initialValue: Int) {
        delegate = JavaAtomicInteger(initialValue)
    }

    actual fun getAndIncrement(): Int = delegate.getAndIncrement()
    actual fun decrementAndGet(): Int = delegate.decrementAndGet()
    actual fun get(): Int = delegate.get()
    actual fun incrementAndGet(): Int = delegate.incrementAndGet()
}

internal actual class AtomicBoolean actual constructor(initialValue: Boolean) {
    private val delegate = JavaAtomicBoolean(initialValue)

    actual fun get(): Boolean = delegate.get()

    actual fun set(value: Boolean) {
        delegate.set(value)
    }
}

internal actual class Synchronizer {
    actual inline fun<T> withLock(block: () -> T): T {
        // technically, it is wrong to sync on `this` but we are only using it from common
        // code hence there is no way to access JVM/ART's sync; so I decided to be cheap here
        // and avoid another object.
        return synchronized(this) {
            block()
        }
    }
}
