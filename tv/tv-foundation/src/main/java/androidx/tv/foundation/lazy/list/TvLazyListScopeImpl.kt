/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.tv.foundation.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

@Suppress("IllegalExperimentalApiUsage") // TODO (b/233188423): Address before moving to beta
@OptIn(ExperimentalFoundationApi::class)
internal class TvLazyListScopeImpl : TvLazyListScope {

    private val _intervals = MutableIntervalList<LazyListIntervalContent>()
    val intervals: IntervalList<LazyListIntervalContent> = _intervals

    private var _headerIndexes: MutableList<Int>? = null
    val headerIndexes: List<Int> get() = _headerIndexes ?: emptyList()

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable TvLazyListItemScope.(index: Int) -> Unit
    ) {
        _intervals.addInterval(
            count,
            LazyListIntervalContent(
                key = key,
                type = contentType,
                item = itemContent
            )
        )
    }

    override fun item(
        key: Any?,
        contentType: Any?,
        content: @Composable TvLazyListItemScope.() -> Unit
    ) {
        _intervals.addInterval(
            1,
            LazyListIntervalContent(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                item = { content() }
            )
        )
    }
}

internal class LazyListIntervalContent(
    val key: ((index: Int) -> Any)?,
    val type: ((index: Int) -> Any?),
    val item: @Composable TvLazyListItemScope.(index: Int) -> Unit
)
