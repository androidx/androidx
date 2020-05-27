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

import androidx.paging.LoadType.APPEND
import androidx.paging.LoadType.PREPEND
import androidx.paging.PageEvent.Drop
import androidx.paging.PageEvent.Insert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Create a TransformablePage with separators inside (ignoring edges)
 *
 * Separators between pages are handled outside of the page, see `Flow<PageEvent>.insertSeparators`.
 */
internal fun <R : Any, T : R> TransformablePage<T>.insertInternalSeparators(
    generator: (T?, T?) -> R?
): TransformablePage<R> {
    if (data.isEmpty()) {
        @Suppress("UNCHECKED_CAST")
        return this as TransformablePage<R>
    }

    val initialCapacity = data.size + 4 // extra space to avoid bigger allocations
    val outputList = ArrayList<R>(initialCapacity)
    val outputIndices = ArrayList<Int>(initialCapacity)

    outputList.add(data.first())
    outputIndices.add(originalIndices?.first() ?: 0)
    for (i in 1 until data.size) {
        val item = data[i]
        val separator = generator(data[i - 1], item)
        if (separator != null) {
            outputList.add(separator)
            outputIndices.add(i)
        }
        outputList.add(item)
        outputIndices.add(i)
    }
    return if (outputList.size == data.size) {
        /*
         * If we inserted no separators, just use original page.
         *
         * This isn't a particularly important optimization, but it does make tests easier to
         * write, since Insert event coming in is unchanged
         */
        @Suppress("UNCHECKED_CAST")
        this as TransformablePage<R>
    } else {
        TransformablePage(
            originalPageOffset = originalPageOffset,
            data = outputList,
            originalPageSize = originalPageSize,
            originalIndices = outputIndices
        )
    }
}

/**
 * Create a TransformablePage with the given separator (or empty, if the separator is null)
 */
internal fun <T : Any> separatorPage(
    separator: T?,
    originalPageOffset: Int,
    originalPageSize: Int,
    originalIndex: Int
): TransformablePage<T> = if (separator != null) {
    // page with just the separator
    TransformablePage(
        originalPageOffset = originalPageOffset,
        data = listOf(separator),
        originalPageSize = originalPageSize,
        originalIndices = listOf(originalIndex)
    )
} else {
    // empty page
    TransformablePage(
        originalPageOffset = originalPageOffset,
        data = emptyList(),
        originalPageSize = originalPageSize,
        originalIndices = null
    )
}

/**
 * Create a TransformablePage with the given separator (or empty, if the separator is null)
 */
internal fun <R : Any, T : R> separatorPage(
    separator: R?,
    adjacentPage: TransformablePage<T>,
    originalIndex: Int
): TransformablePage<R> = separatorPage(
    separator = separator,
    originalPageOffset = adjacentPage.originalPageOffset,
    originalPageSize = adjacentPage.originalPageSize,
    originalIndex = originalIndex
)

/**
 * Create a TransformablePage with the given separator (or empty, if the separator is null)
 */
internal fun <R : Any, T : R> separatorPage(
    separator: R?,
    adjacentPage: DataPage<T>,
    originalIndex: Int
): TransformablePage<R> = separatorPage(
    separator = separator,
    originalPageOffset = adjacentPage.originalPageOffset,
    originalPageSize = adjacentPage.originalPageSize,
    originalIndex = originalIndex
)

/**
 * Per-page adjacency info - used to create adjacent separators.
 */
internal class DataPage<T : Any>(
    page: TransformablePage<T>
) {
    val first: T = page.data.first()
    val last: T = page.data.last()
    val originalPageOffset: Int = page.originalPageOffset
    val originalPageSize: Int = page.originalPageSize
    val originalLastIndex
        get() = originalPageSize - 1
}

/**
 * Iterate through the list of page info, dropping events, and mapping the incoming drop count to
 * an output count.
 */
private inline fun <T : Any> MutableList<DataPage<T>?>.dropPages(
    nonSeparatorCount: Int,
    indexProvider: (List<DataPage<T>?>) -> Int
): Int {
    if (isEmpty()) {
        // nothing to drop
        check(nonSeparatorCount == 0)
        return 0
    }

    // drop nonSeparatorCount of pages. Even if 0, we want to be sure to drop a terminal
    // separator, since that side of the list is no longer done
    var nonSeparatorPagesToDrop = nonSeparatorCount
    var outputDropCount = 0
    while (nonSeparatorPagesToDrop > 0) {
        val page = removeAt(indexProvider(this))
        if (page != null) {
            nonSeparatorPagesToDrop--
        }
        outputDropCount++
    }

    // now check if last page is a separator. if so, we need to drop it too, since it was built
    // with now-dropped data (unless nonSeparatorCount was 0, which is why the early return)
    val finalPotentialSeparatorIndex = indexProvider(this)
    val finalPage = get(finalPotentialSeparatorIndex)
    if (finalPage == null) {
        removeAt(finalPotentialSeparatorIndex)
        outputDropCount++
    }
    return outputDropCount
}

internal fun <T : Any> MutableList<DataPage<T>?>.dropPagesStart(count: Int): Int =
    dropPages(count) { 0 }

internal fun <T : Any> MutableList<DataPage<T>?>.dropPagesEnd(count: Int): Int =
    dropPages(count) { it.lastIndex }

private class SeparatorState<R : Any, T : R>(
    val generator: (T?, T?) -> R?
) {
    /**
     * Lookup table of previously emitted pages.
     *
     *     Separator -> null
     *     Non-separator -> DataPage
     *
     * This table is used to emit drops (so we know how much to pad drops to account for separators)
     * and to provide adjacency data, to insert separators as new pages arrive.
     */
    val pageStash = mutableListOf<DataPage<T>?>()

    /**
     * True if next insert event should be treated as terminal, as a previous terminal event was
     * empty and no items has been loaded yet.
     */
    var endTerminalSeparatorDeferred = false
    var startTerminalSeparatorDeferred = false

    @Suppress("UNCHECKED_CAST")
    fun onEvent(event: PageEvent<T>): PageEvent<R> = when (event) {
        is Insert<T> -> onInsert(event)
        is Drop -> onDrop(event) as Drop<R>
        is PageEvent.LoadStateUpdate -> event as PageEvent<R>
    }.also {
        // validate internal state after each modification
        if (endTerminalSeparatorDeferred) {
            check(pageStash.isEmpty()) { "deferred endTerm, page stash should be empty" }
        }
        if (startTerminalSeparatorDeferred) {
            check(pageStash.isEmpty()) { "deferred startTerm, page stash should be empty" }
        }
    }

    fun Insert<T>.asRType(): Insert<R> {
        @Suppress("UNCHECKED_CAST")
        return this as Insert<R>
    }

    fun CombinedLoadStates.terminatesStart(): Boolean {
        val endState = prepend
        return endState is LoadState.NotLoading && endState.endOfPaginationReached
    }

    fun CombinedLoadStates.terminatesEnd(): Boolean {
        val endState = append
        return endState is LoadState.NotLoading && endState.endOfPaginationReached
    }

    internal fun <T : Any> Insert<T>.terminatesStart(): Boolean = if (loadType == APPEND) {
        startTerminalSeparatorDeferred
    } else {
        combinedLoadStates.terminatesStart()
    }

    internal fun <T : Any> Insert<T>.terminatesEnd(): Boolean = if (loadType == PREPEND) {
        endTerminalSeparatorDeferred
    } else {
        combinedLoadStates.terminatesEnd()
    }

    fun onInsert(event: Insert<T>): Insert<R> {
        val eventTerminatesStart = event.terminatesStart()
        val eventTerminatesEnd = event.terminatesEnd()
        val eventEmpty = event.pages.isEmpty()

        if (pageStash.isNotEmpty()) {
            require(pageStash.first() != null || event.loadType != PREPEND) {
                "Additional prepend event after prepend state is done"
            }
            require(pageStash.last() != null || event.loadType != APPEND) {
                "Additional append event after append state is done"
            }
        }

        if (eventEmpty) {
            if (eventTerminatesStart && eventTerminatesEnd) {
                // if event is empty, and fully terminal, resolve single separator, and that's it
                val separator = generator(null, null)
                endTerminalSeparatorDeferred = false
                startTerminalSeparatorDeferred = false
                pageStash.add(null) // represents separator
                return event.transformPages { listOf(separatorPage(separator, 0, 0, 0)) }
            } else if (!eventTerminatesStart && !eventTerminatesEnd) {
                // If event is non terminal simply ignore it.
                return event.asRType()
            } else if (pageStash.isEmpty()) {
                // can't insert the appropriate separator yet - defer!
                if (eventTerminatesEnd) {
                    endTerminalSeparatorDeferred = true
                }
                if (eventTerminatesStart) {
                    startTerminalSeparatorDeferred = true
                }
                return event.asRType()
            }
        }

        // If we've gotten to this point, that means the outgoing insert will have data.
        // Either this event has data, or the pageStash does.
        val outList = ArrayList<TransformablePage<R>>(event.pages.size)
        val outStateList = ArrayList<DataPage<T>?>(event.pages.size)
        if (eventTerminatesStart) {
            outStateList.add(null) // represents separator
            if (eventEmpty) {
                // header separator, using data from previous generation
                val firstStash = pageStash.first()!!
                val separator = generator(null, firstStash.first)
                outList.add(separatorPage(separator, firstStash, originalIndex = 0))
            } else {
                val firstPage = event.pages.first()
                val separator = generator(null, firstPage.data.first())
                outList.add(separatorPage(separator, firstPage, originalIndex = 0))
            }
        }

        // create pages based on data in the event
        if (!eventEmpty) {
            var itemBefore = if (event.loadType == APPEND) pageStash.lastOrNull()?.last else null
            event.pages.forEachIndexed { index, page ->
                // If page is being appended, or if we're in between pages, insert separator page
                if (index != 0 || (event.loadType == APPEND && pageStash.isNotEmpty())) {
                    val separator = generator(itemBefore!!, page.data.first())
                    outStateList.add(null) // represents separator
                    outList.add(separatorPage(separator, page, originalIndex = 0))
                }

                outStateList.add(DataPage(page))
                outList.add(page.insertInternalSeparators(generator))

                itemBefore = page.data.last()
            }
            if (event.loadType == PREPEND && pageStash.isNotEmpty()) {
                val lastPage = event.pages.last()
                val separator = generator(lastPage.data.last(), pageStash.first()!!.first)
                outStateList.add(null) // represents separator
                outList.add(
                    separatorPage(separator, lastPage, lastPage.originalLastIndex)
                )
            }
        }

        if (eventTerminatesEnd) {
            outStateList.add(null) // represents separator
            if (eventEmpty) {
                val lastStash = pageStash.last()!!
                // header separator, using data from previous generation
                val separator = generator(lastStash.last, null)
                outList.add(separatorPage(separator, lastStash, lastStash.originalLastIndex))
            } else {
                // header separator, using data from adjacent page
                val lastPage = event.pages.last()
                val separator = generator(lastPage.data.first(), null)
                outList.add(
                    separatorPage(separator, lastPage, lastPage.originalLastIndex)
                )
            }
        }

        endTerminalSeparatorDeferred = false
        startTerminalSeparatorDeferred = false

        if (event.loadType == APPEND) {
            pageStash.addAll(outStateList)
        } else {
            pageStash.addAll(0, outStateList)
        }
        return event.transformPages { outList }
    }

    fun onDrop(event: Drop<T>): Drop<T> {
        val newCount = if (event.loadType == PREPEND) {
            if (pageStash.isEmpty()) {
                startTerminalSeparatorDeferred = false
            }
            pageStash.dropPagesStart(event.count)
        } else {
            if (pageStash.isEmpty()) {
                endTerminalSeparatorDeferred = false
            }
            pageStash.dropPagesEnd(event.count)
        }

        @Suppress("UNCHECKED_CAST")
        return if (newCount == event.count) {
            event
        } else {
            event.copy(count = newCount)
        }
    }
}

/**
 * This is intentionally not named insertSeparators to avoid creating a clashing import
 * with PagingData.insertSeparators, which is public
 */
internal fun <T : R, R : Any> Flow<PageEvent<T>>.insertEventSeparators(
    generator: (T?, T?) -> R?
): Flow<PageEvent<R>> {
    val separatorState = SeparatorState(generator)
    return removeEmptyPages().map { separatorState.onEvent(it) }
}