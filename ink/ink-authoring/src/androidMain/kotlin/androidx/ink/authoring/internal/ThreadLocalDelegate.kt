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

package androidx.ink.authoring.internal

import kotlin.reflect.KProperty

/**
 * [ThreadLocal] subclass that can be used as a read-only delegate with the `by` operator.
 *
 * Example:
 * ```
 * val foo by threadLocal { MutableVec(...) }
 * foo.x = 5F
 * foo.y = 6F
 * ```
 */
internal fun <T> threadLocal(initialValueProvider: () -> T): ThreadLocalDelegate<T> =
    ThreadLocalDelegate(initialValueProvider)

internal class ThreadLocalDelegate<T> constructor(private val initialValueProvider: () -> T) :
    ThreadLocal<T>() {
    override fun initialValue(): T = initialValueProvider()

    @Suppress("NOTHING_TO_INLINE")
    public inline operator fun getValue(thisObj: Any?, property: KProperty<*>): T = get()!!
}
