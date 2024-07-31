/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry.internal

import androidx.annotation.RestrictTo
import kotlin.reflect.KProperty

/**
 * Allows more convenient lambda syntax for declaring and initializing a [ThreadLocal]. Use with
 * `by` to treat it as a delegate and access its value implicitly.
 *
 * Example:
 * ```
 * val foo by threadLocal { MutableVec(...) }
 * foo.x = 5F
 * foo.y = 6F
 * ```
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun <T> threadLocal(initialValueProvider: () -> T): ThreadLocal<T> =
    object : ThreadLocal<T>() {
        override fun initialValue(): T = initialValueProvider()
    }

/**
 * Allows a [ThreadLocal] to act as a delegate, so a `ThreadLocal<T>` can act in code like a simple
 * `T` object. This method doesn't need to be called explicitly, as it is an operator for access.
 * See [threadLocal] for easier syntax for declaration and initialization, as well as for examples.
 */
@Suppress("NOTHING_TO_INLINE")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public inline operator fun <T> ThreadLocal<T>.getValue(thisObj: Any?, property: KProperty<*>): T =
    get()!!
