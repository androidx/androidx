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

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// TODO implement properly
internal actual class AtomicInt {
    private var delegate: Int
    actual constructor(initialValue: Int) {
        delegate = initialValue
    }

    actual fun getAndIncrement(): Int = delegate++
    actual fun decrementAndGet(): Int = --delegate
    actual fun get(): Int = delegate
    actual fun incrementAndGet(): Int = ++delegate
}

public actual open class IOException actual constructor(message: String?, cause: Throwable?) : Exception() {
   actual constructor(message: String?) : this(message, null)
}

//TODO: Find a good IO Dispatcher
internal actual fun ioDispatcher(): CoroutineDispatcher = Dispatchers.Default