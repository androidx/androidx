/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.ink.nativeloader

import androidx.annotation.RestrictTo
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Delegate for native pointers, cleans up the pointer when it goes out of scope, allocates the
 * pointer on initialization after cleanup is set up. This is needed for cleanup in KMP because
 * Kotlin-native does not support finalize().
 *
 * ```
 * val nativePointer: Long by NativePointer(FooNative::create, FooNative::free)
 * ```
 *
 * @param pointerAlloc The function to allocate the native pointer, used during initialization and
 *   discarded after use. Must not return 0 (representing native `nullptr`), in that case it should
 *   throw an exception instead.
 * @param pointerFree The function to free the native pointer. Must not rely on being called from a
 *   particular thread. Must not directly or indirectly reference the [NativePointer] instance,
 *   which generally means it must not capture any reference to objects the pointer is stored on.
 *   One way to accomplish that is to use a reference to a method on the singleton object that
 *   manages the native interface.
 * @throws IllegalStateException if [pointerAlloc] returns 0.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // NonPublicApi
expect public class NativePointer(
    pointerAlloc: () -> Long, // Must not be retained
    pointerFree: (Long) -> Unit, // Must not capture a reference to this instance
) : ReadOnlyProperty<Any, Long> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Long
}
