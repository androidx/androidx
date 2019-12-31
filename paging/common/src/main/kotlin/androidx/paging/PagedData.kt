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

package androidx.paging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Container for Paged data from a single generation of loads.
 *
 * Each refresh of data (generally either pushed by local storage, or pulled from the network)
 * will have a separate corresponding [PagedData].
 */
class PagedData<T : Any> internal constructor(
    internal val flow: Flow<PageEvent<T>>,
    internal val receiver: UiReceiver
) {
    private inline fun <R : Any> transform(crossinline transform: (PageEvent<T>) -> PageEvent<R>) =
        PagedData(
            flow = flow.map { transform(it) },
            receiver = receiver
        )

    /**
     * Returns a PagedData containing the result of applying the given [transform] to each
     * element, as it is loaded.
     */
    fun <R : Any> map(transform: (T) -> R): PagedData<R> = transform { it.map(transform) }

    /**
     * Returns a PagedData of all elements returned from applying the given [transform]
     * to each element, as it is loaded.
     */
    fun <R : Any> flatMap(transform: (T) -> Iterable<R>): PagedData<R> =
        transform { it.flatMap(transform) }

    /**
     * Returns a PagedData containing only elements matching the given [predicate]
     */
    fun filter(predicate: (T) -> Boolean): PagedData<T> = transform { it.filter(predicate) }

    /**
     * Returns a PagedData containing each original element, with an optional separator generated
     * by [generator], given the elements before and after (or null, in boundary conditions).
     *
     * For example, to create letter separators in an alphabetically sorted list:
     *
     * ```
     * flow.insertSeparators { before: String?, after: String? ->
     *     if (before == null || before.get(0) != after?.get(0) ?: null) {
     *         // separator - after is first item with its first letter
     *         after.get(0).toUpperCase().toString()
     *     } else {
     *         // no separator - first letters of before/after are the same
     *         null
     *     }
     * }
     * ```
     *
     * This transformation would make the example data set:
     *
     *     "apple", "apricot", "banana", "carrot"
     *
     * Become:
     *
     *     "A", "apple", "apricot", "B", "banana", "C", "carrot"
     *
     * Note that this transform is applied asynchronously, as pages are loaded. Potential
     * separators between pages are only computed once both pages are loaded.
     */
    fun <R : T> insertSeparators(
        generator: (T?, T?) -> R?
    ) = PagedData(
        flow = flow.insertSeparators(generator),
        receiver = receiver
    )

    /**
     * Returns a PagedData containing each original element, with the passed header [item] added to
     * the start of the list.
     *
     * The header [item] is added to a loaded page which marks the end of the data stream in the
     * prepend direction by returning null in [PagedSource.LoadResult.Page.prevKey]. It will be
     * removed if the first page in the list is dropped, which can happen in the case of loaded
     * pages exceeding [PagedList.Config.maxSize].
     *
     * Note: This operation is not idempotent, calling it multiple times will continually add
     * more headers to the start of the list, which can be useful if multiple header items are
     * required.
     */
    fun addHeader(item: T) = PagedData(flow.addHeader(item), receiver)

    /**
     * Returns a PagedData containing each original element, with the passed footer [item] added to
     * the end of the list.
     *
     * The footer [item] is added to a loaded page which marks the end of the data stream in the
     * append direction, either by returning null in [PagedSource.LoadResult.Page.nextKey]. It
     * will be removed if the first page in the list is dropped, which can happen in the case of
     * loaded* pages exceeding [PagedList.Config.maxSize].
     *
     * Note: This operation is not idempotent, calling it multiple times will continually add
     * more footer to the end of the list, which can be useful if multiple footer items are
     * required.
     */
    fun addFooter(item: T) = PagedData(flow.addFooter(item), receiver)
}

internal interface UiReceiver {
    fun addHint(hint: ViewportHint)
    fun retry()
}