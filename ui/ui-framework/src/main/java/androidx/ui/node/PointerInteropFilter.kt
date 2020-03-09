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
import android.view.View
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

internal fun Modifier.pointerInteropModifier(view: View): Modifier {
    return this + PointerInteropFilter(view)
}

internal class PointerInteropFilter(val view: View) : PointerInputModifier {

    private enum class DispatchToViewState {
        Unknown, Dispatching, NotDispatching
    }

    override val pointerInputFilter =
        object : PointerInputFilter() {
            private var state = DispatchToViewState.Unknown

            override fun onPointerInput(
                changes: List<PointerInputChange>,
                pass: PointerEventPass,
                bounds: IntPxSize
            ): List<PointerInputChange> {
                @Suppress("NAME_SHADOWING")
                var changes = changes

                if (state !== DispatchToViewState.NotDispatching) {
                    if (pass == PointerEventPass.InitialDown &&
                        changes.any {
                            it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
                        }
                    ) {
                        changes = dispatchToMotionEvent(changes)
                    }
                    if (pass == PointerEventPass.PostDown &&
                        changes.none {
                            it.changedToDownIgnoreConsumed() || it.changedToUpIgnoreConsumed()
                        }
                    ) {
                        changes = dispatchToMotionEvent(changes)
                    }
                }
                if (pass == PointerEventPass.PostDown) {
                    if (changes.all { it.changedToUpIgnoreConsumed() }) {
                        reset()
                    }
                }
                return changes
            }

            override fun onCancel() {
                // TODO(shepshapard): We have to know if the cancel event was caused by our View
                //  calling ViewParent#requestDisallowInterceptTouchEvent(boolean), or if we
                //  are cancelling for some other reason.  If our view called
                //  requestDisallowInterceptTouchEvent, we shouldn't cancel them.
                if (state === DispatchToViewState.Dispatching) {
                    emptyCancelMotionEventScope(
                        SystemClock.uptimeMillis().milliseconds
                    ) { motionEvent ->
                        view.dispatchTouchEvent(motionEvent)
                    }
                    reset()
                }
            }

            private fun reset() {
                state = DispatchToViewState.Unknown
            }

            private fun dispatchToMotionEvent(changes: List<PointerInputChange>):
                    List<PointerInputChange> {

                @Suppress("NAME_SHADOWING")
                var changes = changes

                if (changes.any { it.anyChangeConsumed() }) {
                    if (state === DispatchToViewState.Dispatching) {
                        changes.toCancelMotionEventScope { motionEvent ->
                            view.dispatchTouchEvent(motionEvent)
                        }
                    }
                    state = DispatchToViewState.NotDispatching
                } else {
                    changes.toMotionEventScope { motionEvent ->
                        state = if (view.dispatchTouchEvent(motionEvent)) {
                            DispatchToViewState.Dispatching
                        } else {
                            DispatchToViewState.NotDispatching
                        }
                    }
                    if (state === DispatchToViewState.Dispatching) {
                        changes = changes.map { it.consumeAllChanges() }
                    }
                }
                return changes
            }
        }
}