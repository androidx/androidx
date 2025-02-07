/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.foundation.lazy

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.getScrollViewportLength
import androidx.compose.ui.semantics.horizontalScrollAxisRange
import androidx.compose.ui.semantics.indexForKey
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.scrollToIndex
import androidx.compose.ui.semantics.verticalScrollAxisRange
import androidx.wear.compose.foundation.lazy.layout.LazyLayoutItemProvider
import kotlinx.coroutines.launch

@Composable
internal fun Modifier.lazyLayoutSemantics(
    itemProviderLambda: () -> LazyLayoutItemProvider,
    state: LazyLayoutSemanticState,
    orientation: Orientation,
    userScrollEnabled: Boolean,
    reverseScrolling: Boolean,
): Modifier =
    this then
        LazyLayoutSemanticsModifierElement(
            itemProviderLambda = itemProviderLambda,
            state = state,
            orientation = orientation,
            userScrollEnabled = userScrollEnabled,
            reverseScrolling = reverseScrolling,
        )

internal interface LazyLayoutSemanticState {
    val viewport: Int
    val contentPadding: Int
    val scrollOffset: Float
    val maxScrollOffset: Float

    fun collectionInfo(): CollectionInfo

    suspend fun scrollToItem(index: Int)
}

internal fun TransformingLazyColumnSemanticState(
    state: TransformingLazyColumnState
): LazyLayoutSemanticState =
    object : LazyLayoutSemanticState {
        override val viewport: Int
            get() = state.layoutInfo.viewportSize.height

        override val contentPadding: Int
            get() =
                state.layoutInfoState.value.let { it.beforeContentPadding + it.afterContentPadding }

        override val scrollOffset: Float
            get() =
                with(state.layoutInfoState.value) {
                        if (anchorItemIndex == 0) {
                            return@with anchorItemScrollOffset
                        }
                        if (!canScrollForward) {
                            return@with maxScrollOffset
                        }
                        visibleItemsAverageHeight * anchorItemIndex +
                            anchorItemScrollOffset +
                            itemSpacing * (anchorItemIndex - 1)
                    }
                    .toFloat()
                    .coerceAtLeast(0f)

        override val maxScrollOffset: Float
            get() =
                state.layoutInfoState.value
                    .let {
                        if (it.visibleItems.isEmpty()) {
                            return@let 0
                        }
                        it.visibleItemsAverageHeight * it.totalItemsCount +
                            it.itemSpacing * (it.totalItemsCount - 1)
                    }
                    .toFloat()

        override fun collectionInfo(): CollectionInfo =
            CollectionInfo(rowCount = state.itemsCount, columnCount = 1)

        override suspend fun scrollToItem(index: Int) = state.scrollToItem(index)
    }

private class LazyLayoutSemanticsModifierElement(
    val itemProviderLambda: () -> LazyLayoutItemProvider,
    val state: LazyLayoutSemanticState,
    val orientation: Orientation,
    val userScrollEnabled: Boolean,
    val reverseScrolling: Boolean,
) : ModifierNodeElement<LazyLayoutSemanticsModifierNode>() {
    override fun create(): LazyLayoutSemanticsModifierNode =
        LazyLayoutSemanticsModifierNode(
            itemProviderLambda = itemProviderLambda,
            state = state,
            orientation = orientation,
            userScrollEnabled = userScrollEnabled,
            reverseScrolling = reverseScrolling,
        )

    override fun update(node: LazyLayoutSemanticsModifierNode) {
        node.update(
            itemProviderLambda = itemProviderLambda,
            state = state,
            orientation = orientation,
            userScrollEnabled = userScrollEnabled,
            reverseScrolling = reverseScrolling,
        )
    }

    override fun InspectorInfo.inspectableProperties() {
        // Not a public modifier.
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LazyLayoutSemanticsModifierElement) return false

        if (itemProviderLambda !== other.itemProviderLambda) return false
        if (state != other.state) return false
        if (orientation != other.orientation) return false
        if (userScrollEnabled != other.userScrollEnabled) return false
        if (reverseScrolling != other.reverseScrolling) return false

        return true
    }

    override fun hashCode(): Int {
        var result = itemProviderLambda.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + userScrollEnabled.hashCode()
        result = 31 * result + reverseScrolling.hashCode()
        return result
    }
}

private class LazyLayoutSemanticsModifierNode(
    private var itemProviderLambda: () -> LazyLayoutItemProvider,
    private var state: LazyLayoutSemanticState,
    private var orientation: Orientation,
    private var userScrollEnabled: Boolean,
    private var reverseScrolling: Boolean,
) : Modifier.Node(), SemanticsModifierNode {

    override val shouldAutoInvalidate: Boolean
        get() = false

    private val isVertical
        get() = orientation == Orientation.Vertical

    private val collectionInfo
        get() = state.collectionInfo()

    private lateinit var scrollAxisRange: ScrollAxisRange

    private val indexForKeyMapping: (Any) -> Int = { needle ->
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

    private var scrollToIndexAction: ((Int) -> Boolean)? = null

    init {
        updateCachedSemanticsValues()
    }

    fun update(
        itemProviderLambda: () -> LazyLayoutItemProvider,
        state: LazyLayoutSemanticState,
        orientation: Orientation,
        userScrollEnabled: Boolean,
        reverseScrolling: Boolean,
    ) {
        // These properties are only read lazily, so we don't need to invalidate
        // semantics if they change.
        this.itemProviderLambda = itemProviderLambda
        this.state = state

        // These properties are read when appling semantics, but don't need to rebuild the cache.
        if (this.orientation != orientation) {
            this.orientation = orientation
            invalidateSemantics()
        }

        // These values are used to build different cached values. If they, we need to rebuild the
        // cache.
        if (
            this.userScrollEnabled != userScrollEnabled || this.reverseScrolling != reverseScrolling
        ) {
            this.userScrollEnabled = userScrollEnabled
            this.reverseScrolling = reverseScrolling
            updateCachedSemanticsValues()
            invalidateSemantics()
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        isTraversalGroup = true
        indexForKey(indexForKeyMapping)

        if (isVertical) {
            verticalScrollAxisRange = scrollAxisRange
        } else {
            horizontalScrollAxisRange = scrollAxisRange
        }

        scrollToIndexAction?.let { scrollToIndex(action = it) }

        getScrollViewportLength { (state.viewport - state.contentPadding).toFloat() }

        collectionInfo = this@LazyLayoutSemanticsModifierNode.collectionInfo
    }

    private fun updateCachedSemanticsValues() {
        scrollAxisRange =
            ScrollAxisRange(
                value = { state.scrollOffset },
                maxValue = { state.maxScrollOffset },
                reverseScrolling = reverseScrolling
            )

        scrollToIndexAction =
            if (userScrollEnabled) {
                { index ->
                    coroutineScope.launch { state.scrollToItem(index) }
                    true
                }
            } else {
                null
            }
    }
}
