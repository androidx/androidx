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

@file:Suppress("NOTHING_TO_INLINE", "KotlinRedundantDiagnosticSuppress")

package androidx.compose.runtime.collection

import kotlin.jvm.JvmField
import kotlin.jvm.JvmInline
import kotlin.math.min

@JvmInline
internal value class Stack<T>(private val backing: ArrayList<T> = ArrayList()) {
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

internal class IntStack {
    @JvmField internal var slots = IntArray(10)
    @JvmField internal var tos = 0

    inline val size: Int
        get() = tos

    private fun resize(): IntArray {
        val copy = slots.copyOf(slots.size * 2)
        slots = copy
        return copy
    }

    fun push(value: Int) {
        var slots = slots
        if (tos >= slots.size) {
            slots = resize()
        }
        slots[tos++] = value
    }

    fun pop(): Int = slots[--tos]

    fun peekOr(default: Int): Int {
        val index = tos - 1
        return if (index >= 0) slots[index] else default
    }

    fun peek() = slots[tos - 1]

    fun peek2() = slots[tos - 2]

    fun peek(index: Int) = slots[index]

    inline fun isEmpty() = tos == 0

    inline fun isNotEmpty() = tos != 0

    fun clear() {
        tos = 0
    }

    fun indexOf(value: Int): Int {
        val slots = slots
        val end = min(slots.size, tos)
        for (i in 0 until end) {
            if (slots[i] == value) return i
        }
        return -1
    }
}
