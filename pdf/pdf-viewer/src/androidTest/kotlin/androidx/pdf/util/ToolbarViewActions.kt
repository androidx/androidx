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

import android.app.Activity
import android.graphics.PointF
import android.view.View
import androidx.pdf.view.annotation.AnnotationToolbar
import androidx.pdf.view.annotation.draganddrop.ToolbarCoordinator
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice

/**
 * Helper actions for performing drag and drop on an [AnnotationToolbar] within a
 * [ToolbarCoordinator].
 */
internal object ToolbarViewActions {

    /** Offset from the edge of the coordinator to avoid triggering system gesture insets. */
    private const val GESTURE_SAFE_BOUNDARY_OFFSET = 60f

    /** Number of steps for [androidx.test.uiautomator.UiDevice.drag] gesture animation. */
    private const val DRAG_STEPS = 40

    /** Target dock edges for drag-and-drop actions. */
    enum class DragTarget {
        LEFT,
        RIGHT,
        BOTTOM,
    }

    /**
     * Drags the toolbar identified by [toolbarId] in [activity] to the specified [to] edge of its
     * parent coordinator.
     *
     * @param activity active host activity containing the toolbar view
     * @param toolbarId ID of the toolbar view to drag
     * @param to target edge to drag the toolbar towards
     */
    fun performDragAndDrop(activity: Activity, toolbarId: Int, to: DragTarget) {
        val (start, end) = calculateDragCoordinates(activity, toolbarId, to)
        val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiDevice.drag(start.x.toInt(), start.y.toInt(), end.x.toInt(), end.y.toInt(), DRAG_STEPS)
    }

    /** Calculates screen coordinates for starting and ending a drag-and-drop gesture. */
    private fun calculateDragCoordinates(
        activity: Activity,
        toolbarId: Int,
        to: DragTarget,
    ): Pair<PointF, PointF> {
        var start = PointF()
        var end = PointF()

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view =
                checkNotNull(activity.findViewById<View>(toolbarId)) {
                    "View with ID $toolbarId not found in activity"
                }

            val toolbarCoords = IntArray(2).also { view.getLocationOnScreen(it) }
            start = PointF(toolbarCoords[0] + view.width / 2f, toolbarCoords[1] + view.height / 2f)

            val coordinator = view.parent as View
            val coordinatorCoords = IntArray(2).also { coordinator.getLocationOnScreen(it) }

            end = calculateTargetCoordinates(to, coordinator, coordinatorCoords)
        }

        return Pair(start, end)
    }

    /** Calculates target drop coordinates based on [DragTarget] and coordinator bounds. */
    private fun calculateTargetCoordinates(
        to: DragTarget,
        coordinator: View,
        coordinatorCoords: IntArray,
    ): PointF =
        when (to) {
            DragTarget.LEFT ->
                PointF(
                    coordinatorCoords[0] + GESTURE_SAFE_BOUNDARY_OFFSET,
                    coordinatorCoords[1] + coordinator.height / 2f,
                )
            DragTarget.RIGHT ->
                PointF(
                    coordinatorCoords[0] + coordinator.width - GESTURE_SAFE_BOUNDARY_OFFSET,
                    coordinatorCoords[1] + coordinator.height / 2f,
                )
            DragTarget.BOTTOM ->
                PointF(
                    coordinatorCoords[0] + coordinator.width / 2f,
                    coordinatorCoords[1] + coordinator.height - GESTURE_SAFE_BOUNDARY_OFFSET,
                )
        }
}
