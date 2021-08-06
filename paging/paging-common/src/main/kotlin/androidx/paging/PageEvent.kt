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
import androidx.paging.LoadType.REFRESH

/**
 * Events in the stream from paging fetch logic to UI.
 *
 * Every event sent to the UI is a PageEvent, and will be processed atomically.
 */
internal sealed class PageEvent<T : Any> {
    // Intentional to prefer Refresh, Prepend, Append constructors from Companion.
    @Suppress("DataClassPrivateConstructor")
    data class Insert<T : Any> private constructor(
        val loadType: LoadType,
        val pages: List<TransformablePage<T>>,
        val placeholdersBefore: Int,
        val placeholdersAfter: Int,
        val sourceLoadStates: LoadStates,
        val mediatorLoadStates: LoadStates? = null
    ) : PageEvent<T>() {
        init {
            require(loadType == APPEND || placeholdersBefore >= 0) {
                "Prepend insert defining placeholdersBefore must be > 0, but was" +
                    " $placeholdersBefore"
            }
            require(loadType == PREPEND || placeholdersAfter >= 0) {
                "Append insert defining placeholdersAfter must be > 0, but was" +
                    " $placeholdersAfter"
            }
            require(loadType != REFRESH || pages.isNotEmpty()) {
                "Cannot create a REFRESH Insert event with no TransformablePages as this could " +
                    "permanently stall pagination. Note that this check does not prevent empty " +
                    "LoadResults and is instead usually an indication of an internal error in " +
                    "Paging itself."
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
            sourceLoadStates = sourceLoadStates,
            mediatorLoadStates = mediatorLoadStates,
        )

        override suspend fun <R : Any> map(transform: suspend (T) -> R): PageEvent<R> = mapPages {
            TransformablePage(
                originalPageOffsets = it.originalPageOffsets,
                data = it.data.map { item -> transform(item) },
                hintOriginalPageOffset = it.hintOriginalPageOffset,
                hintOriginalIndices = it.hintOriginalIndices
            )
        }

        override suspend fun <R : Any> flatMap(
            transform: suspend (T) -> Iterable<R>
        ): PageEvent<R> = mapPages {
            val data = mutableListOf<R>()
            val originalIndices = mutableListOf<Int>()
            it.data.forEachIndexed { index, t ->
                data += transform(t)
                val indexToStore = it.hintOriginalIndices?.get(index) ?: index
                while (originalIndices.size < data.size) {
                    originalIndices.add(indexToStore)
                }
            }
            TransformablePage(
                originalPageOffsets = it.originalPageOffsets,
                data = data,
                hintOriginalPageOffset = it.hintOriginalPageOffset,
                hintOriginalIndices = originalIndices
            )
        }

        override suspend fun filter(predicate: suspend (T) -> Boolean): PageEvent<T> = mapPages {
            val data = mutableListOf<T>()
            val originalIndices = mutableListOf<Int>()
            it.data.forEachIndexed { index, t ->
                if (predicate(t)) {
                    data.add(t)
                    originalIndices.add(it.hintOriginalIndices?.get(index) ?: index)
                }
            }
            TransformablePage(
                originalPageOffsets = it.originalPageOffsets,
                data = data,
                hintOriginalPageOffset = it.hintOriginalPageOffset,
                hintOriginalIndices = originalIndices
            )
        }

        companion object {
            fun <T : Any> Refresh(
                pages: List<TransformablePage<T>>,
                placeholdersBefore: Int,
                placeholdersAfter: Int,
                sourceLoadStates: LoadStates,
                mediatorLoadStates: LoadStates? = null
            ) = Insert(
                REFRESH,
                pages,
                placeholdersBefore,
                placeholdersAfter,
                sourceLoadStates,
                mediatorLoadStates,
            )

            fun <T : Any> Prepend(
                pages: List<TransformablePage<T>>,
                placeholdersBefore: Int,
                sourceLoadStates: LoadStates,
                mediatorLoadStates: LoadStates? = null
            ) = Insert(
                PREPEND,
                pages,
                placeholdersBefore,
                -1,
                sourceLoadStates,
                mediatorLoadStates,
            )

            fun <T : Any> Append(
                pages: List<TransformablePage<T>>,
                placeholdersAfter: Int,
                sourceLoadStates: LoadStates,
                mediatorLoadStates: LoadStates? = null
            ) = Insert(
                APPEND,
                pages,
                -1,
                placeholdersAfter,
                sourceLoadStates,
                mediatorLoadStates,
            )

            /**
             * Empty refresh, used to convey initial state.
             *
             * Note - has no remote state, so remote state may be added over time
             */
            val EMPTY_REFRESH_LOCAL: Insert<Any> = Refresh(
                pages = listOf(TransformablePage.EMPTY_INITIAL_PAGE),
                placeholdersBefore = 0,
                placeholdersAfter = 0,
                sourceLoadStates = LoadStates(
                    refresh = LoadState.NotLoading.Incomplete,
                    prepend = LoadState.NotLoading.Complete,
                    append = LoadState.NotLoading.Complete,
                ),
            )
        }
    }
    // TODO: b/195658070 consider refactoring Drop events to carry full source/mediator states.
    data class Drop<T : Any>(
        val loadType: LoadType,
        /**
         * Smallest [TransformablePage.originalPageOffsets] to drop; inclusive.
         */
        val minPageOffset: Int,
        /**
         * Largest [TransformablePage.originalPageOffsets] to drop; inclusive
         */
        val maxPageOffset: Int,
        val placeholdersRemaining: Int
    ) : PageEvent<T>() {

        init {
            require(loadType != REFRESH) { "Drop load type must be PREPEND or APPEND" }
            require(pageCount > 0) { "Drop count must be > 0, but was $pageCount" }
            require(placeholdersRemaining >= 0) {
                "Invalid placeholdersRemaining $placeholdersRemaining"
            }
        }

        val pageCount get() = maxPageOffset - minPageOffset + 1
    }

    /**
     * A [PageEvent] to notify presenter layer of changes in local and remote LoadState.
     *
     * Uses two LoadStates objects instead of CombinedLoadStates so that consumers like
     * PagingDataDiffer can define behavior of convenience properties
     */
    data class LoadStateUpdate<T : Any>(
        val source: LoadStates,
        val mediator: LoadStates? = null,
    ) : PageEvent<T>()

    @Suppress("UNCHECKED_CAST")
    open suspend fun <R : Any> map(transform: suspend (T) -> R): PageEvent<R> = this as PageEvent<R>

    @Suppress("UNCHECKED_CAST")
    open suspend fun <R : Any> flatMap(transform: suspend (T) -> Iterable<R>): PageEvent<R> {
        return this as PageEvent<R>
    }

    open suspend fun filter(predicate: suspend (T) -> Boolean): PageEvent<T> = this
}
