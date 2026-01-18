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

import android.view.View
import androidx.pdf.ink.view.draganddrop.ToolbarCoordinator
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.matcher.ViewMatchers.withId

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
        onView(withId(toolbarId))
            .perform(
                GeneralSwipeAction(
                    Swipe.FAST,
                    GeneralLocation.CENTER,
                    { view ->
                        val coordinator = view.parent as View
                        val screenPos = IntArray(2)
                        coordinator.getLocationOnScreen(screenPos)
                        val x: Float
                        val y: Float

                        when (to) {
                            DragTarget.LEFT -> {
                                x = screenPos[0].toFloat() + DELTA_FROM_BOUNDARY
                                y = screenPos[1].toFloat() + (coordinator.height / 2f)
                            }
                            DragTarget.RIGHT -> {
                                x = screenPos[0].toFloat() + coordinator.width - DELTA_FROM_BOUNDARY
                                y = screenPos[1].toFloat() + (coordinator.height / 2f)
                            }
                            DragTarget.BOTTOM -> {
                                x = screenPos[0].toFloat() + (coordinator.width / 2f)
                                y =
                                    screenPos[1].toFloat() + coordinator.height -
                                        DELTA_FROM_BOUNDARY
                            }
                        }
                        floatArrayOf(x, y)
                    },
                    Press.FINGER,
                )
            )
    }

    enum class DragTarget {
        LEFT,
        RIGHT,
        BOTTOM,
    }
}
