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
import androidx.compose.runtime.Immutable

/**
 * Represents the span of an item in a [LazyVerticalGrid].
 */
@ExperimentalFoundationApi
@Suppress("INLINE_CLASS_DEPRECATED")
@Immutable
inline class GridItemSpan internal constructor(private val packedValue: Long) {
    /**
     * The span of the item on the current line. This will be the horizontal span for items of
     * [LazyVerticalGrid].
     */
    @ExperimentalFoundationApi
    val currentLineSpan: Int get() = packedValue.toInt()
}

/**
 * Creates a [GridItemSpan] with a specified [currentLineSpan]. This will be the horizontal span
 * for an item of a [LazyVerticalGrid].
 */
@ExperimentalFoundationApi
fun GridItemSpan(currentLineSpan: Int) = GridItemSpan(currentLineSpan.toLong())

/**
 * Scope of lambdas used to calculate the spans of items in lazy grids.
 */
@ExperimentalFoundationApi
interface LazyGridItemSpanScope {
    /**
     * The row of the item the span is calculated for.
     */
    @ExperimentalFoundationApi
    val itemRow: Int

    /**
     * The column of the item the span is calculated for.
     */
    @ExperimentalFoundationApi
    val itemColumn: Int

    /**
     * The max current line (horizontal for vertical grids) the item can occupy.
     */
    @ExperimentalFoundationApi
    val maxCurrentLineSpan: Int
}
