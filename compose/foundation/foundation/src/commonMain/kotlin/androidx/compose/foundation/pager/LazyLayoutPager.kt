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
import androidx.compose.foundation.gestures.BringIntoViewSpec
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
import androidx.compose.ui.geometry.Offset
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
    /** Number of pages to compose and layout before and after the visible pages */
    beyondBoundsPageCount: Int = PagerDefaults.BeyondBoundsPageCount,
    /** Space between pages */
    pageSpacing: Dp = 0.dp,
    /** Allows to change how to calculate the Page size */
    pageSize: PageSize,
    /** A [NestedScrollConnection] that dictates how this [Pager] behaves with nested lists.  */
    pageNestedScrollConnection: NestedScrollConnection,
    /** a stable and unique key representing the Page */
    key: ((index: Int) -> Any)?,
    /** The alignment to align pages horizontally. Required when isVertical is true */
    horizontalAlignment: Alignment.Horizontal,
    /** The alignment to align pages vertically. Required when isVertical is false */
    verticalAlignment: Alignment.Vertical,
    /** The content of the list */
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

    val semanticState = rememberPagerSemanticState(
        state,
        reverseLayout,
        orientation == Orientation.Vertical
    )

    val pagerBringIntoViewSpec = remember(state) { PagerBringIntoViewSpec(state) }

    LazyLayout(
        modifier = modifier
            .then(state.remeasurementModifier)
            .then(state.awaitLayoutModifier)
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
                bringIntoViewSpec = pagerBringIntoViewSpec
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
    val pageContent: @Composable PagerScope.(page: Int) -> Unit,
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
    override val key: ((page: Int) -> Any)?,
    val item: @Composable PagerScope.(page: Int) -> Unit
) : LazyLayoutIntervalContent.Interval

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberPagerItemProviderLambda(
    state: PagerState,
    pageContent: @Composable PagerScope.(page: Int) -> Unit,
    key: ((index: Int) -> Any)?,
    pageCount: () -> Int
): () -> PagerLazyLayoutItemProvider {
    val latestContent = rememberUpdatedState(pageContent)
    val latestKey = rememberUpdatedState(key)
    return remember(state, latestContent, latestKey, pageCount) {
        val intervalContentState = derivedStateOf(referentialEqualityPolicy()) {
            PagerLayoutIntervalContent(latestContent.value, latestKey.value, pageCount())
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
                state.upDownDifference = Offset.Zero // Reset
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
private class PagerBringIntoViewSpec(val pagerState: PagerState) : BringIntoViewSpec {

    override val scrollAnimationSpec: AnimationSpec<Float> = spring()

    /**
     * [calculateScrollDistance] for Pager behaves differently than in a normal list. We must
     * always respect the snapped pages over bringing a child into view. The logic here will
     * behave like so:
     *
     * 1) If a child is outside of the view, start bringing it into view.
     * 2) If a child's trailing edge is outside of the page bounds and the child is smaller than
     * the page, scroll until the trailing edge is in view.
     * 3) Once a child is fully in view, if it is smaller than the page, scroll until the page is
     * settled.
     * 4) If the child is larger than the page, scroll until it is partially in view and continue
     * scrolling until the page is settled.
     */
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        return if (offset >= containerSize || offset < 0) {
            offset
        } else {
            if (size <= containerSize && (offset + size) > containerSize) {
                offset // bring into view
            } else {
                // are we in a settled position?
                if (pagerState.currentPageOffsetFraction.absoluteValue == 0.0f) {
                    0f
                } else {
                    offset
                }
            }
        }
    }
}
