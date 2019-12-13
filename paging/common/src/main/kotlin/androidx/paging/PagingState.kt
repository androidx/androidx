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

/**
 * Fully resolved, immutable snapshot of Paging state: placeholders, items, and LoadStates.
 */
internal data class PagingState<T : Any>(
    override val placeholdersStart: Int,
    override val placeholdersEnd: Int,
    private val pages: List<TransformablePage<T>>,
    override val storageCount: Int = pages.fullCount(),
    val loadStateRefresh: LoadState,
    val loadStateStart: LoadState,
    val loadStateEnd: LoadState,
    private val hintReceiver: (ViewportHint) -> Unit
) : NullPaddedList<T> {
    override fun toString() =
        """
            $placeholdersStart nulls, $pages, $placeholdersEnd nulls
            r=$loadStateRefresh, s=$loadStateStart, e=$loadStateEnd
            rec=$hintReceiver
        """.trimIndent()

    override val size: Int = placeholdersStart + storageCount + placeholdersEnd

    override fun getFromStorage(localIndex: Int): T {
        var pageIndex = 0
        var indexInPage: Int = localIndex

        // Since we don't know if page sizes are regular, we walk to correct page.
        val localPageCount = pages.size
        while (pageIndex < localPageCount) {
            val pageSize = pages[pageIndex].data.size
            if (pageSize > indexInPage) {
                // stop, found the page
                break
            }
            indexInPage -= pageSize
            pageIndex++
        }
        return pages[pageIndex].data[indexInPage]
    }

    /**
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    fun getAndLoadAround(index: Int): T? {
        if (index < 0 || index >= size) {
            throw IndexOutOfBoundsException("Index: $index, Size: $size")
        }

        val localIndex = index - placeholdersStart
        if (localIndex < 0 || localIndex >= storageCount) {
            return null
        }

        var pageIndex = 0
        var indexInPage = localIndex
        while (indexInPage >= pages[pageIndex].data.size && pageIndex < pages.lastIndex) {
            // index doesn't appear in current page, keep looking!
            indexInPage -= pages[pageIndex].data.size
            pageIndex++
        }
        hintReceiver(pages[pageIndex].getLoadHint(indexInPage))
        return pages[pageIndex].data[indexInPage]
    }

    internal class Producer<T : Any>(
        private val hintReceiver: (ViewportHint) -> Unit
    ) {
        private var leadingNullCount: Int = 0
        private var trailingNullCount: Int = 0
        private var size: Int = 0
        private var pages: List<TransformablePage<T>> = listOf()
        private var storageCount: Int = 0

        private var stateRefresh: LoadState = LoadState.Loading
        private var stateStart: LoadState = LoadState.Idle
        private var stateEnd: LoadState = LoadState.Idle

        fun processEvent(pageEvent: PageEvent<T>): PagingState<T> {
            // TODO: store metadata about recent events, so it's easy to do fast diffs on the
            //  adapter side
            when (pageEvent) {
                is PageEvent.Insert -> insert(pageEvent)
                is PageEvent.Drop -> drop(pageEvent)
                is PageEvent.StateUpdate -> stateUpdate(pageEvent)
            }
            return snapshot()
        }

        private fun stateUpdate(state: PageEvent.StateUpdate<T>) {
            when (state.loadType) {
                LoadType.REFRESH -> stateRefresh = state.loadState
                LoadType.START -> stateStart = state.loadState
                LoadType.END -> stateEnd = state.loadState
            }
        }

        private fun insert(insert: PageEvent.Insert<T>) {
            val count = insert.pages.fullCount()
            when (insert.loadType) {
                LoadType.REFRESH -> {
                    check(pages.isEmpty()) { "Refresh must be first" }
                    pages = insert.pages
                    storageCount += count
                    leadingNullCount = insert.placeholdersStart
                    trailingNullCount = insert.placeholdersEnd
                }
                LoadType.START -> {
                    pages = insert.pages + pages
                    storageCount += count
                    leadingNullCount = insert.placeholdersStart
                }
                LoadType.END -> {
                    pages = pages + insert.pages
                    storageCount += count
                    trailingNullCount = insert.placeholdersEnd
                }
            }
        }

        private fun drop(drop: PageEvent.Drop<T>) {
            if (drop.loadType == LoadType.START) {
                val removeCount = pages.take(drop.count).fullCount()
                pages = pages.subList(fromIndex = drop.count, toIndex = pages.size)
                storageCount -= removeCount
                leadingNullCount = drop.placeholdersRemaining
            } else {
                val removeCount = pages.takeLast(drop.count).fullCount()
                pages = pages.subList(fromIndex = 0, toIndex = pages.size - drop.count)
                storageCount -= removeCount
                trailingNullCount = drop.placeholdersRemaining
            }
        }

        private fun snapshot() = PagingState(
            placeholdersStart = leadingNullCount,
            placeholdersEnd = trailingNullCount,
            pages = pages,
            storageCount = storageCount,
            loadStateRefresh = stateRefresh,
            loadStateStart = stateStart,
            loadStateEnd = stateEnd,
            hintReceiver = hintReceiver
        )
    }

    companion object {
        internal fun <T : Any> List<TransformablePage<T>>.fullCount() = sumBy { it.data.size }

        internal val noopHintReceiver: (ViewportHint) -> Unit = {}

        internal val initialImpl = PagingState<Any>(
            placeholdersStart = 0,
            placeholdersEnd = 0,
            pages = listOf(),
            storageCount = 0,
            loadStateRefresh = LoadState.Loading,
            loadStateStart = LoadState.Idle,
            loadStateEnd = LoadState.Idle,
            hintReceiver = noopHintReceiver
        )

        @Suppress("UNCHECKED_CAST")
        fun <T : Any> initial() = initialImpl as PagingState<T>
    }
}