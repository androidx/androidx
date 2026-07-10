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

package androidx.pdf.ink.view.draganddrop

import android.view.View
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START
import kotlin.math.hypot

/**
 * Manages a set of views that act as anchor points for a drag-and-drop operation.
 *
 * This class is responsible for:
 * - Toggling the visibility of the anchor views.
 * - Calculating which anchor is closest to a given point on the screen.
 * - Providing visual feedback by highlighting the closest (active) anchor.
 *
 * @param left The [View] representing the anchor on the start/left side.
 * @param right The [View] representing the anchor on the end/right side.
 * @param bottom The [View] representing the anchor on the bottom.
 */
internal class AnchorManager(
    private val left: View,
    private val right: View,
    private val bottom: View,
) {

    private val anchors =
        mapOf(DOCK_STATE_START to left, DOCK_STATE_END to right, DOCK_STATE_BOTTOM to bottom)

    fun showAnchors() {
        anchors.values.forEach { view ->
            // Reset to inactive state initially
            view.alpha = ALPHA_INACTIVE
            view.visibility = View.VISIBLE
        }
    }

    fun hideAnchors() {
        anchors.values.forEach { it.visibility = View.GONE }
    }

    /** Calculates nearest anchor, updates UI highlights, and returns the closest State. */
    fun updateHighlightingAndGetClosest(
        currentX: Float,
        currentY: Float,
        viewWidth: Int,
        viewHeight: Int,
    ): Int {
        val centerX = currentX + viewWidth / 2
        val centerY = currentY + viewHeight / 2

        val closestEntry = anchors.minByOrNull { (_, view) -> getDistance(centerX, centerY, view) }

        anchors.values.forEach { it.alpha = ALPHA_INACTIVE }

        // Highlight the closest one and return its state.
        closestEntry?.let { (state, view) ->
            view.alpha = ALPHA_ACTIVE
            return state
        }

        return DOCK_STATE_BOTTOM
    }

    fun getAnchorView(@ToolbarDockState.DockState state: Int): View? = anchors[state]

    private fun getDistance(x1: Float, y1: Float, target: View): Float {
        val x2 = target.x + target.width / 2
        val y2 = target.y + target.height / 2
        return hypot(x1 - x2, y1 - y2)
    }

    companion object {
        // Opacity values for visual feedback
        private const val ALPHA_ACTIVE = 0.8f
        private const val ALPHA_INACTIVE = 0.3f
    }
}
