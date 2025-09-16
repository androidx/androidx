/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.material3.adaptive.layout.internal.Strings
import androidx.compose.material3.adaptive.layout.internal.delegableSemantics
import androidx.compose.material3.adaptive.layout.internal.getString
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Creates and remembers a [DragToResizeState] instance.
 *
 * This function creates a state object that tracks and controls the resizing behavior of a
 * composable element via dragging. The state is saved and restored across recompositions and
 * configuration changes using [rememberSaveable].
 *
 * @param dockedEdge The edge to which the element is docked. This determines the orientation of the
 *   resizing operation (horizontal or vertical) and the direction of the size change when dragging.
 * @param minSize The minimum allowed size for the resizable element, as a [Dp]. Defaults to
 *   [Dp.Unspecified], which instructs scaffold to use its default setting.
 * @param maxSize The maximum allowed size for the resizable element, as a [Dp]. Defaults to
 *   [Dp.Unspecified]. Note that the dragged size cannot be larger than the scaffold's size, even if
 *   the max size set here is larger than the scaffold's size.
 */
@Composable
internal fun rememberDragToResizeState(
    dockedEdge: DockedEdge,
    minSize: Dp = Dp.Unspecified,
    maxSize: Dp = Dp.Unspecified,
): DragToResizeState {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    return rememberSaveable(
            dockedEdge,
            saver = DragToResizeState.Saver(dockedEdge, layoutDirection),
        ) {
            DragToResizeState(dockedEdge, layoutDirection)
        }
        .apply {
            this.coroutineScope = coroutineScope
            this.minSize =
                if (minSize.isSpecified) with(density) { minSize.roundToPx() } else Int.MIN_VALUE
            this.maxSize =
                if (maxSize.isSpecified) with(density) { maxSize.roundToPx() } else Int.MAX_VALUE
        }
}

private fun DragToResizeState(
    dockedEdge: DockedEdge,
    layoutDirection: LayoutDirection,
): DragToResizeState =
    when (dockedEdge) {
        DockedEdge.Top -> DragToResizeState.Top()
        DockedEdge.Bottom -> DragToResizeState.Bottom()
        DockedEdge.Start ->
            if (layoutDirection == LayoutDirection.Ltr) {
                DragToResizeState.Left()
            } else {
                DragToResizeState.Right()
            }
        DockedEdge.End ->
            if (layoutDirection == LayoutDirection.Ltr) {
                DragToResizeState.Right()
            } else {
                DragToResizeState.Left()
            }
    }

/**
 * A state object that can be used to control the resizing behavior of a pane.
 *
 * This class provides a way to track the current size of a resizable pane and to handle drag
 * interactions that modify the size. It supports both horizontal and vertical resizing.
 *
 * This state object is primarily designed for internal use within pane scaffolds.
 */
@Stable
internal abstract class DragToResizeState private constructor() : DraggableState {
    // TODO(conradchen): To figure out a better way to expose this in the relevant APIs
    internal var coroutineScope: CoroutineScope? = null

    internal var size
        get() = sizeInternal
        private set(value) {
            sizeInternal =
                if (sizeRange.isEmpty()) {
                    value
                } else if (value <= sizeRange.start) {
                    state = State.Collapsed
                    sizeRange.start
                } else if (value >= sizeRange.endInclusive) {
                    state = State.Expanded
                    sizeRange.endInclusive
                } else {
                    value
                }
        }

    internal var sizeInternal: Float by mutableFloatStateOf(Float.NaN)

    internal var state: State by mutableStateOf(State.Default)

    internal abstract val orientation: Orientation

    internal open fun getDraggedWidth(
        measuringWidth: Int,
        defaultMinWidth: Int,
        scaffoldWidth: Int,
    ) = measuringWidth

    internal open fun getDraggedHeight(
        measuringHeight: Int,
        defaultMinHeight: Int,
        scaffoldHeight: Int,
    ) = measuringHeight

    internal abstract val sizeRange: ClosedFloatingPointRange<Float>

    internal open fun convertDelta(delta: Float) = delta

    internal var widthRange: ClosedFloatingPointRange<Float> = 0f..0f
    internal var heightRange: ClosedFloatingPointRange<Float> = 0f..0f

    internal var minSize: Int = 0
    internal var maxSize: Int = 0

    private var lastMeasuredSize: Float = Float.NaN
    private var lastDraggedSize: Float = Float.NaN

    private val defaultSize
        get() = if (lastDraggedSize.isNaN()) lastMeasuredSize else lastDraggedSize

    private val dragMutex = MutatorMutex()

    private val dragScope =
        object : DragScope {
            override fun dragBy(pixels: Float): Unit = dispatchRawDelta(pixels)
        }

    internal fun animateTo(targetState: State) {
        if (state == targetState) {
            return
        }
        animateTo(getTargetSize(targetState))
        state = targetState
    }

    private fun animateTo(targetSize: Float) {
        when (state) {
            State.Default -> size = targetSize
            else -> {
                coroutineScope!!.launch {
                    animate(initialValue = size, targetValue = targetSize) { value, _ ->
                        size = value
                    }
                }
            }
        }
    }

    override suspend fun drag(dragPriority: MutatePriority, block: suspend DragScope.() -> Unit) =
        coroutineScope {
            dragMutex.mutateWith(dragScope, dragPriority, block)
        }

    override fun dispatchRawDelta(delta: Float) {
        if (Snapshot.withoutReadObservation { size.isNaN() }) {
            return
        }
        val actualDelta = convertDelta(delta)
        state = State.Dragged
        size += actualDelta
        lastDraggedSize = size
    }

    internal fun getAndUpdateDraggedSize(measuringSize: Int): Int {
        lastMeasuredSize = measuringSize.toFloat()
        if (size.isNaN()) {
                getTargetSize(state)
            } else {
                size // Note that we still need to set it again in this case to re-coerce the value
            }
            .also { size = it }
        return size.toInt()
    }

    private fun getTargetSize(targetState: State) =
        when (targetState) {
            State.Expanded -> sizeRange.endInclusive
            State.Collapsed -> sizeRange.start
            State.Default -> defaultSize
            else -> size
        }

    internal abstract class Horizontal : DragToResizeState() {
        override val sizeRange
            get() = widthRange

        override val orientation: Orientation = Orientation.Horizontal

        override fun getDraggedWidth(
            measuringWidth: Int,
            defaultMinWidth: Int,
            scaffoldWidth: Int,
        ): Int {
            val minWidth = if (minSize == Int.MIN_VALUE) defaultMinWidth else minSize
            this.widthRange = (minWidth..min(maxSize, scaffoldWidth)).toFloatRange()
            return getAndUpdateDraggedSize(measuringWidth)
        }
    }

    internal abstract class Vertical : DragToResizeState() {
        override val sizeRange
            get() = heightRange

        override val orientation: Orientation = Orientation.Vertical

        override fun getDraggedHeight(
            measuringHeight: Int,
            defaultMinHeight: Int,
            scaffoldHeight: Int,
        ): Int {
            val minHeight = if (minSize == Int.MIN_VALUE) defaultMinHeight else minSize
            this.heightRange = (minHeight..min(maxSize, scaffoldHeight)).toFloatRange()
            return getAndUpdateDraggedSize(measuringHeight)
        }
    }

    internal class Left : Horizontal()

    internal class Right : Horizontal() {
        override fun convertDelta(delta: Float) = -delta
    }

    internal class Top : Vertical()

    internal class Bottom : Vertical() {
        override fun convertDelta(delta: Float) = -delta
    }

    internal enum class State {
        Expanded,
        Collapsed,
        Dragged,
        Default,
    }

    companion object {
        internal fun Saver(
            dockedEdge: DockedEdge,
            layoutDirection: LayoutDirection,
        ): Saver<DragToResizeState, *> =
            Saver(
                save = {
                    listOf(
                        it.size,
                        it.state,
                        it.widthRange.start,
                        it.widthRange.endInclusive,
                        it.heightRange.start,
                        it.heightRange.endInclusive,
                    )
                },
                restore = {
                    DragToResizeState(dockedEdge, layoutDirection).also { state ->
                        state.size = it[0] as Float
                        state.state = it[1] as State
                        state.widthRange = (it[2] as Float)..(it[3] as Float)
                        state.heightRange = (it[4] as Float)..(it[5] as Float)
                    }
                },
            )
    }
}

/**
 * Represents the edge of a resizable pane that is docked, i.e. the edge that will stay in the same
 * position during resizing.
 *
 * For example if the edge is [DockedEdge.Top], the top edge of the pane won't move and the bottom
 * edge will be moveable to resize the pane.
 *
 * Note that [DragToResizeState] only supports resizing along one orientation according to their
 * [DockedEdge]. For example if [DockedEdge.Top] or [DockedEdge.Bottom] has been set, the resizing
 * can only happen along the vertical axis.
 */
internal enum class DockedEdge {
    /** The top edge of the pane is fixed, and resizing happens by moving the bottom edge. */
    Top,
    /** The bottom edge of the pane is fixed, and resizing happens by moving the top edge. */
    Bottom,
    /** The start edge of the pane is fixed, and resizing happens by moving the end edge. */
    Start,
    /** The end edge of the pane is fixed, and resizing happens by moving the start edge. */
    End,
}

/**
 * Apply this modifier to the clickable delegate of the drag-to-resize behavior to provide the
 * alternative input method of dragging to resize under the accessibility context. This modifier
 * will enable the associated composable to be clickable and switch through 3 states of
 * [DragToResizeState]: expanded, collapsed, and partially expanded in order.
 *
 * When in the expanded state, the pane associated with the [DragToResizeState] will be displayed in
 * its maximum possible size, bounded by the scaffold's size and the maximum size. When in the
 * collapsed state, the associated pane will be displayed in its minimum size. And in the partially
 * expanded size, the pane will be displayed in its default size or the size set by user with
 * dragging.
 *
 * @param state the associated [DragToResizeState] that this modifier will operate on.
 */
// TODO(conradchen): Figure out if we should publish it and the right API shape
internal fun Modifier.clickToResize(state: DragToResizeState) =
    this.clickable { state.animateTo(state.nextState) }
        .delegableSemantics {
            val nextState = state.nextState
            onClick(
                getString(
                    when (nextState) {
                        DragToResizeState.State.Expanded ->
                            Strings.dragToResizeClickToExpandDescription
                        DragToResizeState.State.Collapsed ->
                            Strings.dragToResizeClickToCollapseDescription
                        else -> Strings.dragToResizeClickToPartiallyExpandDescription
                    }
                )
            ) {
                state.animateTo(nextState)
                true
            }
            stateDescription =
                getString(
                    when (state.state) {
                        DragToResizeState.State.Expanded ->
                            Strings.dragToResizeExpandedStateDescription
                        DragToResizeState.State.Collapsed ->
                            Strings.dragToResizeCollapsedStateDescription
                        else -> Strings.dragToResizePartiallyExpandedStateDescription
                    }
                )
        }

private val DragToResizeState.nextState
    get() =
        when (state) {
            DragToResizeState.State.Expanded -> DragToResizeState.State.Collapsed
            DragToResizeState.State.Collapsed -> DragToResizeState.State.Default
            else -> DragToResizeState.State.Expanded
        }

private fun IntRange.toFloatRange() = first.toFloat()..last.toFloat()
