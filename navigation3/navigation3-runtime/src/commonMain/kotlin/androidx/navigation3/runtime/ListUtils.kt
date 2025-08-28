/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigation3.runtime

import androidx.collection.MutableScatterSet
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/** Internal util to use compose ui's fastX iterator apis whenever possible */
internal inline fun <T, R> List<T>.fastMapOrMap(transform: (T) -> R) =
    if (this is RandomAccess) {
        this.fastMap(transform)
    } else {
        @Suppress("ListIterator") this.map(transform)
    }

internal inline fun <T> List<T>.fastForEachReversedOrForEachReversed(action: (T) -> Unit) {
    if (this is RandomAccess) {
        this.fastForEachReversed(action)
    } else {
        @Suppress("ListIterator") this.reversed().forEach(action)
    }
}

internal inline fun <T> List<T>.fastForEachOrForEach(action: (T) -> Unit) {
    if (this is RandomAccess) {
        this.fastForEach(action)
    } else {
        @Suppress("ListIterator") this.forEach(action)
    }
}

internal fun <T> List<T>.fastDistinctOrDistinct(): List<T> =
    if (this is RandomAccess) {
        this.fastDistinctBy { it }
    } else {
        @Suppress("ListIterator") this.distinct()
    }

/** Helpers copied from compose:ui:ui-util to prevent adding dep on ui-util */
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastForEach(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices) {
        val item = get(index)
        action(item)
    }
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T, R> List<T>.fastMap(transform: (T) -> R): List<R> {
    contract { callsInPlace(transform) }
    val target = ArrayList<R>(size)
    fastForEach { target += transform(it) }
    return target
}

@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastForEachReversed(action: (T) -> Unit) {
    contract { callsInPlace(action) }
    for (index in indices.reversed()) {
        val item = get(index)
        action(item)
    }
}

@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
private inline fun <T, K> List<T>.fastDistinctBy(selector: (T) -> K): List<T> {
    contract { callsInPlace(selector) }
    val set = MutableScatterSet<K>(size)
    val target = ArrayList<T>(size)
    fastForEach { e ->
        val key = selector(e)
        if (set.add(key)) target += e
    }
    return target
}
