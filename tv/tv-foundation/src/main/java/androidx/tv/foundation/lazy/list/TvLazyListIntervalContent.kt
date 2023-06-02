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
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable
import androidx.tv.foundation.ExperimentalTvFoundationApi

@Suppress("IllegalExperimentalApiUsage") // TODO (b/233188423): Address before moving to beta
@OptIn(ExperimentalFoundationApi::class)
internal class TvLazyListIntervalContent(
    content: TvLazyListScope.() -> Unit,
) : LazyLayoutIntervalContent<TvLazyListInterval>(), TvLazyListScope {
    override val intervals: MutableIntervalList<TvLazyListInterval> = MutableIntervalList()

    private var _headerIndexes: MutableList<Int>? = null
    val headerIndexes: List<Int> get() = _headerIndexes ?: emptyList()

    init {
        apply(content)
    }

    override fun items(
        count: Int,
        @Suppress("PrimitiveInLambda")
        key: ((index: Int) -> Any)?,
        @Suppress("PrimitiveInLambda")
        contentType: (index: Int) -> Any?,
        @Suppress("PrimitiveInLambda")
        itemContent: @Composable TvLazyListItemScope.(index: Int) -> Unit
    ) {
        intervals.addInterval(
            count,
            TvLazyListInterval(
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
        intervals.addInterval(
            1,
            TvLazyListInterval(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                item = { content() }
            )
        )
    }

    @ExperimentalTvFoundationApi
    override fun stickyHeader(
        key: Any?,
        contentType: Any?,
        content: @Composable TvLazyListItemScope.() -> Unit
    ) {
        val headersIndexes = _headerIndexes ?: mutableListOf<Int>().also {
            _headerIndexes = it
        }
        headersIndexes.add(intervals.size)

        item(key, contentType, content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class TvLazyListInterval(
    @Suppress("PrimitiveInLambda")
    override val key: ((index: Int) -> Any)?,
    @Suppress("PrimitiveInLambda")
    override val type: ((index: Int) -> Any?),
    @Suppress("PrimitiveInLambda")
    val item: @Composable TvLazyListItemScope.(index: Int) -> Unit
) : LazyLayoutIntervalContent.Interval
