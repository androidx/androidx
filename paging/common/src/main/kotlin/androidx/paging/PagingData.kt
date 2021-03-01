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
    internal val receiver: UiReceiver
) {

    public companion object {
        internal val NOOP_RECEIVER = object : UiReceiver {
            override fun accessHint(viewportHint: ViewportHint) {}

            override fun retry() {}

            override fun refresh() {}
        }

        @Suppress("MemberVisibilityCanBePrivate") // synthetic access
        internal val EMPTY = PagingData(
            flow = flowOf(PageEvent.Insert.EMPTY_REFRESH_LOCAL),
            receiver = NOOP_RECEIVER
        )

        /**
         * Create a [PagingData] that immediately displays an empty list of items when submitted to
         * [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         */
        @Suppress("UNCHECKED_CAST")
        @JvmStatic // Convenience for Java developers.
        public fun <T : Any> empty(): PagingData<T> = EMPTY as PagingData<T>

        /**
         * Create a [PagingData] that immediately displays a static list of items when submitted to
         * [AsyncPagingDataAdapter][androidx.paging.AsyncPagingDataAdapter].
         *
         * @param data Static list of [T] to display.
         */
        @JvmStatic // Convenience for Java developers.
        public fun <T : Any> from(data: List<T>): PagingData<T> = PagingData(
            flow = flowOf(
                PageEvent.Insert.Refresh(
                    pages = listOf(TransformablePage(originalPageOffset = 0, data = data)),
                    placeholdersBefore = 0,
                    placeholdersAfter = 0,
                    combinedLoadStates = CombinedLoadStates(
                        refresh = LoadState.NotLoading.Incomplete,
                        prepend = LoadState.NotLoading.Complete,
                        append = LoadState.NotLoading.Complete,
                        source = LoadStates(
                            refresh = LoadState.NotLoading.Incomplete,
                            prepend = LoadState.NotLoading.Complete,
                            append = LoadState.NotLoading.Complete
                        )
                    )
                )
            ),
            receiver = NOOP_RECEIVER
        )
    }
}
