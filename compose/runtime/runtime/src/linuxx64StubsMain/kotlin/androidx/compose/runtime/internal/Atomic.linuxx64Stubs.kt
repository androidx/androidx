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

import androidx.compose.runtime.implementedInJetBrainsFork

internal actual class AtomicReference<V> actual constructor(value: V) {
    actual fun get(): V = implementedInJetBrainsFork()

    actual fun set(value: V): Unit = implementedInJetBrainsFork()

    actual fun getAndSet(value: V): V = implementedInJetBrainsFork()

    actual fun compareAndSet(expect: V, newValue: V): Boolean = implementedInJetBrainsFork()
}

internal actual class AtomicInt actual constructor(value: Int) {
    actual fun get(): Int = implementedInJetBrainsFork()

    actual fun set(value: Int): Unit = implementedInJetBrainsFork()

    actual fun compareAndSet(expect: Int, newValue: Int): Boolean = implementedInJetBrainsFork()

    actual fun add(amount: Int): Int = implementedInJetBrainsFork()
}
