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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.datastore.core

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// TODO(b/442842414): Replace with `kotlinx.io.IOException` when it is stable.
actual open class IOException actual constructor(message: String?, cause: Throwable?) :
    Exception() {
    actual constructor(message: String?) : this(message, null)
}

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

// TODO(b/441511612): Support locking for LocalStorage and OPFS.
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

internal actual fun ioDispatcher(): CoroutineDispatcher {
    return Dispatchers.Default
}
