/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.runtime

import androidx.collection.MutableIntList
import androidx.collection.MutableObjectList
import androidx.collection.mutableIntListOf
import androidx.collection.mutableObjectListOf
import kotlin.jvm.JvmInline

@JvmInline
internal value class Stack<T>(private val backing: MutableObjectList<T> = mutableObjectListOf()) {
    val size: Int
        get() = backing.size

    fun push(value: T) = backing.add(value)

    fun pop(): T = backing.removeAt(size - 1)

    fun peek(): T = backing.get(size - 1)

    fun peek(index: Int): T = backing.get(index)

    fun isEmpty() = backing.isEmpty()

    fun isNotEmpty() = !isEmpty()

    fun clear() = backing.clear()

    @Suppress("UNCHECKED_CAST")
    fun toArray(): Array<T> = Array<Any?>(backing.size) { backing[it] } as Array<T>
}

@JvmInline
internal value class IntStack(private val slots: MutableIntList = mutableIntListOf()) {
    val size: Int
        get() = slots.size

    fun push(value: Int) = slots.add(value)

    fun pop(): Int = slots.removeAt(size - 1)

    fun peekOr(default: Int): Int = if (size > 0) peek() else default

    fun peek() = slots[size - 1]

    fun peek2() = slots[size - 2]

    fun peek(index: Int) = slots[index]

    fun isEmpty() = slots.isEmpty()

    fun isNotEmpty() = slots.isNotEmpty()

    fun clear() = slots.clear()

    fun indexOf(value: Int) = slots.indexOf(value)
}
