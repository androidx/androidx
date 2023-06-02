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

package androidx.compose.foundation.lazy

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

@OptIn(ExperimentalFoundationApi::class)
internal class LazyListIntervalContent(
    content: LazyListScope.() -> Unit,
) : LazyLayoutIntervalContent<LazyListInterval>(), LazyListScope {
    override val intervals: MutableIntervalList<LazyListInterval> = MutableIntervalList()

    private var _headerIndexes: MutableList<Int>? = null
    val headerIndexes: List<Int> get() = _headerIndexes ?: emptyList()

    init {
        apply(content)
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyItemScope.(index: Int) -> Unit
    ) {
        intervals.addInterval(
            count,
            LazyListInterval(
                key = key,
                type = contentType,
                item = itemContent
            )
        )
    }

    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        intervals.addInterval(
            1,
            LazyListInterval(
                key = if (key != null) { _: Int -> key } else null,
                type = { contentType },
                item = { content() }
            )
        )
    }

    @ExperimentalFoundationApi
    override fun stickyHeader(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyItemScope.() -> Unit
    ) {
        val headersIndexes = _headerIndexes ?: mutableListOf<Int>().also {
            _headerIndexes = it
        }
        headersIndexes.add(intervals.size)

        item(key, contentType, content)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyListInterval(
    @Suppress("PrimitiveInLambda")
    override val key: ((index: Int) -> Any)?,
    @Suppress("PrimitiveInLambda")
    override val type: ((index: Int) -> Any?),
    @Suppress("PrimitiveInLambda")
    val item: @Composable LazyItemScope.(index: Int) -> Unit
) : LazyLayoutIntervalContent.Interval
