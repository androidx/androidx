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

package androidx.web.compose

import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScrollModifierNode
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.util.fastFirstOrNull
import kotlinx.coroutines.launch

/**
 * Connects [WebSurface] drags to nested scroll parents. [WebSurfaceNode] delegates to this node and
 * forwards it what the page didn't scroll, its pointer events, and when its drag ends.
 *
 * Chromium scrolls the page asynchronously, so we only find out what the page didn't scroll after
 * the fact, in [onPageOverScroll], and pass that on to parents in post scroll. That's enough for
 * parents that react to overscroll, like `LazyColumn` or `PullToRefreshBox` pulling out its
 * indicator. But they can't take any of it back, like pushing the indicator back up, as that needs
 * moves offered in pre scroll before the page scrolls with them.
 *
 * So once parents have taken some of the page's overscroll along an axis, we offer them later moves
 * along that axis in pre scroll, in [PointerEventPass.Initial] before [WebSurfaceNode] forwards
 * them to Chromium. If they take any of a move, the page mustn't scroll with it too, so the rest of
 * the gesture is handed over to them: we consume its events, which cancels the page's drag, and
 * dispatch its moves and the release fling to parents ourselves, along the axes they took
 * overscroll on.
 *
 * Parents that only take pre scroll, like collapsing top app bars, never take overscroll, so
 * they're never offered moves and see the same as they would without the handover.
 */
internal class NestedScrollHandoverNode : DelegatingNode(), CompositionLocalConsumerModifierNode {

    private val dispatcher = NestedScrollDispatcher()
    private val velocityTracker = VelocityTracker()

    private var phase = Phase.Page

    /** Whether parents took some of the page's horizontal overscroll during this gesture. */
    private var parentsTookX = false

    /** Whether parents took some of the page's vertical overscroll during this gesture. */
    private var parentsTookY = false

    /** The pointer whose moves we offer to parents. */
    private var pointerId: PointerId? = null

    init {
        delegate(nestedScrollModifierNode(NoOpNestedScrollConnection, dispatcher))
    }

    /**
     * Passes on to parents what the page didn't scroll: [delta] from a drag, or the [velocity] left
     * when a page fling reached an edge. [isDragging] is whether [WebSurfaceNode] still has the
     * drag.
     */
    fun onPageOverScroll(delta: Offset, velocity: Velocity, isDragging: Boolean) {
        // Once the page's touch is cancelled, because parents have the gesture or another node
        // took it, anything the page still reports is from moves that have since gone elsewhere.
        if (phase != Phase.Page) return
        if (velocity.x == 0f && velocity.y == 0f) {
            // Pre scroll needs a synchronous answer that we don't have, but we still dispatch it
            // with zero so parents get the full nested scroll cycle.
            dispatcher.dispatchPreScroll(Offset.Zero, NestedScrollSource.UserInput)
            val consumed =
                dispatcher.dispatchPostScroll(Offset.Zero, delta, NestedScrollSource.UserInput)
            if (consumed.x != 0f) parentsTookX = true
            if (consumed.y != 0f) parentsTookY = true
            // Chromium reports overscroll asynchronously, so it can arrive after the drag ended.
            // End the gesture again so parents settle with this delta too.
            if (!isDragging) dispatchPageFling(Velocity.Zero)
        } else {
            // Chromium stops the fling at the edge, so hand the rest to parents. This isn't
            // dispatched as SideEffect scrolls, as Pager throws to cancel those.
            dispatchPageFling(velocity)
        }
    }

    /** Called when [WebSurfaceNode] stops dragging. */
    fun onDragEnded() {
        // If parents have the gesture, we end it ourselves when it's released.
        if (phase != Phase.Page) return
        // Chromium works out the fling velocity itself and flings the page with it, so we have
        // nothing to offer parents in pre fling. We still end the gesture with zero velocity so
        // they can settle (snap an app bar, release pull-to-refresh). If the page's fling then
        // hits an edge, onPageOverScroll hands on what's left.
        dispatchPageFling(Velocity.Zero)
    }

    /**
     * Follows the gesture in [PointerEventPass.Initial], before [WebSurfaceNode] forwards events to
     * Chromium. While the page has the gesture we offer its moves to parents, and once parents have
     * it we dispatch its moves and release to them instead.
     */
    fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass) {
        if (pass != PointerEventPass.Initial) return
        val action = pointerEvent.motionEvent?.actionMasked ?: return
        when (action) {
            MotionEvent.ACTION_DOWN -> startGesture(pointerEvent)
            // A wheel scroll is new input for the page, so what it reports is current again.
            MotionEvent.ACTION_SCROLL -> if (phase == Phase.Done) phase = Phase.Page
        }
        if (pointerEvent.isConsumed) {
            // Another node took the gesture. If the page had it, WebSurfaceNode ends the drag.
            if (phase == Phase.Parents) endHandover(Velocity.Zero)
            return
        }
        val change = trackedChange(pointerEvent, action) ?: return
        velocityTracker.addPointerInputChange(change)
        when (phase) {
            Phase.Page -> if (action == MotionEvent.ACTION_MOVE) offerMove(pointerEvent, change)
            Phase.Parents -> {
                when (action) {
                    MotionEvent.ACTION_MOVE -> scrollParents(change.positionChange().onParentAxes())
                    MotionEvent.ACTION_UP -> endHandover(releaseVelocity().onParentAxes())
                }
                // Parents have the rest of the gesture, so none of it reaches the page. This comes
                // last, as positionChange() is zero once a change is consumed.
                pointerEvent.consume()
            }
            Phase.Done -> {}
        }
    }

    /** Ends the gesture if parents have it. If the page has it, [WebSurfaceNode] ends the drag. */
    fun onCancelPointerInput() {
        if (phase == Phase.Parents) endHandover(Velocity.Zero)
    }

    /**
     * Called when [WebSurfaceNode] cancels the page's touch, after ending its drag if it had one.
     * That happens when parents or another node take the gesture, or pointer input is cancelled, so
     * anything the page still reports from the gesture is stale and dropped until new input starts.
     */
    fun onPageTouchCancelled() {
        if (phase == Phase.Page) phase = Phase.Done
    }

    /** Starts following a new gesture from its [down] event. */
    private fun startGesture(down: PointerEvent) {
        phase = Phase.Page
        parentsTookX = false
        parentsTookY = false
        pointerId = down.changes.firstOrNull()?.id
    }

    /**
     * Returns the change for the pointer we follow. If it lifts while others are still down, we
     * follow one of those instead, like Compose drag detection does, and restart velocity tracking
     * so that the jump between the two pointers isn't taken for a fling.
     */
    private fun trackedChange(pointerEvent: PointerEvent, action: Int): PointerInputChange? {
        val change = pointerEvent.changes.fastFirstOrNull { it.id == pointerId } ?: return null
        if (action != MotionEvent.ACTION_POINTER_UP || change.pressed) return change
        val next = pointerEvent.changes.fastFirstOrNull { it.pressed } ?: return null
        pointerId = next.id
        velocityTracker.resetTracking()
        return next
    }

    /**
     * Offers a move to parents in pre scroll, along the axes they took overscroll on. If they take
     * any of it, the rest of the gesture is theirs.
     */
    private fun offerMove(pointerEvent: PointerEvent, change: PointerInputChange) {
        val delta = change.positionChange().onParentAxes()
        if (delta == Offset.Zero) return
        val consumed = dispatcher.dispatchPreScroll(delta, NestedScrollSource.UserInput)
        if (consumed == Offset.Zero) return
        // Consuming the move makes WebSurfaceNode cancel the page's drag.
        phase = Phase.Parents
        pointerEvent.consume()
        dispatcher.dispatchPostScroll(Offset.Zero, delta - consumed, NestedScrollSource.UserInput)
    }

    /** Scrolls parents with a move of a gesture they have, as a scrollable would. */
    private fun scrollParents(delta: Offset) {
        if (delta == Offset.Zero) return
        val consumed = dispatcher.dispatchPreScroll(delta, NestedScrollSource.UserInput)
        dispatcher.dispatchPostScroll(Offset.Zero, delta - consumed, NestedScrollSource.UserInput)
    }

    /** The velocity of the pointer we follow, to fling parents with when it's released. */
    private fun releaseVelocity(): Velocity {
        val max = currentValueOf(LocalViewConfiguration).maximumFlingVelocity
        return velocityTracker.calculateVelocity(Velocity(max, max))
    }

    /** Ends a gesture that parents have, flinging them with [velocity] as a scrollable would. */
    private fun endHandover(velocity: Velocity) {
        phase = Phase.Done
        dispatcher.coroutineScope.launch {
            val consumed = dispatcher.dispatchPreFling(velocity)
            dispatcher.dispatchPostFling(Velocity.Zero, velocity - consumed)
        }
    }

    /** The page flings itself, so parents only get the [available] velocity it didn't use. */
    private fun dispatchPageFling(available: Velocity) {
        dispatcher.coroutineScope.launch {
            dispatcher.dispatchPreFling(Velocity.Zero)
            dispatcher.dispatchPostFling(Velocity.Zero, available)
        }
    }

    /** Keeps only the axes that parents took overscroll on. */
    private fun Offset.onParentAxes() =
        Offset(if (parentsTookX) x else 0f, if (parentsTookY) y else 0f)

    /** Keeps only the axes that parents took overscroll on. */
    private fun Velocity.onParentAxes() =
        Velocity(if (parentsTookX) x else 0f, if (parentsTookY) y else 0f)

    private enum class Phase {
        /** Parents get what the page doesn't scroll. This is also the state between gestures. */
        Page,

        /** Parents have the rest of the gesture, and the page's drag is cancelled. */
        Parents,

        /**
         * The page's touch was cancelled, so anything it still reports from the gesture is dropped.
         * Either parents had the rest of the gesture, or another node took it.
         */
        Done,
    }
}

/** We only dispatch to parents and have no nested scroll children to react to. */
private val NoOpNestedScrollConnection = object : NestedScrollConnection {}
