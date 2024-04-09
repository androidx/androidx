/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.layout.LazyLayoutSemanticState
import androidx.compose.ui.semantics.CollectionInfo

internal fun LazyLayoutSemanticState(
    state: PagerState,
    isVertical: Boolean
): LazyLayoutSemanticState = object : LazyLayoutSemanticState {
    override val scrollOffset: Float
        get() = state.currentAbsoluteScrollOffset().toFloat()
    override val maxScrollOffset: Float
        get() = state.layoutInfo.calculateNewMaxScrollOffset(state.pageCount).toFloat()

    override suspend fun animateScrollBy(delta: Float): Float = state.animateScrollBy(delta)

    override suspend fun scrollToItem(index: Int) {
        state.scrollToPage(index)
    }

    override fun collectionInfo(): CollectionInfo =
        if (isVertical) {
            CollectionInfo(rowCount = state.pageCount, columnCount = 1)
        } else {
            CollectionInfo(rowCount = 1, columnCount = state.pageCount)
        }

    override val viewport: Int
        get() = if (state.layoutInfo.orientation == Orientation.Vertical) {
            state.layoutInfo.viewportSize.height
        } else {
            state.layoutInfo.viewportSize.width
        }
    override val contentPadding: Int
        get() = state.layoutInfo.beforeContentPadding + state.layoutInfo.afterContentPadding
}
