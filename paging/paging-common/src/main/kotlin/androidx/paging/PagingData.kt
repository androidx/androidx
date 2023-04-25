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
import kotlinx.coroutines.flow.flowOf

/**
 * Container for Paged data from a single generation of loads.
 *
 * Each refresh of data (generally either pushed by local storage, or pulled from the network)
 * will have a separate corresponding [PagingData].
 */
public class PagingData<T : Any> internal constructor(
    internal val flow: Flow<PageEvent<T>>,
    internal val uiReceiver: UiReceiver,
    internal val hintReceiver: HintReceiver,

    /**
     * A lambda returning a nullable PageEvent.Insert containing data which can be accessed
     * and displayed synchronously without requiring collection.
     *
     * For example, the data may be real loaded data that has been cached via [cachedIn].
     */
    private val cachedPageEvent: () -> PageEvent.Insert<T>? = { null }
) {
    public companion object {
        internal val NOOP_UI_RECEIVER = object : UiReceiver {
            override fun retry() {}

            override fun refresh() {}
        }

        internal val NOOP_HINT_RECEIVER = object : HintReceiver {
            override fun accessHint(viewportHint: ViewportHint) {}
        }

        /**
         * Create a [PagingData] that immediately displays an empty list of items without
         * dispatching any load state updates when submitted to a presenter. E.g.,
         * [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic // Convenience for Java developers.
        public fun <T : Any> empty(): PagingData<T> = PagingData(
            flow = flowOf(
                PageEvent.StaticList(
                    data = listOf(),
                    sourceLoadStates = null,
                    mediatorLoadStates = null,
                )
            ),
            uiReceiver = NOOP_UI_RECEIVER,
            hintReceiver = NOOP_HINT_RECEIVER,
        )

        /**
         * Create a [PagingData] that immediately displays an empty list of items when submitted to
         * a presenter. E.g., [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         *
         * @param sourceLoadStates [LoadStates] of [PagingSource] to pass forward to a presenter.
         * E.g., [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         * @param mediatorLoadStates [LoadStates] of [RemoteMediator] to pass forward to a
         * presenter. E.g., [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         */
        @Suppress("UNCHECKED_CAST")
        @JvmOverloads
        @JvmStatic // Convenience for Java developers.
        public fun <T : Any> empty(
            sourceLoadStates: LoadStates,
            mediatorLoadStates: LoadStates? = null,
        ): PagingData<T> = PagingData(
            flow = flowOf(
                PageEvent.StaticList(
                    data = listOf(),
                    sourceLoadStates = sourceLoadStates,
                    mediatorLoadStates = mediatorLoadStates,
                )
            ),
            uiReceiver = NOOP_UI_RECEIVER,
            hintReceiver = NOOP_HINT_RECEIVER,
        )

        /**
         * Create a [PagingData] that immediately displays a static list of items without
         * dispatching any load state updates when submitted to a presenter. E.g.,
         * [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         *
         * @param data Static list of [T] to display.
         */
        @JvmStatic // Convenience for Java developers.
        public fun <T : Any> from(
            data: List<T>,
        ): PagingData<T> = PagingData(
            flow = flowOf(
                PageEvent.StaticList(
                    data = data,
                    sourceLoadStates = null,
                    mediatorLoadStates = null,
                )
            ),
            uiReceiver = NOOP_UI_RECEIVER,
            hintReceiver = NOOP_HINT_RECEIVER,
        )

        /**
         * Create a [PagingData] that immediately displays a static list of items when submitted to
         * a presenter. E.g., [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         *
         * @param data Static list of [T] to display.
         * @param sourceLoadStates [LoadStates] of [PagingSource] to pass forward to a presenter.
         * E.g., [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         * @param mediatorLoadStates [LoadStates] of [RemoteMediator] to pass forward to a
         * presenter. E.g., [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         */
        @JvmOverloads
        @JvmStatic // Convenience for Java developers.
        public fun <T : Any> from(
            data: List<T>,
            sourceLoadStates: LoadStates,
            mediatorLoadStates: LoadStates? = null,
        ): PagingData<T> = PagingData(
            flow = flowOf(
                PageEvent.StaticList(
                    data = data,
                    sourceLoadStates = sourceLoadStates,
                    mediatorLoadStates = mediatorLoadStates,
                )
            ),
            uiReceiver = NOOP_UI_RECEIVER,
            hintReceiver = NOOP_HINT_RECEIVER,
        )
    }

    internal fun cachedEvent(): PageEvent.Insert<T>? = cachedPageEvent()
}