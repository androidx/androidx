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

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * [InitialPagedList] is an empty placeholder that's sent at the front of a stream of [PagedList].
 *
 * It's used solely for listening to [LoadType.REFRESH] loading events, and retrying
 * any errors that occur during initial load.
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InitialPagedList<K : Any, V : Any>(
    coroutineScope: CoroutineScope,
    notifyDispatcher: CoroutineDispatcher,
    backgroundDispatcher: CoroutineDispatcher,
    config: Config,
    initialLastKey: K?
) : ContiguousPagedList<K, V>(
    pagingSource = LegacyPagingSource(notifyDispatcher, InitialDataSource()),
    coroutineScope = coroutineScope,
    notifyDispatcher = notifyDispatcher,
    backgroundDispatcher = backgroundDispatcher,
    boundaryCallback = null,
    config = config,
    initialPage = PagingSource.LoadResult.Page.empty(),
    initialLastKey = initialLastKey
)
