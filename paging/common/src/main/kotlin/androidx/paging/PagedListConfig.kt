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
 * Constructs a [PagedList.Config], convenience for [PagedList.Config.Builder].
 *
 * @param pageSize Number of items loaded at once from the DataSource.
 * @param prefetchDistance Distance the PagedList should prefetch.
 * @param enablePlaceholders False if null placeholders should be disabled.
 * @param initialLoadSizeHint Number of items to load while initializing the PagedList.
 * @param maxSize Maximum number of items to keep in memory, or
 *                [PagedList.Config.MAX_SIZE_UNBOUNDED] to disable page dropping.
 */
@Suppress("FunctionName")
fun Config(
    pageSize: Int,
    prefetchDistance: Int = pageSize,
    enablePlaceholders: Boolean = true,
    initialLoadSizeHint: Int = pageSize * PagedList.Config.Builder.DEFAULT_INITIAL_PAGE_MULTIPLIER,
    maxSize: Int = PagedList.Config.MAX_SIZE_UNBOUNDED
): PagedList.Config {
    return PagedList.Config.Builder()
        .setPageSize(pageSize)
        .setPrefetchDistance(prefetchDistance)
        .setEnablePlaceholders(enablePlaceholders)
        .setInitialLoadSizeHint(initialLoadSizeHint)
        .setMaxSize(maxSize)
        .build()
}
