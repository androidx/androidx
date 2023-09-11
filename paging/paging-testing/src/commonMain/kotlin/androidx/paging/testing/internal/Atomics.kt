/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.paging.testing.internal

internal expect class AtomicInt(initialValue: Int) {
    fun get(): Int
    fun set(value: Int)
}

internal expect class AtomicBoolean(initialValue: Boolean) {
    fun get(): Boolean
    fun set(value: Boolean)
    fun compareAndSet(expect: Boolean, update: Boolean): Boolean
}

internal expect class AtomicRef<T>(initialValue: T) {
    fun get(): T
    fun set(value: T)
    fun getAndSet(value: T): T
}
