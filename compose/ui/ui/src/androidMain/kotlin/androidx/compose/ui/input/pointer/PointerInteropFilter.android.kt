/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.input.pointer

import android.os.SystemClock
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.view.MotionEvent.ACTION_HOVER_MOVE
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.view.ViewParent
import androidx.compose.runtime.remember
import androidx.compose.ui.AndroidComposeUiFlags
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.AndroidViewHolder

/**
 * A special PointerInputModifier that provides access to the underlying [MotionEvent]s originally
 * dispatched to Compose. Prefer [pointerInput] and use this only for interoperation with existing
 * code that consumes [MotionEvent]s.
 *
 * While the main intent of this Modifier is to allow arbitrary code to access the original
 * [MotionEvent] dispatched to Compose, for completeness, analogs are provided to allow arbitrary
 * code to interact with the system as if it were an Android View.
 *
 * This includes 2 APIs,
 * 1. [onTouchEvent] has a Boolean return type which is akin to the return type of
 *    [View.onTouchEvent]. If the provided [onTouchEvent] returns true, it will continue to receive
 *    the event stream (unless the event stream has been intercepted) and if it returns false, it
 *    will not.
 * 2. [requestDisallowInterceptTouchEvent] is a lambda that you can optionally provide so that you
 *    can later call it (yes, in this case, you call the lambda that you provided) which is akin to
 *    calling [ViewParent.requestDisallowInterceptTouchEvent]. When this is called, any associated
 *    ancestors in the tree that abide by the contract will act accordingly and will not intercept
 *    the even stream.
 *
 * @see [View.onTouchEvent]
 * @see [ViewParent.requestDisallowInterceptTouchEvent]
 */
public fun Modifier.pointerInteropFilter(
    requestDisallowInterceptTouchEvent: (RequestDisallowInterceptTouchEvent)? = null,
    onTouchEvent: (MotionEvent) -> Boolean,
): Modifier =
    composed(
        inspectorInfo =
            debugInspectorInfo {
                name = "pointerInteropFilter"
                properties["requestDisallowInterceptTouchEvent"] =
                    requestDisallowInterceptTouchEvent
                properties["onTouchEvent"] = onTouchEvent
            }
    ) {
        val filter = remember { PointerInteropFilter() }
        filter.onTouchEvent = onTouchEvent
        filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
        filter
    }

/**
 * Function that can be passed to [pointerInteropFilter] and then later invoked which provides an
 * analog to [ViewParent.requestDisallowInterceptTouchEvent].
 */
public class RequestDisallowInterceptTouchEvent : (Boolean) -> Unit {
    internal var pointerInteropFilter: PointerInteropFilter? = null

    public override fun invoke(disallowIntercept: Boolean) {
        pointerInteropFilter?.disallowIntercept = disallowIntercept
    }
}

/**
 * Similar to the 2 argument overload of [pointerInteropFilter], but connects directly to an
 * [AndroidViewHolder] for more seamless interop with Android.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun Modifier.pointerInteropFilter(view: AndroidViewHolder): Modifier {
    val filter = PointerInteropFilter()
    filter.onTouchEvent = { motionEvent ->
        when (motionEvent.actionMasked) {
            ACTION_DOWN,
            ACTION_POINTER_DOWN,
            ACTION_MOVE,
            ACTION_UP,
            ACTION_POINTER_UP,
            ACTION_OUTSIDE,
            ACTION_CANCEL -> view.dispatchTouchEvent(motionEvent)
            ACTION_HOVER_ENTER,
            ACTION_HOVER_MOVE,
            ACTION_HOVER_EXIT -> {
                val owner = view.layoutNode.owner as? AndroidComposeView
                // In AndroidComposeView.dispatchHoverEvent(), all hover MotionEvents (including
                // finger touch exploration and hardware mouse/stylus hover) are first sent to
                // AndroidComposeViewAccessibilityDelegateCompat.dispatchHoverEvent(). When touch
                // exploration is active, the accessibility delegate acts as the sole dispatcher of
                // hover events to androidViewsHandler: it forwards hover to uncovered interop views
                // and blocks hover when covered by Compose semantics nodes. Returning false here
                // avoids bypassing non-pointer-input Compose semantics overlays (e.g. Text) and
                // prevents duplicate hover dispatches to uncovered interop views.
                if (
                    AndroidComposeUiFlags.isInteropHoverZOrderEnabled &&
                        owner?.isTouchExplorationEnabled == true
                ) {
                    false
                } else {
                    view.dispatchGenericMotionEvent(motionEvent)
                }
            }
            // ACTION_BUTTON_PRESS,
            // ACTION_BUTTON_RELEASE,
            else -> view.dispatchGenericMotionEvent(motionEvent)
        }
    }
    val requestDisallowInterceptTouchEvent = RequestDisallowInterceptTouchEvent()
    filter.requestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    view.onRequestDisallowInterceptTouchEvent = requestDisallowInterceptTouchEvent
    return this.then(filter)
}

/**
 * The stateful part of pointerInteropFilter that manages the interop with Android.
 *
 * The intent of this PointerInputModifier is to allow Android Views and PointerInputModifiers to
 * interact seamlessly despite the differences in the 2 systems. Below is a detailed explanation for
 * how the interop is accomplished.
 *
 * When the type of event is not a movement event, we dispatch to the Android View as soon as
 * possible (during [PointerEventPass.Initial]) so that the Android View can react to down and up
 * events before Compose PointerInputModifiers normally would.
 *
 * When the type of event is a movement event, we start dispatching to the Android View during
 * [PointerEventPass.Initial] if we have unconsumed events. In order to respect the Compose nested
 * pointer input ordering, during this time we won't consume the event so Compose can take over if
 * needed during their Main pass, we only consume during the [PointerEventPass.Initial] pass if the
 * view requested disallow intercept.
 *
 * Whenever we are about to call [onTouchEvent], we check to see if anything in Compose consumed any
 * aspect of the pointer input changes, and if they did, we intercept the stream and dispatch
 * ACTION_CANCEL to the Android View if they have already returned true for a call to
 * View#dispatchTouchEvent(...).
 *
 * If we do call [onTouchEvent], and it returns true, we consume all of the changes so that nothing
 * in Compose also responds.
 *
 * If the [requestDisallowInterceptTouchEvent] is provided and called with true, we simply dispatch
 * move events during [PointerEventPass.Initial] so that normal PointerInputModifiers don't get a
 * chance to consume first. Note: This does mean that it is possible for a Compose
 * PointerInputModifier to "intercept" even after requestDisallowInterceptTouchEvent has been called
 * because consumption can occur during [PointerEventPass.Initial]. This may seem like a flaw, but
 * in reality, any PointerInputModifier that consumes that aggressively would likely only do so
 * after some consumption already occurred on a later pass, and this ability to do so is on par with
 * a [ViewGroup]'s ability to override [ViewGroup.dispatchTouchEvent] instead of overriding the more
 * usual [ViewGroup.onTouchEvent] and [ViewGroup .onInterceptTouchEvent].
 *
 * If [requestDisallowInterceptTouchEvent] is later called with false (the Android equivalent of
 * calling [ViewParent.requestDisallowInterceptTouchEvent] is exceedingly rare), we revert back to
 * the normal behavior.
 *
 * If all pointers go up on the pointer interop filter, parents will be set to be allowed to
 * intercept when new pointers go down. [requestDisallowInterceptTouchEvent] must be called again to
 * change that state.
 */
internal class PointerInteropFilter : PointerInputModifier {

    lateinit var onTouchEvent: (MotionEvent) -> Boolean

    var requestDisallowInterceptTouchEvent: RequestDisallowInterceptTouchEvent? = null
        set(value) {
            field?.pointerInteropFilter = null
            field = value
            field?.pointerInteropFilter = this
        }

    internal var disallowIntercept = false

    /** The 3 possible states */
    private enum class DispatchToViewState {
        /** We have yet to dispatch a new event stream to the child Android View. */
        Unknown,

        /**
         * We have dispatched to the child Android View and it wants to continue to receive events
         * for the current event stream.
         */
        Dispatching,

        /**
         * We intercepted the event stream, or the Android View no longer wanted to receive events
         * for the current event stream.
         */
        NotDispatching,
    }

    @OptIn(ExperimentalComposeUiApi::class)
    override val pointerInputFilter =
        object : PointerInputFilter() {

            private var state = DispatchToViewState.Unknown

            override val shareWithSiblings
                get() = true

            /**
             * Last pointer input event that was sent to the Main Pass. We save this so we don't
             * send the same event to the view over 2 different passes.
             */
            private var lastEventDispatchedToInitialPass: PointerEvent? = null

            /**
             * Tracks whether a synthetic [PointerEventType.Exit] occurred during an active trackpad
             * pan gesture.
             *
             * When a pan gesture exits a node's bounds, [HitPathTracker] first dispatches a
             * synthetic [PointerEventType.Exit] across all three passes (`Initial`, `Main`,
             * `Final`), immediately followed in the same frame by the [PointerEventType.PanMove]
             * event across all three passes, and then prunes the pointer from the node after
             * `PanMove`'s `Final` pass (meaning this filter will never receive
             * [PointerEventType.PanEnd]).
             *
             * We must wait until the `Final` pass of that [PointerEventType.PanMove] event (rather
             * than resetting during the `Final` pass of the preceding [PointerEventType.Exit]) to
             * call [stopDispatching] and [reset]; otherwise, resetting `state` to
             * [DispatchToViewState.Unknown] during `Exit` would cause the immediately following
             * `PanMove` to re-enter [dispatchToView], dispatching `ACTION_MOVE` after
             * `ACTION_CANCEL` and leaving `state` stuck after the node is pruned.
             */
            private var panExitedBounds = false

            override fun onPointerEvent(
                pointerEvent: PointerEvent,
                pass: PointerEventPass,
                bounds: IntSize,
            ) {
                // Ignore synthetic Enter/Exit events generated during a pan gesture, as the
                // underlying MotionEvent (ACTION_DOWN/MOVE/UP) will be dispatched by the actual
                // Pan events and dispatching synthetic hover events would result in duplicate
                // MotionEvent dispatches and improper state changes.
                if (
                    pointerEvent.internalPointerEvent?.activeGesture == PointerClassification.Pan &&
                        (pointerEvent.type == PointerEventType.Enter ||
                            pointerEvent.type == PointerEventType.Exit)
                ) {
                    if (pointerEvent.type == PointerEventType.Exit) {
                        panExitedBounds = true
                    }
                    return
                }

                val changes = pointerEvent.changes

                val isPanStartOrEnd =
                    pointerEvent.type == PointerEventType.PanStart ||
                        pointerEvent.type == PointerEventType.PanEnd

                val isMoveEvent =
                    !isPanStartOrEnd &&
                        changes.fastAll {
                            !it.changedToDownIgnoreConsumed() && !it.changedToUpIgnoreConsumed()
                        }

                val hasUnconsumedMove = isMoveEvent && changes.fastAll { !it.isConsumed }

                // If we were told to disallow intercept, or if the event was a down or up event,
                // we dispatch to Android as early as possible.  If the event is a move event and
                // we can still intercept, we dispatch to Android after we have a chance to
                // intercept due to movement.
                val dispatchDuringInitialTunnel =
                    disallowIntercept ||
                        isPanStartOrEnd ||
                        changes.fastAny {
                            it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
                        } ||
                        (hasUnconsumedMove)

                if (state !== DispatchToViewState.NotDispatching) {
                    if (pass == PointerEventPass.Initial && dispatchDuringInitialTunnel) {
                        lastEventDispatchedToInitialPass = pointerEvent
                        val shouldConsumeNow = !isMoveEvent || disallowIntercept
                        dispatchToView(pointerEvent, shouldConsumeNow)
                    }

                    // the view requested disallow during the initial pass,
                    // consume the events before we bubble them up to Compose.
                    if (
                        pass == PointerEventPass.Main &&
                            isMoveEvent &&
                            pointerEvent == lastEventDispatchedToInitialPass &&
                            disallowIntercept
                    ) {
                        changes.fastForEach { it.consume() }
                    }

                    if (
                        pass == PointerEventPass.Final &&
                            !dispatchDuringInitialTunnel &&
                            // this was already dispatched during the initial pass
                            pointerEvent != lastEventDispatchedToInitialPass
                    ) {
                        dispatchToView(pointerEvent, true)
                    }
                }
                if (pass == PointerEventPass.Final) {
                    // If a trackpad pan gesture exited the node's bounds, HitPathTracker will prune
                    // this node after the Final pass without delivering PanEnd. Cancel any active
                    // view dispatch and reset state now so neither the View nor this filter gets
                    // stuck in an active/suppressed gesture state.
                    if (panExitedBounds && pointerEvent.type != PointerEventType.PanEnd) {
                        if (state === DispatchToViewState.Dispatching) {
                            stopDispatching(pointerEvent)
                        }
                        reset()
                    } else if (
                        // If all of the changes were up changes, or if a trackpad pan gesture has
                        // ended, then the "event stream" has ended and we reset.
                        //
                        // Note: Trackpad pan gestures maintain pressed = false across the entire
                        // stream in MotionEventAdapter, which prevents changedToUpIgnoreConsumed()
                        // from returning true on ACTION_UP. Checking for PointerEventType.PanEnd is
                        // therefore required to properly reset the dispatching state when a pan
                        // gesture concludes.
                        changes.fastAll { it.changedToUpIgnoreConsumed() } ||
                            pointerEvent.type == PointerEventType.PanEnd
                    ) {
                        reset()
                    }

                    if (pointerEvent == lastEventDispatchedToInitialPass && isMoveEvent) {
                        // we've reached the final pass, if the motion event that was sent
                        // during the initial pass was consumed, it means Compose claimed it
                        // so we should stop dispatching to the View
                        if (changes.fastAny { it.isConsumed } && !disallowIntercept) {
                            stopDispatching(pointerEvent)
                        } else {
                            changes.fastForEach { it.consume() }
                        }
                    }
                }
            }

            override fun onCancel() {
                // If we are still dispatching to the Android View, we have to send them a
                // cancel event, otherwise, we should not.
                if (state === DispatchToViewState.Dispatching) {
                    emptyCancelMotionEventScope(SystemClock.uptimeMillis()) { motionEvent ->
                        onTouchEvent(motionEvent)
                    }
                }
                reset()
            }

            /** Resets all of our state to be ready for a "new event stream". */
            private fun reset() {
                state = DispatchToViewState.Unknown
                disallowIntercept = false
                lastEventDispatchedToInitialPass = null
                panExitedBounds = false
            }

            /**
             * Dispatches to the Android View.
             *
             * Also consumes aspects of [pointerEvent] and updates our [state] accordingly.
             *
             * Will dispatch ACTION_CANCEL if any aspect of [pointerEvent] has been consumed and
             * update our [state] accordingly.
             *
             * @param pointerEvent The change to dispatch.
             * @return The resulting changes (fully consumed or untouched).
             */
            private fun dispatchToView(pointerEvent: PointerEvent, shouldConsume: Boolean) {
                val changes = pointerEvent.changes

                if (changes.fastAny { it.isConsumed }) {
                    // We should no longer dispatch to the Android View.
                    stopDispatching(pointerEvent)
                } else {
                    // Dispatch and update our state with the result.
                    pointerEvent.toMotionEventScope(
                        this.layoutCoordinates?.localToRoot(Offset.Zero)
                            ?: error("layoutCoordinates not set")
                    ) { motionEvent ->
                        if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN) {
                            // If the action is ACTION_DOWN, we care about the return value of
                            // onTouchEvent and use it to set our initial dispatching state.
                            state =
                                if (onTouchEvent(motionEvent)) {
                                    DispatchToViewState.Dispatching
                                } else {
                                    DispatchToViewState.NotDispatching
                                }
                        } else {
                            // Otherwise, we don't care about the return value. This is intended
                            // to be in accordance with how the Android View system works.
                            onTouchEvent(motionEvent)
                        }
                    }
                    if (state === DispatchToViewState.Dispatching) {
                        // If the Android View claimed the event, consume all changes.
                        if (shouldConsume) changes.fastForEach { it.consume() }

                        pointerEvent.internalPointerEvent?.suppressMovementConsumption =
                            !disallowIntercept
                    }
                }
            }

            private fun stopDispatching(pointerEvent: PointerEvent) {
                if (state === DispatchToViewState.Dispatching) {
                    // If we were dispatching, send ACTION_CANCEL.
                    pointerEvent.toCancelMotionEventScope(
                        this.layoutCoordinates?.localToRoot(Offset.Zero)
                            ?: error("layoutCoordinates not set")
                    ) { motionEvent ->
                        onTouchEvent(motionEvent)
                    }
                }
                state = DispatchToViewState.NotDispatching
            }
        }
}

/**
 * Calls [watcher] with each [MotionEvent] that the layout area or any child [pointerInput]
 * receives. The [MotionEvent] may or may not have been transformed to the local coordinate system.
 * The Compose View will be considered as handling the [MotionEvent] in the area that the
 * [motionEventSpy] is active.
 *
 * This method can only be used to observe [MotionEvent]s and can not be used to capture an event
 * stream.
 *
 * [watcher] is called during the [PointerEventPass.Initial] pass.
 *
 * Developers should use [pointerInput] to handle pointer input processing within Compose.
 * [motionEventSpy] is only useful as part of Android View interoperability.
 *
 * If you need to handle and consume [MotionEvent]s, use [pointerInteropFilter].
 */
public fun Modifier.motionEventSpy(watcher: (motionEvent: MotionEvent) -> Unit): Modifier =
    this.pointerInput(watcher) {
        interceptOutOfBoundsChildEvents = true
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.motionEvent?.let(watcher)
            }
        }
    }
