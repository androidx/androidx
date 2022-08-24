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

package androidx.tv.foundation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.OnRemeasuredModifier
import androidx.compose.ui.modifier.ModifierLocalProvider
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/* Copied from
 compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/gestures/
 Scrollable.kt and modified */

/**
 * Configure touch scrolling and flinging for the UI element in a single [Orientation].
 *
 * Users should update their state themselves using default [ScrollableState] and its
 * `consumeScrollDelta` callback or by implementing [ScrollableState] interface manually and reflect
 * their own state in UI when using this component.
 *
 * @param state [ScrollableState] state of the scrollable. Defines how scroll events will be
 * interpreted by the user land logic and contains useful information about on-going events.
 * @param orientation orientation of the scrolling
 * @param pivotOffsets offsets of child element within the parent and starting edge of the child
 * from the pivot defined by the parentOffset.
 * @param enabled whether or not scrolling in enabled
 * @param reverseDirection reverse the direction of the scroll, so top to bottom scroll will
 * behave like bottom to top and left to right will behave like right to left.
 * drag events when this scrollable is being dragged.
 */

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalTvFoundationApi
fun Modifier.scrollableWithPivot(
    state: ScrollableState,
    orientation: Orientation,
    pivotOffsets: PivotOffsets,
    enabled: Boolean = true,
    reverseDirection: Boolean = false
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "scrollableWithPivot"
        properties["orientation"] = orientation
        properties["state"] = state
        properties["enabled"] = enabled
        properties["reverseDirection"] = reverseDirection
        properties["pivotOffsets"] = pivotOffsets
    },
    factory = {
        val coroutineScope = rememberCoroutineScope()
        val keepFocusedChildInViewModifier =
            remember(coroutineScope, orientation, state, reverseDirection) {
                ContentInViewModifier(
                    coroutineScope, orientation, state, reverseDirection, pivotOffsets)
            }

        Modifier
            .focusGroup()
            .then(keepFocusedChildInViewModifier.modifier)
            .pointerScrollable(
                orientation,
                reverseDirection,
                state,
                enabled
            )
            .then(if (enabled) ModifierLocalScrollableContainerProvider else Modifier)
    }
)

@Suppress("ComposableModifierFactory")
@Composable
private fun Modifier.pointerScrollable(
    orientation: Orientation,
    reverseDirection: Boolean,
    controller: ScrollableState,
    enabled: Boolean
): Modifier {
    val nestedScrollDispatcher = remember { mutableStateOf(NestedScrollDispatcher()) }
    val scrollLogic = rememberUpdatedState(
        ScrollingLogic(
            orientation,
            reverseDirection,
            controller
        )
    )
    val nestedScrollConnection = remember(enabled) {
        scrollableNestedScrollConnection(scrollLogic, enabled)
    }

    return this.nestedScroll(nestedScrollConnection, nestedScrollDispatcher.value)
}

private class ScrollingLogic(
    val orientation: Orientation,
    val reverseDirection: Boolean,
    val scrollableState: ScrollableState,
) {
    private fun Float.toOffset(): Offset = when {
        this == 0f -> Offset.Zero
        orientation == Horizontal -> Offset(this, 0f)
        else -> Offset(0f, this)
    }

    private fun Offset.toFloat(): Float =
        if (orientation == Horizontal) this.x else this.y
    private fun Float.reverseIfNeeded(): Float = if (reverseDirection) this * -1 else this

    fun performRawScroll(scroll: Offset): Offset {
        return if (scrollableState.isScrollInProgress) {
            Offset.Zero
        } else {
            scrollableState.dispatchRawDelta(scroll.toFloat().reverseIfNeeded())
                .reverseIfNeeded().toOffset()
        }
    }
}

private fun scrollableNestedScrollConnection(
    scrollLogic: State<ScrollingLogic>,
    enabled: Boolean
): NestedScrollConnection = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset = if (enabled) {
        scrollLogic.value.performRawScroll(available)
    } else {
        Offset.Zero
    }
}

/**
 * Handles any logic related to bringing or keeping content in view, including
 * [BringIntoViewResponder] and ensuring the focused child stays in view when the scrollable area
 * is shrunk.
 */
@OptIn(ExperimentalFoundationApi::class)
private class ContentInViewModifier(
    private val scope: CoroutineScope,
    private val orientation: Orientation,
    private val scrollableState: ScrollableState,
    private val reverseDirection: Boolean,
    private val pivotOffsets: PivotOffsets
) : BringIntoViewResponder, OnRemeasuredModifier, OnPlacedModifier {
    private var focusedChild: LayoutCoordinates? = null
    private var coordinates: LayoutCoordinates? = null
    private var oldSize: IntSize? = null

    val modifier: Modifier = this
        .onFocusedBoundsChanged { focusedChild = it }
        .bringIntoViewResponder(this)

    override fun onRemeasured(size: IntSize) {
        val coordinates = coordinates
        val oldSize = oldSize
        // We only care when this node becomes smaller than it previously was, so don't care about
        // the initial measurement.
        if (oldSize != null && oldSize != size && coordinates?.isAttached == true) {
            onSizeChanged(coordinates, oldSize)
        }
        this.oldSize = size
    }

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun calculateRectForParent(localRect: Rect): Rect {
        val oldSize = checkNotNull(oldSize) {
            "Expected BringIntoViewRequester to not be used before parents are placed."
        }
        // oldSize will only be null before the initial measurement.
        return computeDestination(localRect, oldSize, pivotOffsets)
    }

    override suspend fun bringChildIntoView(localRect: Rect) {
        performBringIntoView(localRect, calculateRectForParent(localRect))
    }

    private fun onSizeChanged(coordinates: LayoutCoordinates, oldSize: IntSize) {
        val containerShrunk = if (orientation == Horizontal) {
            coordinates.size.width < oldSize.width
        } else {
            coordinates.size.height < oldSize.height
        }
        // If the container is growing, then if the focused child is only partially visible it will
        // soon be _more_ visible, so don't scroll.
        if (!containerShrunk) return

        val focusedBounds = focusedChild
            ?.let { coordinates.localBoundingBoxOf(it, clipBounds = false) }
            ?: return
        val myOldBounds = Rect(Offset.Zero, oldSize.toSize())
        val adjustedBounds = computeDestination(focusedBounds, coordinates.size, pivotOffsets)
        val wasVisible = myOldBounds.overlaps(focusedBounds)
        val isFocusedChildClipped = adjustedBounds != focusedBounds

        if (wasVisible && isFocusedChildClipped) {
            scope.launch {
                performBringIntoView(focusedBounds, adjustedBounds)
            }
        }
    }

    /**
     * Compute the destination given the source rectangle and current bounds.
     *
     * @param source The bounding box of the item that sent the request to be brought into view.
     * @param pivotOffsets offsets of child element within the parent and starting edge of the child
     * from the pivot defined by the parentOffset.
     * @return the destination rectangle.
     */
    private fun computeDestination(
        source: Rect,
        intSize: IntSize,
        pivotOffsets: PivotOffsets
    ): Rect {
        val size = intSize.toSize()
        return when (orientation) {
            Vertical ->
                source.translate(
                    0f,
                    relocationDistance(source.top, source.bottom, size.height, pivotOffsets))
            Horizontal ->
                source.translate(
                    relocationDistance(source.left, source.right, size.width, pivotOffsets),
                    0f)
        }
    }

    /**
     * Using the source and destination bounds, perform an animated scroll.
     */
    private suspend fun performBringIntoView(source: Rect, destination: Rect) {
        val offset = when (orientation) {
            Vertical -> source.top - destination.top
            Horizontal -> source.left - destination.left
        }
        val scrollDelta = if (reverseDirection) -offset else offset

        // Note that this results in weird behavior if called before the previous
        // performBringIntoView finishes due to b/220119990.
        scrollableState.animateScrollBy(scrollDelta)
    }

    /**
     * Calculate the offset needed to bring one of the edges into view. The leadingEdge is the side
     * closest to the origin (For the x-axis this is 'left', for the y-axis this is 'top').
     * The trailing edge is the other side (For the x-axis this is 'right', for the y-axis this is
     * 'bottom').
     */
    private fun relocationDistance(
        leadingEdgeOfItemRequestingFocus: Float,
        trailingEdgeOfItemRequestingFocus: Float,
        parentSize: Float,
        pivotOffsets: PivotOffsets
    ): Float {
        val totalWidthOfItemRequestingFocus =
            trailingEdgeOfItemRequestingFocus - leadingEdgeOfItemRequestingFocus
        val pivotOfItemRequestingFocus =
            pivotOffsets.childFraction * totalWidthOfItemRequestingFocus
        val intendedLocationOfItemRequestingFocus = parentSize * pivotOffsets.parentFraction

        return leadingEdgeOfItemRequestingFocus - intendedLocationOfItemRequestingFocus +
            pivotOfItemRequestingFocus
    }
}

// TODO: b/203141462 - make this public and move it to ui
/**
 * Whether this modifier is inside a scrollable container, provided by
 * [Modifier.scrollableWithPivot]. Defaults to false.
 */
internal val ModifierLocalScrollableContainer = modifierLocalOf { false }

private object ModifierLocalScrollableContainerProvider : ModifierLocalProvider<Boolean> {
    override val key = ModifierLocalScrollableContainer
    override val value = true
}
