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

package androidx.compose.foundation.lazy.grid

import androidx.collection.IntList
import androidx.collection.MutableIntList
import androidx.collection.emptyIntList
import androidx.collection.mutableIntListOf
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

internal class LazyGridIntervalContent(content: LazyGridScope.() -> Unit) :
    LazyGridScope, LazyLayoutIntervalContent<LazyGridInterval>() {
    internal val spanLayoutProvider: LazyGridSpanLayoutProvider = LazyGridSpanLayoutProvider(this)

    override val intervals = MutableIntervalList<LazyGridInterval>()

    internal var hasCustomSpans = false

    private var _headerIndexes: MutableIntList? = null

    val headerIndexes: IntList
        get() = _headerIndexes ?: emptyIntList()

    init {
        apply(content)
    }

    override fun item(
        key: Any?,
        span: (LazyGridItemSpanScope.() -> GridItemSpan)?,
        contentType: Any?,
        content: @Composable LazyGridItemScope.() -> Unit
    ) {
        intervals.addInterval(
            1,
            LazyGridInterval(
                key = key?.let { { key } },
                span = span?.let { { span() } } ?: DefaultSpan,
                type = { contentType },
                item = { content() }
            )
        )
        if (span != null) hasCustomSpans = true
    }

    override fun items(
        count: Int,
        key: ((index: Int) -> Any)?,
        span: (LazyGridItemSpanScope.(Int) -> GridItemSpan)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable LazyGridItemScope.(index: Int) -> Unit
    ) {
        intervals.addInterval(
            count,
            LazyGridInterval(
                key = key,
                span = span ?: DefaultSpan,
                type = contentType,
                item = itemContent
            )
        )
        if (span != null) hasCustomSpans = true
    }

    override fun stickyHeader(
        key: Any?,
        contentType: Any?,
        content: @Composable LazyGridItemScope.(Int) -> Unit
    ) {
        val headersIndexes = _headerIndexes ?: mutableIntListOf().also { _headerIndexes = it }
        val headerIndex = intervals.size
        headersIndexes.add(headerIndex)
        item(key, { GridItemSpan(maxLineSpan) }, contentType) { content.invoke(this, headerIndex) }
    }

    private companion object {
        val DefaultSpan: LazyGridItemSpanScope.(Int) -> GridItemSpan = { GridItemSpan(1) }
    }
}

internal class LazyGridInterval(
    override val key: ((index: Int) -> Any)?,
    val span: LazyGridItemSpanScope.(Int) -> GridItemSpan,
    override val type: ((index: Int) -> Any?),
    val item: @Composable LazyGridItemScope.(Int) -> Unit
) : LazyLayoutIntervalContent.Interval
