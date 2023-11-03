/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.layout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.indexForKey
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.verticalScrollAxisRange
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Suppress("ComposableModifierFactory")
@Composable
internal fun Modifier.lazyLayoutSemantics(
    itemProviderLambda: () -> LazyLayoutItemProvider,
    state: LazyLayoutSemanticState,
    orientation: Orientation,
    userScrollEnabled: Boolean,
    reverseScrolling: Boolean
): Modifier {
    val coroutineScope = rememberCoroutineScope()
    return this.then(
        remember(
            itemProviderLambda,
            state,
            orientation,
            userScrollEnabled
        ) {
            val isVertical = orientation == Orientation.Vertical
            val indexForKeyMapping: (Any) -> Int = { needle ->
                val itemProvider = itemProviderLambda()
                var result = -1
                for (index in 0 until itemProvider.itemCount) {
                    if (itemProvider.getKey(index) == needle) {
                        result = index
                        break
                    }
                }
                result
            }

            val accessibilityScrollState = ScrollAxisRange(
                value = { state.pseudoScrollOffset() },
                maxValue = { state.pseudoMaxScrollOffset() },
                reverseScrolling = reverseScrolling
            )

            val scrollByAction: ((x: Float, y: Float) -> Boolean)? = if (userScrollEnabled) {
                { x, y ->
                    val delta = if (isVertical) {
                        y
                    } else {
                        x
                    }
                    coroutineScope.launch {
                        state.animateScrollBy(delta)
                    }
                    // TODO(aelias): is it important to return false if we know in advance we cannot scroll?
                    true
                }
            } else {
                null
            }

            val scrollToIndexAction: ((Int) -> Boolean)? = if (userScrollEnabled) {
                { index ->
                    val itemProvider = itemProviderLambda()
                    require(index >= 0 && index < itemProvider.itemCount) {
                        "Can't scroll to index $index, it is out of " +
                            "bounds [0, ${itemProvider.itemCount})"
                    }
                    coroutineScope.launch {
                        state.scrollToItem(index)
                    }
                    true
                }
            } else {
                null
            }

            val collectionInfo = state.collectionInfo()

            Modifier.semantics {
                isTraversalGroup = true
                indexForKey(indexForKeyMapping)

                if (isVertical) {
                    verticalScrollAxisRange = accessibilityScrollState
                } else {
                    horizontalScrollAxisRange = accessibilityScrollState
                }

                if (scrollByAction != null) {
                    scrollBy(action = scrollByAction)
                }

                if (scrollToIndexAction != null) {
                    scrollToIndex(action = scrollToIndexAction)
                }

                this.collectionInfo = collectionInfo
            }
        }
    )
}

internal interface LazyLayoutSemanticState {
    val firstVisibleItemScrollOffset: Int
    val firstVisibleItemIndex: Int
    val canScrollForward: Boolean
    fun collectionInfo(): CollectionInfo
    suspend fun animateScrollBy(delta: Float)
    suspend fun scrollToItem(index: Int)

    // It is impossible for lazy lists to provide an absolute scroll offset because the size of the
    // items above the viewport is not known, but the AccessibilityEvent system API expects one
    // anyway. So this provides a best-effort pseudo-offset that avoids breaking existing behavior.
    //
    // The key properties that A11y services are known to actually rely on are:
    // A) each scroll change generates a TYPE_VIEW_SCROLLED AccessibilityEvent
    // B) the integer offset in the AccessibilityEvent is different than the last one (note that the
    // magnitude and direction of the change does not matter for the known use cases)
    // C) scrollability is indicated by whether the scroll position is exactly 0 or exactly
    // maxScrollOffset
    //
    // To preserve property B) as much as possible, the constant 500 is chosen to be larger than a
    // single scroll delta would realistically be, while small enough to avoid losing precision due
    // to the 24-bit float significand of ScrollAxisRange with realistic list sizes (if there are
    // fewer than ~16000 items, the integer value is exactly preserved).
    fun pseudoScrollOffset() =
        (firstVisibleItemScrollOffset + firstVisibleItemIndex * 500).toFloat()

    fun pseudoMaxScrollOffset() =
        if (canScrollForward) {
            // If we can scroll further, indicate that by setting it slightly higher than
            // the current value
            pseudoScrollOffset() + 100
        } else {
            // If we can't scroll further, the current value is the max
            pseudoScrollOffset()
        }.toFloat()
}
