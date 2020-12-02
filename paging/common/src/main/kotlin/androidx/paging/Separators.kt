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
internal suspend fun <R : Any, T : R> TransformablePage<T>.insertInternalSeparators(
    generator: suspend (T?, T?) -> R?
): TransformablePage<R> {
    if (data.isEmpty()) {
        @Suppress("UNCHECKED_CAST")
        return this as TransformablePage<R>
    }

    val initialCapacity = data.size + 4 // extra space to avoid bigger allocations
    val outputList = ArrayList<R>(initialCapacity)
    val outputIndices = ArrayList<Int>(initialCapacity)

    outputList.add(data.first())
    outputIndices.add(hintOriginalIndices?.first() ?: 0)
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
            originalPageOffsets = originalPageOffsets,
            data = outputList,
            hintOriginalPageOffset = hintOriginalPageOffset,
            hintOriginalIndices = outputIndices
        )
    }
}

/**
 * Create a [TransformablePage] with the given separator (or empty, if the separator is null)
 */
internal fun <T : Any> separatorPage(
    separator: T,
    originalPageOffsets: IntArray,
    hintOriginalPageOffset: Int,
    hintOriginalIndex: Int
): TransformablePage<T> = TransformablePage(
    originalPageOffsets = originalPageOffsets,
    data = listOf(separator),
    hintOriginalPageOffset = hintOriginalPageOffset,
    hintOriginalIndices = listOf(hintOriginalIndex)
)

/**
 * Create a [TransformablePage] with the given separator, and add it if [separator] is non-null
 *
 * This is a helper to create separator pages that contain a single separator to be used to join
 * pages provided from stream.
 */
internal fun <T : Any> MutableList<TransformablePage<T>>.addSeparatorPage(
    separator: T?,
    originalPageOffsets: IntArray,
    hintOriginalPageOffset: Int,
    hintOriginalIndex: Int
) {
    if (separator == null) return

    val separatorPage = separatorPage(
        separator = separator,
        originalPageOffsets = originalPageOffsets,
        hintOriginalPageOffset = hintOriginalPageOffset,
        hintOriginalIndex = hintOriginalIndex
    )
    add(separatorPage)
}

/**
 * Create a [TransformablePage] with the given separator, and add it if [separator] is non-null
 *
 * This is a helper to create separator pages that contain a single separator to be used to join
 * pages provided from stream.
 */
internal fun <R : Any, T : R> MutableList<TransformablePage<R>>.addSeparatorPage(
    separator: R?,
    adjacentPageBefore: TransformablePage<T>?,
    adjacentPageAfter: TransformablePage<T>?,
    hintOriginalPageOffset: Int,
    hintOriginalIndex: Int
) {
    val beforeOffsets = adjacentPageBefore?.originalPageOffsets
    val afterOffsets = adjacentPageAfter?.originalPageOffsets
    addSeparatorPage(
        separator = separator,
        originalPageOffsets = when {
            beforeOffsets != null && afterOffsets != null -> {
                (beforeOffsets + afterOffsets).distinct().sorted().toIntArray()
            }
            beforeOffsets == null && afterOffsets != null -> afterOffsets
            beforeOffsets != null && afterOffsets == null -> beforeOffsets
            else -> throw IllegalArgumentException(
                "Separator page expected adjacentPageBefore or adjacentPageAfter, but both were" +
                    " null."
            )
        },
        hintOriginalPageOffset = hintOriginalPageOffset,
        hintOriginalIndex = hintOriginalIndex
    )
}

private class SeparatorState<R : Any, T : R>(
    val generator: suspend (before: T?, after: T?) -> R?
) {
    /**
     * Lookup table of previously emitted pages, that skips empty pages.
     *
     * This table is used to keep track of originalPageOffsets for separators that would span
     * across empty pages. It includes a simplified version of loaded pages which only has the
     * first and last item in each page to reduce memory pressure.
     *
     * Note: [TransformablePage] added to this stash must always have
     * [TransformablePage.originalPageOffsets] defined, since it needs to keep track of the
     * originalPageOffset of the last item.
     */
    val pageStash = mutableListOf<TransformablePage<T>>()

    /**
     * True if next insert event should be treated as terminal, as a previous terminal event was
     * empty and no items has been loaded yet.
     */
    var endTerminalSeparatorDeferred = false
    var startTerminalSeparatorDeferred = false

    var footerAdded = false
    var headerAdded = false

    @Suppress("UNCHECKED_CAST")
    suspend fun onEvent(event: PageEvent<T>): PageEvent<R> = when (event) {
        is Insert<T> -> onInsert(event)
        is Drop -> {
            onDrop(event) // Update pageStash state
            event as Drop<R>
        }
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

    fun LoadState.isTerminal(): Boolean {
        return this is LoadState.NotLoading && endOfPaginationReached
    }

    fun CombinedLoadStates.terminatesStart(): Boolean {
        return source.prepend.isTerminal() && mediator?.prepend?.isTerminal() != false
    }

    fun CombinedLoadStates.terminatesEnd(): Boolean {
        return source.append.isTerminal() && mediator?.append?.isTerminal() != false
    }

    fun <T : Any> Insert<T>.terminatesStart(): Boolean = if (loadType == APPEND) {
        startTerminalSeparatorDeferred
    } else {
        combinedLoadStates.terminatesStart()
    }

    fun <T : Any> Insert<T>.terminatesEnd(): Boolean = if (loadType == PREPEND) {
        endTerminalSeparatorDeferred
    } else {
        combinedLoadStates.terminatesEnd()
    }

    suspend fun onInsert(event: Insert<T>): Insert<R> {
        val eventTerminatesStart = event.terminatesStart()
        val eventTerminatesEnd = event.terminatesEnd()
        val eventEmpty = event.pages.all { it.data.isEmpty() }

        require(!headerAdded || event.loadType != PREPEND || eventEmpty) {
            "Additional prepend event after prepend state is done"
        }
        require(!footerAdded || event.loadType != APPEND || eventEmpty) {
            "Additional append event after append state is done"
        }

        if (eventEmpty) {
            if (eventTerminatesStart && eventTerminatesEnd) {
                // if event is empty, and fully terminal, resolve single separator, and that's it
                val separator = generator(null, null)
                endTerminalSeparatorDeferred = false
                startTerminalSeparatorDeferred = false
                return if (separator == null) {
                    event.asRType()
                } else {
                    event.transformPages {
                        listOf(separatorPage(separator, intArrayOf(0), 0, 0))
                    }
                }
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
        val stashOutList = ArrayList<TransformablePage<T>>(event.pages.size)

        var firstNonEmptyPage: TransformablePage<T>? = null
        var firstNonEmptyPageIndex: Int? = null
        var lastNonEmptyPage: TransformablePage<T>? = null
        var lastNonEmptyPageIndex: Int? = null
        if (!eventEmpty) {
            // Compute the first non-empty page index to be used as adjacent pages for creating
            // separator pages.
            // Note: We're guaranteed to have at least one non-empty page at this point.
            var pageIndex = 0
            while (pageIndex < event.pages.lastIndex && event.pages[pageIndex].data.isEmpty()) {
                pageIndex++
            }
            firstNonEmptyPageIndex = pageIndex
            firstNonEmptyPage = event.pages[pageIndex]

            // Compute the last non-empty page index to be used as adjacent pages for creating separator
            // pages.
            // Note: We're guaranteed to have at least one non-empty page at this point.
            pageIndex = event.pages.lastIndex
            while (pageIndex > 0 && event.pages[pageIndex].data.isEmpty()) {
                pageIndex--
            }
            lastNonEmptyPageIndex = pageIndex
            lastNonEmptyPage = event.pages[pageIndex]
        }

        // Header separator
        if (eventTerminatesStart) {
            headerAdded = true

            // Using data from previous generation if event is empty, adjacent page otherwise.
            val pageAfter = if (eventEmpty) pageStash.first() else firstNonEmptyPage!!
            outList.addSeparatorPage(
                separator = generator(null, pageAfter.data.first()),
                adjacentPageBefore = null,
                adjacentPageAfter = pageAfter,
                hintOriginalPageOffset = pageAfter.hintOriginalPageOffset,
                hintOriginalIndex = pageAfter.hintOriginalIndices?.first() ?: 0
            )
        }

        // Create pages based on data in the event
        if (!eventEmpty) {
            // Add empty pages before [firstNonEmptyPageIndex] from event directly.
            for (pageIndex in 0 until firstNonEmptyPageIndex!!) {
                outList.add(event.pages[pageIndex].insertInternalSeparators(generator))
            }

            // Insert separator page between last stash and first non-empty event page if APPEND.
            if (event.loadType == APPEND && pageStash.isNotEmpty()) {
                val lastStash = pageStash.last()
                val separator = generator(lastStash.data.last(), firstNonEmptyPage!!.data.first())
                outList.addSeparatorPage(
                    separator = separator,
                    adjacentPageBefore = lastStash,
                    adjacentPageAfter = firstNonEmptyPage,
                    hintOriginalPageOffset = firstNonEmptyPage.hintOriginalPageOffset,
                    hintOriginalIndex = firstNonEmptyPage.hintOriginalIndices?.first() ?: 0
                )
            }

            // Add the first non-empty insert event page with separators inserted.
            stashOutList.add(transformablePageToStash(firstNonEmptyPage!!))
            outList.add(firstNonEmptyPage.insertInternalSeparators(generator))

            // Handle event pages that may be sparsely populated by empty pages.
            event.pages
                .subList(firstNonEmptyPageIndex, lastNonEmptyPageIndex!! + 1)
                // Note: If we enter reduce loop, pageBefore is guaranteed to be non-null.
                .reduce { pageBefore, page ->
                    if (page.data.isNotEmpty()) {
                        // Insert separator pages in between insert event pages.
                        val separator = generator(pageBefore.data.last(), page.data.first())
                        outList.addSeparatorPage(
                            separator = separator,
                            adjacentPageBefore = pageBefore,
                            adjacentPageAfter = page,
                            hintOriginalPageOffset = if (event.loadType == PREPEND) {
                                pageBefore.hintOriginalPageOffset
                            } else {
                                page.hintOriginalPageOffset
                            },
                            hintOriginalIndex = if (event.loadType == PREPEND) {
                                pageBefore.hintOriginalIndices?.last() ?: pageBefore.data.lastIndex
                            } else {
                                page.hintOriginalIndices?.first() ?: 0
                            }
                        )
                    }

                    if (page.data.isNotEmpty()) {
                        stashOutList.add(transformablePageToStash(page))
                    }
                    // Add the insert event page with separators inserted.
                    outList.add(page.insertInternalSeparators(generator))

                    // Current page becomes the next pageBefore on next iteration unless empty.
                    if (page.data.isNotEmpty()) page else pageBefore
                }

            // Insert separator page between first stash and last non-empty event page if PREPEND.
            if (event.loadType == PREPEND && pageStash.isNotEmpty()) {
                val pageAfter = pageStash.first()
                val separator = generator(lastNonEmptyPage!!.data.last(), pageAfter.data.first())
                outList.addSeparatorPage(
                    separator = separator,
                    adjacentPageBefore = lastNonEmptyPage,
                    adjacentPageAfter = pageAfter,
                    hintOriginalPageOffset = lastNonEmptyPage.hintOriginalPageOffset,
                    hintOriginalIndex = lastNonEmptyPage.hintOriginalIndices?.last()
                        ?: lastNonEmptyPage.data.lastIndex
                )
            }

            // Add empty pages after [lastNonEmptyPageIndex] from event directly.
            for (pageIndex in (lastNonEmptyPageIndex + 1)..event.pages.lastIndex) {
                outList.add(event.pages[pageIndex].insertInternalSeparators(generator))
            }
        }

        // Footer separator
        if (eventTerminatesEnd) {
            footerAdded = true

            // Using data from previous generation if event is empty, adjacent page otherwise.
            val pageBefore = if (eventEmpty) pageStash.last() else lastNonEmptyPage!!
            outList.addSeparatorPage(
                separator = generator(pageBefore.data.last(), null),
                adjacentPageBefore = pageBefore,
                adjacentPageAfter = null,
                hintOriginalPageOffset = pageBefore.hintOriginalPageOffset,
                hintOriginalIndex = pageBefore.hintOriginalIndices?.last()
                    ?: pageBefore.data.lastIndex
            )
        }

        endTerminalSeparatorDeferred = false
        startTerminalSeparatorDeferred = false

        if (event.loadType == APPEND) {
            pageStash.addAll(stashOutList)
        } else {
            pageStash.addAll(0, stashOutList)
        }
        return event.transformPages { outList }
    }

    /**
     * Process a [Drop] event to update [pageStash] stage.
     */
    fun onDrop(event: Drop<T>) {
        if (pageStash.isEmpty()) {
            if (event.loadType == PREPEND) {
                startTerminalSeparatorDeferred = false
            } else {
                endTerminalSeparatorDeferred = false
            }
        }

        // Drop all stashes that depend on pageOffset being dropped.
        val pageOffsetsToDrop = event.minPageOffset..event.maxPageOffset
        pageStash.removeAll { stash ->
            stash.originalPageOffsets.any { pageOffsetsToDrop.contains(it) }
        }
    }

    private fun <T : Any> transformablePageToStash(
        originalPage: TransformablePage<T>
    ): TransformablePage<T> {
        return TransformablePage(
            originalPageOffsets = originalPage.originalPageOffsets,
            data = listOf(originalPage.data.first(), originalPage.data.last()),
            hintOriginalPageOffset = originalPage.hintOriginalPageOffset,
            hintOriginalIndices = listOf(
                originalPage.hintOriginalIndices?.first() ?: 0,
                originalPage.hintOriginalIndices?.last() ?: originalPage.data.lastIndex
            )
        )
    }
}

/**
 * This is intentionally not named insertSeparators to avoid creating a clashing import
 * with PagingData.insertSeparators, which is public
 */
internal fun <T : R, R : Any> Flow<PageEvent<T>>.insertEventSeparators(
    generator: suspend (T?, T?) -> R?
): Flow<PageEvent<R>> {
    val separatorState = SeparatorState { before: T?, after: T? -> generator(before, after) }
    return map { separatorState.onEvent(it) }
}