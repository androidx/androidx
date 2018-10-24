/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.ui.gestures.multidrag

import androidx.annotation.CallSuper
import androidx.ui.core.Duration
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureArenaEntry
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.drag.Drag
import androidx.ui.gestures.drag_details.DragEndDetails
import androidx.ui.gestures.drag_details.DragUpdateDetails
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.velocity_tracker.VelocityTracker

/**
 * Per-pointer state for a [MultiDragGestureRecognizer].
 *
 * A [MultiDragGestureRecognizer] tracks each pointer separately. The state for
 * each pointer is a subclass of [MultiDragPointerState].
 */
abstract class MultiDragPointerState(
    /** The global coordinates of the pointer when the pointer contacted the screen. */
    val initialPosition: Offset
) {

    private val velocityTracker = VelocityTracker()
    private var client: Drag? = null

    /**
     * The offset of the pointer from the last position that was reported to the client.
     *
     * After the pointer contacts the screen, the pointer might move some
     * distance before this movement will be recognized as a drag. This field
     * accumulates that movement so that we can report it to the client after
     * the drag starts.
     */
    var pendingDelta: Offset? = Offset.zero
        private set

    private var lastPendingEventTimestamp: Duration? = null

    private var arenaEntry: GestureArenaEntry? = null

    internal fun _setArenaEntry(arenaEntry: GestureArenaEntry) {
        assert(this.arenaEntry == null)
        assert(pendingDelta != null)
        assert(client == null)
        this.arenaEntry = arenaEntry
    }

    /** Resolve this pointer's entry in the [GestureArenaManager] with the given disposition. */
    @CallSuper
    internal open fun resolve(disposition: GestureDisposition) {
        arenaEntry!!.resolve(disposition)
    }

    internal fun move(event: PointerMoveEvent) {
        assert(arenaEntry != null)
        if (!event.synthesized)
            velocityTracker.addPosition(event.timeStamp, event.position)
        val client = client
        if (client != null) {
            assert(pendingDelta == null)
            // Call client last to avoid reentrancy.
            client.update(
                DragUpdateDetails(
                    sourceTimeStamp = event.timeStamp,
                    delta = event.delta,
                    globalPosition = event.position
                )
            )
        } else {
            assert(pendingDelta != null)
            pendingDelta = pendingDelta!! + event.delta
            lastPendingEventTimestamp = event.timeStamp
            checkForResolutionAfterMove()
        }
    }

    /**
     * Override this to call resolve() if the drag should be accepted or rejected.
     * This is called when a pointer movement is received, but only if the gesture
     * has not yet been resolved.
     */
    internal open fun checkForResolutionAfterMove() {}

    /**
     * Called when the gesture was accepted.
     *
     * Either immediately or at some future point before the gesture is disposed,
     * call starter(), passing it initialPosition, to start the drag.
     */
    internal abstract fun accepted(starter: GestureMultiDragStartCallback)

    /**
     * Called when the gesture was rejected.
     *
     * The [dispose] method will be called immediately following this.
     */
    @CallSuper
    internal open fun rejected() {
        assert(arenaEntry != null)
        assert(client == null)
        assert(pendingDelta != null)
        pendingDelta = null
        lastPendingEventTimestamp = null
        arenaEntry = null
    }

    internal fun startDrag(client: Drag) {
        assert(arenaEntry != null)
        assert(this.client == null)
        assert(pendingDelta != null)
        this.client = client
        val details = DragUpdateDetails(
            sourceTimeStamp = lastPendingEventTimestamp,
            delta = pendingDelta!!,
            globalPosition = initialPosition
        )
        pendingDelta = null
        lastPendingEventTimestamp = null
        // Call client last to avoid reentrancy.
        client.update(details)
    }

    internal fun up() {
        assert(arenaEntry != null)
        val client = client
        if (client != null) {
            assert(pendingDelta == null)
            val details = DragEndDetails(velocity = velocityTracker.getVelocity())
            this.client = null
            // Call client last to avoid reentrancy.
            client.end(details)
        } else {
            assert(pendingDelta != null)
            pendingDelta = null
            lastPendingEventTimestamp = null
        }
    }

    internal fun cancel() {
        assert(arenaEntry != null)
        val client = client
        if (client != null) {
            assert(pendingDelta == null)
            this.client = null
            // Call client last to avoid reentrancy.
            client.cancel()
        } else {
            assert(pendingDelta != null)
            pendingDelta = null
            lastPendingEventTimestamp = null
        }
    }

    /** Releases any resources used by the object. */
    @CallSuper
    internal open fun dispose() {
        arenaEntry?.resolve(GestureDisposition.rejected)
        arenaEntry = null
        // TODO(Migration/shepshapard): I think this is is supposed to change the programmatic flow
        // when we are running in a debug mode?  Seems like a bad idea?
        // assert(() { pendingDelta = null return true }())
    }
}
