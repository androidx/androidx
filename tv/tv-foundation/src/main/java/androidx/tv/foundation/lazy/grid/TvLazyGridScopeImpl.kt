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

package androidx.tv.foundation.lazy.grid

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.runtime.Composable

@Suppress("IllegalExperimentalApiUsage") // TODO (b/233188423): Address before moving to beta
@OptIn(ExperimentalFoundationApi::class)
internal class LazyGridIntervalContent(
    content: TvLazyGridScope.() -> Unit
) : TvLazyGridScope, LazyLayoutIntervalContent<LazyGridInterval>() {
    internal val spanLayoutProvider: LazyGridSpanLayoutProvider = LazyGridSpanLayoutProvider(this)

    override val intervals = MutableIntervalList<LazyGridInterval>()

    internal var hasCustomSpans = false

    init {
        apply(content)
    }

    override fun item(
        key: Any?,
        span: (TvLazyGridItemSpanScope.() -> TvGridItemSpan)?,
        contentType: Any?,
        content: @Composable TvLazyGridItemScope.() -> Unit
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
        span: (TvLazyGridItemSpanScope.(Int) -> TvGridItemSpan)?,
        contentType: (index: Int) -> Any?,
        itemContent: @Composable TvLazyGridItemScope.(index: Int) -> Unit
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

    private companion object {
        val DefaultSpan: TvLazyGridItemSpanScope.(Int) -> TvGridItemSpan = { TvGridItemSpan(1) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal class LazyGridInterval(
    override val key: ((index: Int) -> Any)?,
    val span: TvLazyGridItemSpanScope.(Int) -> TvGridItemSpan,
    override val type: ((index: Int) -> Any?),
    val item: @Composable TvLazyGridItemScope.(Int) -> Unit
) : LazyLayoutIntervalContent.Interval