/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

internal actual class AtomicReference<V> actual constructor(value: V) {
    actual fun get(): V = implementedInJetBrainsFork()

    actual fun set(value: V) {
        implementedInJetBrainsFork()
    }

    actual fun getAndSet(value: V): V = implementedInJetBrainsFork()

    actual fun compareAndSet(expect: V, newValue: V): Boolean = implementedInJetBrainsFork()
}

internal actual class AtomicLong actual constructor(value: Long) {
    actual fun get(): Long = implementedInJetBrainsFork()

    actual fun set(value: Long) {
        implementedInJetBrainsFork()
    }

    actual fun getAndIncrement(): Long = implementedInJetBrainsFork()
}
