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
import androidx.paging.LoadType.REFRESH
import androidx.paging.LoadType.PREPEND
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
        val placeholdersBefore: Int,
        val placeholdersAfter: Int,
        val combinedLoadStates: CombinedLoadStates
    ) : PageEvent<T>() {
        init {
            require(loadType == APPEND || placeholdersBefore >= 0) {
                "Append state defining placeholdersBefore must be > 0, but was" +
                        " $placeholdersBefore"
            }
            require(loadType == PREPEND || placeholdersAfter >= 0) {
                "Prepend state defining placeholdersAfter must be > 0, but was" +
                        " $placeholdersAfter"
            }
        }

        private inline fun <R : Any> mapPages(
            transform: (TransformablePage<T>) -> TransformablePage<R>
        ) = transformPages { it.map(transform) }

        internal inline fun <R : Any> transformPages(
            transform: (List<TransformablePage<T>>) -> List<TransformablePage<R>>
        ): Insert<R> = Insert(
            loadType = loadType,
            pages = transform(pages),
            placeholdersBefore = placeholdersBefore,
            placeholdersAfter = placeholdersAfter,
            combinedLoadStates = combinedLoadStates
        )

        override fun <R : Any> map(transform: (T) -> R): PageEvent<R> = mapPages {
            TransformablePage(
                originalPageOffset = it.originalPageOffset,
                data = it.data.map(transform),
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

        companion object {
            fun <T : Any> Refresh(
                pages: List<TransformablePage<T>>,
                placeholdersBefore: Int,
                placeholdersAfter: Int,
                combinedLoadStates: CombinedLoadStates
            ) = Insert(REFRESH, pages, placeholdersBefore, placeholdersAfter, combinedLoadStates)

            fun <T : Any> Prepend(
                pages: List<TransformablePage<T>>,
                placeholdersBefore: Int,
                combinedLoadStates: CombinedLoadStates
            ) = Insert(PREPEND, pages, placeholdersBefore, -1, combinedLoadStates)

            fun <T : Any> Append(
                pages: List<TransformablePage<T>>,
                placeholdersAfter: Int,
                combinedLoadStates: CombinedLoadStates
            ) = Insert(APPEND, pages, -1, placeholdersAfter, combinedLoadStates)

            /**
             * Empty refresh, used to convey initial state.
             *
             * Note - has no remote state, so remote state may be added over time
             */
            val EMPTY_REFRESH_LOCAL = Refresh<Any>(
                pages = listOf(),
                placeholdersBefore = 0,
                placeholdersAfter = 0,
                combinedLoadStates = CombinedLoadStates(
                    source = LoadStates(
                        refresh = LoadState.NotLoading.Idle,
                        prepend = LoadState.NotLoading.Done,
                        append = LoadState.NotLoading.Done
                    )
                )
            )
        }
    }

    data class Drop<T : Any>(
        val loadType: LoadType,
        val count: Int,
        val placeholdersRemaining: Int
    ) : PageEvent<T>() {

        init {
            require(loadType != REFRESH) { "Drop load type must be PREPEND or APPEND" }
            require(count >= 0) { "Drop count must be > 0, but was $count" }
            require(placeholdersRemaining >= 0) {
                "Invalid placeholdersRemaining $placeholdersRemaining"
            }
        }
    }

    data class LoadStateUpdate<T : Any>(
        val loadType: LoadType,
        val fromMediator: Boolean,
        val loadState: LoadState // TODO: consider using full state object here
    ) : PageEvent<T>() {
        init {
            require(loadState is LoadState.Loading || loadState is LoadState.Error) {
                "LoadStateUpdates can only be used for Loading or Error. To update loadState to " +
                        "Idle or Done, use Insert / Drop events."
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    open fun <R : Any> map(transform: (T) -> R): PageEvent<R> = this as PageEvent<R>

    @Suppress("UNCHECKED_CAST")
    open fun <R : Any> flatMap(transform: (T) -> Iterable<R>): PageEvent<R> = this as PageEvent<R>

    open fun filter(predicate: (T) -> Boolean): PageEvent<T> = this
}

private fun <T> MutableList<T>.removeFirst(count: Int) {
    repeat(count) { removeAt(0) }
}
private fun <T> MutableList<T>.removeLast(count: Int) {
    repeat(count) { removeAt(lastIndex) }
}

internal inline fun <R : Any, T : R, PageStash, Stash> Flow<PageEvent<T>>.scan(
    crossinline createStash: () -> Stash,
    crossinline createPageStash: (TransformablePage<T>) -> PageStash,
    crossinline createInsert: (PageEvent.Insert<T>, List<PageStash>, Stash) -> PageEvent.Insert<R>,
    crossinline createDrop: (PageEvent.Drop<T>, List<PageStash>, Stash) -> PageEvent.Drop<R>
): Flow<PageEvent<R>> {
    var stash: Stash = createStash()
    val pageStash = mutableListOf<PageStash>()
    return map { event ->
        @Suppress("UNCHECKED_CAST")
        when (event) {
            is PageEvent.Insert<T> -> {
                // use the stash before modifying it, since we may want to inspect adjacent pages
                val output = createInsert(event, pageStash, stash)
                val pageStashes = event.pages.map { createPageStash(it) }
                when (event.loadType) {
                    REFRESH -> {
                        check(pageStash.isEmpty())
                        pageStash.addAll(pageStashes)
                    }
                    PREPEND -> {
                        pageStash.addAll(0, pageStashes)
                    }
                    APPEND -> {
                        pageStash.addAll(pageStash.size, pageStashes)
                    }
                }
                output
            }
            is PageEvent.Drop -> {
                if (event.loadType == PREPEND) {
                    pageStash.removeFirst(event.count)
                } else {
                    pageStash.removeLast(event.count)
                }
                // use the stash after modifying it
                createDrop(event, pageStash, stash)
            }
            is PageEvent.LoadStateUpdate -> event as PageEvent<R>
        }
    }
}

/**
 * Transforms the Flow to an output-equivalent Flow, which does not have empty pages.
 *
 * This can be used before accessing adjacent pages, to ensure adjacent pages have context in
 * them.
 *
 * Note that we don't drop events, since those can contain other important state
 */
internal fun <T : Any> Flow<PageEvent<T>>.removeEmptyPages(): Flow<PageEvent<T>> = scan(
    createStash = { Unit },
    createPageStash = { page ->
        // stash contains whether incoming page was empty
        page.data.isEmpty()
    },
    createInsert = { insert, _, _ ->
        if (insert.pages.any { it.data.isEmpty() }) {
            // filter out empty pages
            insert.transformPages { pages -> pages.filter { it.data.isNotEmpty() } }
        } else {
            // no empty pages, can safely reuse this page
            insert
        }
    },
    createDrop = { drop, pageStash, _ ->
        var newCount = drop.count
        if (drop.loadType == PREPEND) {
            repeat(drop.count) { i ->
                if (pageStash[i]) {
                    newCount--
                }
            }
        } else {
            repeat(drop.count) { i ->
                if (pageStash[pageStash.lastIndex - i]) {
                    newCount--
                }
            }
        }
        if (drop.count == newCount) {
            drop
        } else {
            drop.copy(count = newCount)
        }
    }
)
