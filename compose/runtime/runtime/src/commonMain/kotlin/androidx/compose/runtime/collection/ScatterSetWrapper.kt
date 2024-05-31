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

package androidx.compose.runtime.collection

import androidx.collection.ScatterSet

/**
 * A wrapper for [ScatterSet] that implements [Set] APIs. This wrapper allows to use [ScatterSet]
 * through external APIs and unwrap it back into [Set] for faster iteration / other operations.
 */
internal class ScatterSetWrapper<T>(internal val set: ScatterSet<T>) : Set<T> {
    override val size: Int
        get() = set.size

    override fun isEmpty(): Boolean = set.isEmpty()

    override fun iterator(): Iterator<T> = iterator { set.forEach { yield(it) } }

    override fun containsAll(elements: Collection<T>): Boolean = elements.all { set.contains(it) }

    override fun contains(element: T): Boolean = set.contains(element)
}

internal fun <T> ScatterSet<T>.wrapIntoSet(): Set<T> = ScatterSetWrapper(this)

internal inline fun <T : Any> Set<T>.fastForEach(block: (T) -> Unit) =
    when (this) {
        is ScatterSetWrapper<T> -> {
            set.forEach(block)
        }
        else -> {
            forEach(block)
        }
    }

internal inline fun Set<Any>.fastAny(block: (Any) -> Boolean) =
    if (this is ScatterSetWrapper<Any>) {
        set.any(block)
    } else {
        any(block)
    }
