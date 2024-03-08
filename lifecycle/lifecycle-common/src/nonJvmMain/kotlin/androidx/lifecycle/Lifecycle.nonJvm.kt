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
package androidx.lifecycle

import androidx.annotation.RestrictTo
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public actual class AtomicReference<V> actual constructor(value: V) {
    private val delegate = atomic(value)
    public actual fun get(): V = delegate.value
    public actual fun compareAndSet(expect: V, newValue: V): Boolean =
        delegate.compareAndSet(expect, newValue)
}

public actual abstract class LifecycleCoroutineScope internal actual constructor() :
    CoroutineScope {
    internal actual abstract val lifecycle: Lifecycle
}
