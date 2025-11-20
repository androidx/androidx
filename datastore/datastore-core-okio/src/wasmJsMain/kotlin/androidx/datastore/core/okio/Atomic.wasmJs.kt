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

package androidx.datastore.core.okio

// TODO(b/441511612): Support locking for LocalStorage and OPFS.
internal actual class AtomicInt {

    private var initialValue: Int

    actual constructor(initialValue: Int) {
        this.initialValue = initialValue
    }

    actual fun getAndIncrement(): Int {
        val originalValue = initialValue
        initialValue++
        return originalValue
    }

    actual fun incrementAndGet(): Int {
        initialValue++
        return initialValue
    }

    actual fun decrementAndGet(): Int {
        initialValue--
        return initialValue
    }

    actual fun get(): Int {
        return initialValue
    }
}

internal actual class AtomicBoolean {
    private var initialValue: Boolean

    actual constructor(initialValue: Boolean) {
        this.initialValue = initialValue
    }

    actual fun get(): Boolean {
        return initialValue
    }

    actual fun set(value: Boolean) {
        this.initialValue = value
    }
}

internal actual class Synchronizer actual constructor() {
    /** No lock needed for WASM/JS since it is Single-Threaded by design. */
    actual inline fun <T> withLock(crossinline block: () -> T): T {
        return block()
    }
}
