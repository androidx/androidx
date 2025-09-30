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

package androidx.navigation3

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/** Internal util to use compose ui's fastX iterator apis whenever possible */
internal fun <T> List<T>.fastAnyOrAny(predicate: (T) -> Boolean): Boolean =
    if (this is RandomAccess) {
        this.fastAny(predicate)
    } else {
        @Suppress("ListIterator") this.any(predicate)
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

@Suppress("BanInlineOptIn") // Treat Kotlin Contracts as non-experimental.
@OptIn(ExperimentalContracts::class)
private inline fun <T> List<T>.fastAny(predicate: (T) -> Boolean): Boolean {
    contract { callsInPlace(predicate) }
    fastForEach { if (predicate(it)) return true }
    return false
}
