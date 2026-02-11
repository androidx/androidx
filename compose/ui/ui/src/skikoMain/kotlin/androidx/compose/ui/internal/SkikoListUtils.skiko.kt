/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.internal

import androidx.compose.ui.util.fastForEachIndexed
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Applies the given [transform] function to each element and its index in the original list,
 * appending only the non-null results to the specified [destination] list.
 *
 * **Do not use for collections that come from public APIs**, since they may not support [kotlin.collections.RandomAccess]
 * in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access ([kotlin.collections.ArrayList]).
 */
//fastMapIndexedNotNullTo
@OptIn(ExperimentalContracts::class)
internal inline fun <T, R : Any, C : MutableList<in R>> List<T>.fastMapIndexedNotNullTo(
    destination: C,
    transform: (index: Int, T) -> R?
): C {
    contract { callsInPlace(transform) }
    fastForEachIndexed { index, element -> transform(index, element)?.let { destination.add(it) } }
    return destination
}

//TODO: temporary overload until https://r.android.com/3936572 is merged
/**
 * Returns the index of the first element in the list that satisfies the given [operation]. If no
 * element matches the specified condition, -1 is returned.
 *
 * **Do not use for collections that come from public APIs**, since they may not support [kotlin.collections.RandomAccess]
 * in an efficient way, and this method may actually be a lot slower. Only use for
 * collections that are created by code we control and are known to support random access ([kotlin.collections.ArrayList]).
 */
@OptIn(ExperimentalContracts::class)
internal inline fun <T> List<T>.fastIndexOfFirst(operation: (acc: T) -> Boolean): Int {
    contract { callsInPlace(operation) }
    fastForEachIndexed { index, t -> if (operation(t)) return index }
    return -1
}