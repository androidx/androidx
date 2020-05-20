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

package androidx.ui.node

import android.os.SystemClock
import androidx.ui.core.Modifier
import androidx.ui.core.PointerEventPass
import androidx.ui.core.PointerInputChange
import androidx.ui.core.anyChangeConsumed
import androidx.ui.core.changedToDownIgnoreConsumed
import androidx.ui.core.changedToUpIgnoreConsumed
import androidx.ui.core.consumeAllChanges
import androidx.ui.core.pointerinput.PointerInputFilter
import androidx.ui.core.pointerinput.PointerInputModifier
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.milliseconds
import androidx.ui.util.fastAny
import androidx.ui.viewinterop.AndroidViewHolder

internal fun Modifier.pointerInteropModifier(view: AndroidViewHolder): Modifier {
    return this + PointerInteropFilter(view)
}

/**
 * A special PointerInputModifier that manages pointer input interaction with child Android Views
 * following Android conventions.
 *
 * All interactions with the Android View occur through it's associated [AndroidViewHolder].
 *
 * When the type of event is not a movement event, we dispatch to the Android View as soon as
 * possible (during [PointerEventPass.InitialDown]) so that the Android View can react to down
 * and up events before Compose PointerInputModifiers normally would.
 *
 * When the type of event is a movement event, we dispatch to the Android View during
 * [PointerEventPass.PostDown] to allow Compose PointerInputModifiers to react to movement first,
 * which mimics a ViewParent intercepting the event stream.
 *
 * Whenever we are about to dispatch to the Android View, we check to see if anything in Compose
 * consumed any aspect of the pointer input changes, and if they did, we intercept the stream and
 * dispatch ACTION_CANCEL to the Android View if they have already returned true for a call to
 * View#dispatchTouchEvent(...).
 *
 * If we do dispatch to the View, and it returns true, we consume all of the changes so that
 * nothing in Compose also responds.
 *
 * If the Android View calls ViewParent#requestDisallowInterceptTouchEvent with a value of true, we
 * simply dispatch move events during [PointerEventPass.InitialDown] so that normal
 * PointerInputModifiers don't get a chance to consume first.  Note:  This does mean that it is
 * possible for a Compose PointerInputModifier to "intercept" even after
 * requestDisallowInterceptTouchEvent has been called because consumption can occur during
 * [PointerEventPass.InitialDown].  This may seem like a flaw, but in reality, any
 * PointerInputModifier that consumes that aggressively would likely only do so after some
 * consumption already occurred on a later pass, and this ability to do so is on par with a
 * ViewGroup's ability to override dispatchTouchEvent instead of overriding the more usual
 * onTouchEvent and onInterceptTouchEvent.
 *
 * If the Android View calls ViewParent#requestDisallowInterceptTouchEvent is later called again
 * but with false (exceedingly rare in Android), we revert back to the normal behavior.
 */
internal class PointerInteropFilter(
    /**
     * The [AndroidViewHolder] that contains the Android View we are dispatching to.
     */
    val view: AndroidViewHolder
) : PointerInputModifier {

    /**
     * The 3 possible states
     */
    private enum class DispatchToViewState {
        /**
         * We have yet to dispatch a new event stream to the child Android View.
         */
        Unknown,
        /**
         * We have dispatched to the child Android View and it wants to continue to receive
         * events for the current event stream.
         */
        Dispatching,
        /**
         * We intercepted the event stream, or the Android View no longer wanted to receive
         * events for the current event stream.
         */
        NotDispatching
    }

    override val pointerInputFilter =
        object : PointerInputFilter() {

            init {
                // Setup so that we are notified when the child Android View calls
                // ViewParent#requestDisallowInterceptTouchEvent.
                view.onRequestDisallowInterceptTouchEvent =
                    { disallowIntercept ->
                        this.disallowIntercept = disallowIntercept
                    }
            }

            private var state = DispatchToViewState.Unknown
            private var disallowIntercept = false

            override fun onPointerInput(
                changes: List<PointerInputChange>,
                pass: PointerEventPass,
                bounds: IntPxSize
            ): List<PointerInputChange> {
                @Suppress("NAME_SHADOWING")
                var changes = changes

                // If we were told to disallow intercept, or if the event was a down or up event,
                // we dispatch to Android as early as possible.  If the event is a move event and
                // we can still intercept, we dispatch to Android after we have a chance to
                // intercept due to movement.
                val dispatchDuringInitialTunnel = disallowIntercept ||
                        changes.fastAny {
                            it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
                        }

                if (state !== DispatchToViewState.NotDispatching) {
                    if (pass == PointerEventPass.InitialDown && dispatchDuringInitialTunnel) {
                        changes = dispatchToView(changes)
                    }
                    if (pass == PointerEventPass.PostDown && !dispatchDuringInitialTunnel) {
                        changes = dispatchToView(changes)
                    }
                }
                if (pass == PointerEventPass.PostDown) {
                    // If all of the changes were up changes, then the "event stream" has ended
                    // and we reset.
                    if (changes.all { it.changedToUpIgnoreConsumed() }) {
                        reset()
                    }
                }
                return changes
            }

            override fun onCancel() {
                // If we are still dispatching to the Android View, we have to send them a
                // cancel event, otherwise, we should not.
                if (state === DispatchToViewState.Dispatching) {
                    emptyCancelMotionEventScope(
                        SystemClock.uptimeMillis().milliseconds
                    ) { motionEvent ->
                        view.dispatchTouchEvent(motionEvent)
                    }
                    reset()
                }
            }

            /**
             * Resets all of our state to be ready for a "new event stream".
             */
            private fun reset() {
                state = DispatchToViewState.Unknown
                disallowIntercept = false
            }

            /**
             * Dispatches to the Android View.
             *
             * Also consumes aspects of [changes] and updates our [state] accordingly.
             *
             * Will dispatch ACTION_CANCEL if any aspect of [changes] has been consumed and
             * update our [state] accordingly.
             *
             * @param changes The changes to dispatch.
             * @return The resulting changes (fully consumed or untouched).
             */
            private fun dispatchToView(changes: List<PointerInputChange>):
                    List<PointerInputChange> {

                @Suppress("NAME_SHADOWING")
                var changes = changes

                if (changes.fastAny { it.anyChangeConsumed() }) {
                    // We should no longer dispatch to the Android View.
                    if (state === DispatchToViewState.Dispatching) {
                        // If we were dipatching, send ACTION_CANCEL.
                        changes.toCancelMotionEventScope { motionEvent ->
                            view.dispatchTouchEvent(motionEvent)
                        }
                    }
                    state = DispatchToViewState.NotDispatching
                } else {
                    // Dispatch and update our state with the result.
                    changes.toMotionEventScope { motionEvent ->
                        state = if (view.dispatchTouchEvent(motionEvent)) {
                            DispatchToViewState.Dispatching
                        } else {
                            DispatchToViewState.NotDispatching
                        }
                    }
                    if (state === DispatchToViewState.Dispatching) {
                        // If the Android View claimed the event, consume all changes.
                        changes = changes.map { it.consumeAllChanges() }
                    }
                }
                return changes
            }
        }
}