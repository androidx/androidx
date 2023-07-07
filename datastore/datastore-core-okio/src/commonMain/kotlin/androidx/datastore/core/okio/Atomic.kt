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
}

/**
 * Simple class to provide synchronization blocks.
 *
 * On JVM/ART, this uses simple JDK's synchronization.
 * On other platforms, it uses atomic-fu.
 *
 * @see withLock
 */
internal expect class Synchronizer() {
    inline fun <T> withLock(crossinline block: () -> T): T
}
