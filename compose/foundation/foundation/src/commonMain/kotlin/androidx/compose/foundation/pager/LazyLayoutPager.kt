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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clipScrollableContainer
import androidx.compose.foundation.gestures.BringIntoViewScroller
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.gestures.snapping.SnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.IntervalList
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutIntervalContent
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap
import androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItem
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap
import androidx.compose.foundation.lazy.layout.lazyLayoutSemantics
import androidx.compose.foundation.overscroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import kotlin.math.absoluteValue
import kotlinx.coroutines.coroutineScope

@ExperimentalFoundationApi
@Composable
internal fun Pager(
    /** Modifier to be applied for the inner layout */
    modifier: Modifier,
    /** State controlling the scroll position */
    state: PagerState,
    /** The inner padding to be added for the whole content(not for each individual page) */
    contentPadding: PaddingValues,
    /** reverse the direction of scrolling and layout */
    reverseLayout: Boolean,
    /** The layout orientation of the Pager */
    orientation: Orientation,
    /** fling behavior to be used for flinging */
    flingBehavior: SnapFlingBehavior,
    /** Whether scrolling via the user gestures is allowed. */
    userScrollEnabled: Boolean,
    /** Number of pages to layout before and after the visible pages */
    beyondBoundsPageCount: Int = 0,
    /** Space between pages **/
    pageSpacing: Dp = 0.dp,
    /** Allows to change how to calculate the Page size **/
    pageSize: PageSize,
    /** A [NestedScrollConnection] that dictates how this [Pager] behaves with nested lists.  **/
    pageNestedScrollConnection: NestedScrollConnection,
    /** a stable and unique key representing the Page **/
    @Suppress("PrimitiveInLambda")
    key: ((index: Int) -> Any)?,
    /** The alignment to align pages horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal,
    /** The alignment to align pages vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical,
    /** The content of the list */
    @Suppress("PrimitiveInLambda")
    pageContent: @Composable PagerScope.(page: Int) -> Unit
) {
    require(beyondBoundsPageCount >= 0) {
        "beyondBoundsPageCount should be greater than or equal to 0, " +
            "you selected $beyondBoundsPageCount"
    }

    val overscrollEffect = ScrollableDefaults.overscrollEffect()

    val pagerItemProvider = rememberPagerItemProviderLambda(
        state = state,
        pageContent = pageContent,
        key = key
    ) { state.pageCount }

    val measurePolicy = rememberPagerMeasurePolicy(
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        orientation = orientation,
        beyondBoundsPageCount = beyondBoundsPageCount,
        pageSpacing = pageSpacing,
        pageSize = pageSize,
        horizontalAlignment = horizontalAlignment,
        verticalAlignment = verticalAlignment,
        itemProviderLambda = pagerItemProvider,
        pageCount = { state.pageCount },
    )

    val pagerFlingBehavior = remember(flingBehavior, state) {
        PagerWrapperFlingBehavior(flingBehavior, state)
    }

    val pagerSemantics = if (userScrollEnabled) {
        Modifier.pagerSemantics(state, orientation == Orientation.Vertical)
    } else {
        Modifier
    }

    val semanticState = rememberPagerSemanticState(
        state,
        reverseLayout,
        orientation == Orientation.Vertical
    )

    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
            .then(pagerSemantics)
            .lazyLayoutSemantics(
                itemProviderLambda = pagerItemProvider,
                state = semanticState,
                orientation = orientation,
                userScrollEnabled = userScrollEnabled,
                reverseScrolling = reverseLayout
            )
            .clipScrollableContainer(orientation)
            .pagerBeyondBoundsModifier(
                state,
                beyondBoundsPageCount,
                reverseLayout,
                orientation
            )
            .overscroll(overscrollEffect)
            .scrollable(
                orientation = orientation,
                reverseDirection = ScrollableDefaults.reverseDirection(
                    LocalLayoutDirection.current,
                    orientation,
                    reverseLayout
                ),
                interactionSource = state.internalInteractionSource,
                flingBehavior = pagerFlingBehavior,
                state = state,
                overscrollEffect = overscrollEffect,
                enabled = userScrollEnabled,
                bringIntoViewScroller = PagerBringIntoViewScroller
            )
            .dragDirectionDetector(state)
            .nestedScroll(pageNestedScrollConnection),
        measurePolicy = measurePolicy,
        prefetchState = state.prefetchState,
        itemProvider = pagerItemProvider
    )
}

@ExperimentalFoundationApi
internal class PagerLazyLayoutItemProvider(
    private val state: PagerState,
    private val intervalContent: LazyLayoutIntervalContent<PagerIntervalContent>,
    private val keyIndexMap: LazyLayoutKeyIndexMap,
) : LazyLayoutItemProvider {

    private val pagerScopeImpl = PagerScopeImpl

    override val itemCount: Int
        get() = intervalContent.itemCount

    @Composable
    override fun Item(index: Int, key: Any) {
        LazyLayoutPinnableItem(key, index, state.pinnedPages) {
            intervalContent.withInterval(index) { localIndex, content ->
                content.item(pagerScopeImpl, localIndex)
            }
        }
    }

    override fun getKey(index: Int): Any =
        keyIndexMap.getKey(index) ?: intervalContent.getKey(index)

    override fun getIndex(key: Any): Int = keyIndexMap.getIndex(key)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PagerLazyLayoutItemProvider) return false

        // the identity of this class is represented by intervalContent object.
        // having equals() allows us to skip items recomposition when intervalContent didn't change
        return intervalContent == other.intervalContent
    }

    override fun hashCode(): Int {
        return intervalContent.hashCode()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class PagerLayoutIntervalContent(
    @Suppress("PrimitiveInLambda")
    val pageContent: @Composable PagerScope.(page: Int) -> Unit,
    @Suppress("PrimitiveInLambda")
    val key: ((index: Int) -> Any)?,
    val pageCount: Int
) : LazyLayoutIntervalContent<PagerIntervalContent>() {
    override val intervals: IntervalList<PagerIntervalContent> =
        MutableIntervalList<PagerIntervalContent>().apply {
            addInterval(pageCount, PagerIntervalContent(key = key, item = pageContent))
        }
}

@OptIn(ExperimentalFoundationApi::class)
internal class PagerIntervalContent(
    @Suppress("PrimitiveInLambda")
    override val key: ((page: Int) -> Any)?,
    @Suppress("PrimitiveInLambda")
    val item: @Composable PagerScope.(page: Int) -> Unit
) : LazyLayoutIntervalContent.Interval

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberPagerItemProviderLambda(
    state: PagerState,
    @Suppress("PrimitiveInLambda")
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
    @Suppress("PrimitiveInLambda")
    key: ((index: Int) -> Any)?,
    @Suppress("PrimitiveInLambda")
    pageCount: () -> Int
): () -> PagerLazyLayoutItemProvider {
    val latestContent = rememberUpdatedState(pageContent)
    return remember(state, latestContent, key, pageCount) {
        val intervalContentState = derivedStateOf(referentialEqualityPolicy()) {
            PagerLayoutIntervalContent(latestContent.value, key, pageCount())
        }
        val itemProviderState = derivedStateOf(referentialEqualityPolicy()) {
            val intervalContent = intervalContentState.value
            val map = NearestRangeKeyIndexMap(state.nearestRange, intervalContent)
            PagerLazyLayoutItemProvider(
                state = state,
                intervalContent = intervalContent,
                keyIndexMap = map
            )
        }
        itemProviderState::value
    }
}

/**
 * A modifier to detect up and down events in a Pager.
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.dragDirectionDetector(state: PagerState) =
    this then Modifier.pointerInput(state) {
        coroutineScope {
            awaitEachGesture {
                val downEvent =
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var upEventOrCancellation: PointerInputChange? = null
                while (upEventOrCancellation == null) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.changes.fastAll { it.changedToUp() }) {
                        // All pointers are up
                        upEventOrCancellation = event.changes[0]
                    }
                }

                state.upDownDifference = upEventOrCancellation.position - downEvent.position
            }
        }
    }

@OptIn(ExperimentalFoundationApi::class)
private val PagerBringIntoViewScroller = object : BringIntoViewScroller {

    override val scrollAnimationSpec: AnimationSpec<Float> = spring()

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val trailingEdge = offset + size
        val leadingEdge = offset

        val sizeOfItemRequestingFocus = (trailingEdge - leadingEdge).absoluteValue
        val childSmallerThanParent = sizeOfItemRequestingFocus <= containerSize
        val initialTargetForLeadingEdge = 0.0f
        val spaceAvailableToShowItem = containerSize - initialTargetForLeadingEdge

        val targetForLeadingEdge =
            if (childSmallerThanParent && spaceAvailableToShowItem < sizeOfItemRequestingFocus) {
                containerSize - sizeOfItemRequestingFocus
            } else {
                initialTargetForLeadingEdge
            }

        return leadingEdge - targetForLeadingEdge
    }
}