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

package androidx.pdf.util

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.pdf.ink.view.draganddrop.ToolbarCoordinator
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.Matcher

internal object ToolbarViewActions {

    // Space from boundary of the view
    private const val DELTA_FROM_BOUNDARY = 10f

    /**
     * Performs a drag and drop action from the center of the toolbar to a specified target edge of
     * the [ToolbarCoordinator].
     *
     * @param to The target edge to drag the toolbar towards.
     */
    fun performDragAndDrop(toolbarId: Int, to: DragTarget) {
        onView(ViewMatchers.withId(toolbarId))
            .perform(
                object : ViewAction {
                    override fun getConstraints(): Matcher<View> = ViewMatchers.isDisplayed()

                    override fun getDescription(): String = "Long press and drag toolbar to $to"

                    override fun perform(uiController: UiController, view: View) {
                        val screenPos = IntArray(2)
                        view.getLocationOnScreen(screenPos)

                        // Calculate Start (Center of toolbar)
                        val startX = screenPos[0] + view.width / 2f
                        val startY = screenPos[1] + view.height / 2f

                        // Calculate End (Target edge of Coordinator)
                        val coordinator = view.parent as View
                        val coordPos = IntArray(2)
                        coordinator.getLocationOnScreen(coordPos)

                        val endX: Float
                        val endY: Float
                        when (to) {
                            DragTarget.LEFT -> {
                                endX = coordPos[0].toFloat() + DELTA_FROM_BOUNDARY
                                endY = coordPos[1].toFloat() + (coordinator.height / 2f)
                            }
                            DragTarget.RIGHT -> {
                                endX =
                                    coordPos[0].toFloat() + coordinator.width - DELTA_FROM_BOUNDARY
                                endY = coordPos[1].toFloat() + (coordinator.height / 2f)
                            }
                            DragTarget.BOTTOM -> {
                                endX = coordPos[0].toFloat() + (coordinator.width / 2f)
                                endY =
                                    coordPos[1].toFloat() + coordinator.height - DELTA_FROM_BOUNDARY
                            }
                        }

                        // --- Dispatch Events ---
                        val downTime = SystemClock.uptimeMillis()

                        // Trigger point: ACTION_DOWN
                        val downEvent =
                            MotionEvent.obtain(
                                downTime,
                                downTime,
                                MotionEvent.ACTION_DOWN,
                                startX,
                                startY,
                                0,
                            )
                        view.dispatchTouchEvent(downEvent)
                        downEvent.recycle()

                        // We don't have to wait for long press to trigger, as for tests we've
                        // short-circuited drag route by disabling animations.

                        // ACTION_MOVE (Drag to destination)
                        val moveTime = SystemClock.uptimeMillis()
                        val moveEvent =
                            MotionEvent.obtain(
                                downTime,
                                moveTime,
                                MotionEvent.ACTION_MOVE,
                                endX,
                                endY,
                                0,
                            )
                        view.dispatchTouchEvent(moveEvent)
                        moveEvent.recycle()

                        // ACTION_UP (Release)
                        val upTime = SystemClock.uptimeMillis()
                        val upEvent =
                            MotionEvent.obtain(
                                downTime,
                                upTime,
                                MotionEvent.ACTION_UP,
                                endX,
                                endY,
                                0,
                            )
                        view.dispatchTouchEvent(upEvent)
                        upEvent.recycle()
                    }
                }
            )
    }

    enum class DragTarget {
        LEFT,
        RIGHT,
        BOTTOM,
    }
}
