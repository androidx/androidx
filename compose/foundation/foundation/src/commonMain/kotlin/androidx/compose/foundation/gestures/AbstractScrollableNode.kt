/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.scrollBy
import androidx.compose.ui.semantics.scrollByOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastAny
import kotlinx.coroutines.launch

/** Base class for 1-D ([ScrollableNode]) and 2-D ([Scrollable2DNode]) scrollable nodes. */
internal abstract class AbstractScrollableNode(
    protected var overscrollEffect: OverscrollEffect?,
    protected var flingBehavior: FlingBehavior?,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    orientation: Orientation?,
) :
    DragGestureNode(
        canDrag = CanDragCalculation,
        enabled = enabled,
        interactionSource = interactionSource,
        orientation = orientation,
    ),
    SemanticsModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    // Placeholder fling behavior, we'll initialize it when the density is available.
    protected abstract val defaultFlingBehavior: ScrollableDefaultFlingBehavior

    protected abstract val scrollLogic: ScrollLogic

    protected val nestedScrollDispatcher = NestedScrollDispatcher()
    protected abstract val nestedScrollConnection: ScrollableNestedScrollConnection

    private var scrollByAction: ((x: Float, y: Float) -> Boolean)? = null
    private var scrollByOffsetAction: (suspend (Offset) -> Offset)? = null

    private var createdMouseWheelScrollingLogic: Boolean = false
    private var createdTrackpadScrollingLogic: Boolean = false

    private var mouseWheelScrollingLogic: NonTouchScrollingLogic? = null
    private var trackpadScrollingLogic: NonTouchScrollingLogic? = null

    /** Creates a new scrolling logic for mouse-wheel events, or `null` if not supported. */
    protected abstract fun createMouseWheelScrollingLogic(): NonTouchScrollingLogic?

    /** Creates a new scrolling logic for trackpad events, or `null` if not supported. */
    protected abstract fun createTrackpadScrollingLogic(): NonTouchScrollingLogic?

    protected fun initializeNestedScrollingDelegation() {
        delegate(nestedScrollModifierNode(nestedScrollConnection, nestedScrollDispatcher))
    }

    fun update(
        enabled: Boolean,
        overscrollEffect: OverscrollEffect?,
        flingBehavior: FlingBehavior?,
    ) {
        if (this.enabled != enabled) { // enabled changed
            nestedScrollConnection.enabled = enabled
            clearScrollSemanticsActions()
            invalidateSemantics()
        }

        this.overscrollEffect = overscrollEffect
        this.flingBehavior = flingBehavior
    }

    override fun onAttach() {
        super.onAttach()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
        trackpadScrollingLogic?.updateDensity(requireDensity())

        updateDefaultFlingBehavior()
    }

    private fun updateDefaultFlingBehavior() {
        if (!isAttached) return
        val density = requireDensity()
        defaultFlingBehavior.updateDensity(density)
    }

    override fun onDensityChange() {
        super<DragGestureNode>.onDensityChange()
        mouseWheelScrollingLogic?.updateDensity(requireDensity())
        trackpadScrollingLogic?.updateDensity(requireDensity())
        onCancelPointerInput()
        updateDefaultFlingBehavior()
    }

    private fun initializeMouseWheelScrollingLogic() {
        if (!createdMouseWheelScrollingLogic) {
            mouseWheelScrollingLogic = createMouseWheelScrollingLogic()
            createdMouseWheelScrollingLogic = true
        }

        mouseWheelScrollingLogic?.startReceivingEvents(coroutineScope)
    }

    private fun initializeTrackpadScrollingLogic() {
        if (!createdTrackpadScrollingLogic) {
            trackpadScrollingLogic = createTrackpadScrollingLogic()
            createdTrackpadScrollingLogic = true
        }

        trackpadScrollingLogic?.startReceivingEvents(coroutineScope)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize,
    ) {
        if (pointerEvent.changes.fastAny { canDrag.invoke(it.type) }) {
            super.onPointerEvent(pointerEvent, pass, bounds)
        }

        if (enabled) {
            initializePointerInputGestureCoordination()
            if (pass == PointerEventPass.Initial && pointerEvent.type == PointerEventType.Scroll) {
                initializeMouseWheelScrollingLogic()
            }
            mouseWheelScrollingLogic?.onPointerEvent(pointerEvent, pass, bounds)

            if (
                pass == PointerEventPass.Initial &&
                    (pointerEvent.type == PointerEventType.PanStart ||
                        pointerEvent.type == PointerEventType.PanMove ||
                        pointerEvent.type == PointerEventType.PanEnd)
            ) {
                initializeTrackpadScrollingLogic()
            }
            trackpadScrollingLogic?.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    protected abstract suspend fun semanticsScrollBy(offset: Offset): Offset

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (enabled && (scrollByAction == null || scrollByOffsetAction == null)) {
            setScrollSemanticsActions()
        }

        scrollByAction?.let { scrollBy(action = it) }

        scrollByOffsetAction?.let { scrollByOffset(action = it) }
    }

    private fun setScrollSemanticsActions() {
        scrollByAction = { x, y ->
            coroutineScope.launch { semanticsScrollBy(Offset(x, y)) }
            true
        }

        scrollByOffsetAction = { offset -> semanticsScrollBy(offset) }
    }

    protected fun clearScrollSemanticsActions() {
        scrollByAction = null
        scrollByOffsetAction = null
    }

    override fun onDragStarted(startedPosition: Offset) {}

    override fun startDragImmediately(): Boolean {
        return scrollLogic.shouldScrollImmediately()
    }
}

internal class ScrollableNestedScrollConnection(
    val scrollingLogic: ScrollLogic,
    var enabled: Boolean,
) : NestedScrollConnection {

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset =
        if (enabled) {
            scrollingLogic.performRawScroll(available)
        } else {
            Offset.Zero
        }

    @OptIn(ExperimentalFoundationApi::class)
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        return if (enabled) {
            val velocityLeft =
                if (scrollingLogic.isFlinging) {
                    Velocity.Zero
                } else {
                    scrollingLogic.doFlingAnimation(available)
                }
            available - velocityLeft
        } else {
            Velocity.Zero
        }
    }
}

// TODO: provide public way to drag by mouse (especially requested for Pager)
internal val CanDragCalculation: (PointerType) -> Boolean = { type -> type != PointerType.Mouse }
