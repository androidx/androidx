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

import androidx.paging.LoadType.END
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.START
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Events in the stream from paging fetch logic to UI.
 *
 * Every event sent to the UI is a PageEvent, and will be processed atomically.
 */
internal sealed class PageEvent<T : Any> {
    data class Insert<T : Any> private constructor(
        val loadType: LoadType,
        val pages: List<TransformablePage<T>>,
        val placeholdersStart: Int,
        val placeholdersEnd: Int
    ) : PageEvent<T>() {
        init {
            require(loadType == END || placeholdersStart >= 0) {
                "Invalid placeholdersBefore $placeholdersStart"
            }
            require(loadType == START || placeholdersEnd >= 0) {
                "Invalid placeholdersAfter $placeholdersEnd"
            }
        }

        private inline fun <R : Any> mapPages(
            predicate: (TransformablePage<T>) -> TransformablePage<R>
        ) = transformPages { it.map(predicate) }

        internal inline fun <R : Any> transformPages(
            predicate: (List<TransformablePage<T>>) -> List<TransformablePage<R>>
        ): PageEvent<R> = Insert(
            loadType = loadType,
            pages = predicate(pages),
            placeholdersStart = placeholdersStart,
            placeholdersEnd = placeholdersEnd
        )

        override fun <R : Any> map(predicate: (T) -> R): PageEvent<R> = mapPages {
            TransformablePage(
                originalPageOffset = it.originalPageOffset,
                data = it.data.map(predicate),
                originalPageSize = it.originalPageSize,
                originalIndices = it.originalIndices
            )
        }

        override fun <R : Any> flatMap(transform: (T) -> Iterable<R>): PageEvent<R> = mapPages {
            val data = mutableListOf<R>()
            val originalIndices = mutableListOf<Int>()
            it.data.forEachIndexed { index, t ->
                data += transform(t)
                val indexToStore = it.originalIndices?.get(index) ?: index
                while (originalIndices.size < data.size) {
                    originalIndices.add(indexToStore)
                }
            }
            TransformablePage(
                originalPageOffset = it.originalPageOffset,
                data = data,
                originalPageSize = it.originalPageSize,
                originalIndices = originalIndices
            )
        }

        override fun filter(predicate: (T) -> Boolean): PageEvent<T> = mapPages {
            val data = mutableListOf<T>()
            val originalIndices = mutableListOf<Int>()
            it.data.forEachIndexed { index, t ->
                if (predicate(t)) {
                    data.add(t)
                    originalIndices.add(it.originalIndices?.get(index) ?: index)
                }
            }
            TransformablePage(
                originalPageOffset = it.originalPageOffset,
                data = data,
                originalPageSize = it.originalPageSize,
                originalIndices = originalIndices
            )
        }

        override fun filterOutEmptyPages(
            currentPages: MutableList<TransformablePage<T>>
        ): PageEvent<T> {
            // insert pre-filtered pages into list, so drop
            // can account for pages we've filtered out here
            applyToList(currentPages)

            return if (pages.any { it.data.isEmpty() }) {
                transformPages { pages -> pages.filter { it.data.isNotEmpty() } }
            } else {
                // no empty pages, can safely reuse this page
                this
            }
        }

        companion object {
            fun <T : Any> Refresh(
                pages: List<TransformablePage<T>>,
                placeholdersStart: Int,
                placeholdersEnd: Int
            ) = Insert(REFRESH, pages, placeholdersStart, placeholdersEnd)

            fun <T : Any> Start(
                pages: List<TransformablePage<T>>,
                placeholdersStart: Int
            ) = Insert(START, pages, placeholdersStart, -1)

            fun <T : Any> End(
                pages: List<TransformablePage<T>>,
                placeholdersEnd: Int
            ) = Insert(END, pages, -1, placeholdersEnd)
        }
    }

    data class Drop<T : Any>(
        val loadType: LoadType,
        val count: Int,
        val placeholdersRemaining: Int
    ) : PageEvent<T>() {

        init {
            require(loadType != REFRESH) { "Drop must be START or END" }
            require(count >= 0) { "Invalid count $count" }
            require(placeholdersRemaining >= 0) {
                "Invalid placeholdersRemaining $placeholdersRemaining"
            }
        }

        /**
         * Alter the drop event to skip dropping any empty pages, since they won't have been
         * sent downstream.
         */
        override fun filterOutEmptyPages(
            currentPages: MutableList<TransformablePage<T>>
        ): PageEvent<T> {
            // decrease count by number of empty pages that would have been dropped, since these
            // haven't been sent downstream
            var newCount = count
            if (loadType == START) {
                repeat(count) { i ->
                    if (currentPages[i].data.isEmpty()) {
                        newCount--
                    }
                }
            } else {
                repeat(count) { i ->
                    if (currentPages[currentPages.size - i].data.isEmpty()) {
                        newCount--
                    }
                }
            }

            // apply drop to currentPages after newCount is computed, so it represents loaded
            // pages before this tranform is applied
            applyToList(currentPages)

            return if (newCount == count) {
                // no empty pages encountered
                this
            } else {
                Drop(
                    loadType,
                    newCount,
                    placeholdersRemaining
                )
            }
        }
    }

    data class StateUpdate<T : Any>(
        val loadType: LoadType,
        val loadState: LoadState
    ) : PageEvent<T>()

    @Suppress("UNCHECKED_CAST")
    open fun <R : Any> map(predicate: (T) -> R): PageEvent<R> = this as PageEvent<R>

    @Suppress("UNCHECKED_CAST")
    open fun <R : Any> flatMap(transform: (T) -> Iterable<R>): PageEvent<R> = this as PageEvent<R>

    open fun filter(predicate: (T) -> Boolean): PageEvent<T> = this

    open fun filterOutEmptyPages(
        currentPages: MutableList<TransformablePage<T>>
    ): PageEvent<T> = this
}

/**
 * TODO: optimize this per usecase, to avoid holding onto the whole page in memory
 */
internal fun <T : Any> PageEvent.Insert<T>.applyToList(
    currentPages: MutableList<TransformablePage<T>>
) {
    when (loadType) {
        REFRESH -> {
            check(currentPages.isEmpty())
            currentPages.addAll(pages)
        }
        START -> {
            currentPages.addAll(0, pages)
        }
        END -> {
            currentPages.addAll(currentPages.size, pages)
        }
    }
}

/**
 * TODO: optimize this per usecase, to avoid holding onto the whole page in memory
 */
internal fun <T : Any> PageEvent.Drop<T>.applyToList(
    currentPages: MutableList<TransformablePage<T>>
) {
    if (loadType == START) {
        repeat(count) { currentPages.removeAt(0) }
    } else {
        repeat(count) { currentPages.removeAt(currentPages.lastIndex) }
    }
}

/**
 * Transforms the Flow to an output-equivalent Flow, which does not have empty pages.
 *
 * This can be used before accessing adjacent pages, to ensure adjacent pages have context in
 * them.
 */
internal fun <T : Any> Flow<PageEvent<T>>.removeEmptyPages(): Flow<PageEvent<T>> {
    val pages = mutableListOf<TransformablePage<T>>()

    // TODO: consider dropping, or not even creating noop (empty) events entirely
    return map { it.filterOutEmptyPages(pages) }
}

/**
 * Transforms the Flow to include optional separators in between each pair of items in the output
 * stream.
 *
 * TODO: support separator at beginning / end - requires tracking of loading state
 *  (to know when an Insert.Start event is terminal)
 */
internal fun <R : Any, T : R> Flow<PageEvent<T>>.insertSeparators(
    predicate: (T?, T?) -> R?
): Flow<PageEvent<R>> {
    val pages = mutableListOf<TransformablePage<T>>()
    return removeEmptyPages()
        .map { event -> event.insertSeparators(pages, predicate) }
}