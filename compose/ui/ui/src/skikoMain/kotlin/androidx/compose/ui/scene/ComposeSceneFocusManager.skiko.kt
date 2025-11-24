/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.scene

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.node.requireLayoutCoordinates

/**
 * The extension of [FocusManager] to manage focus within a [ComposeScene].
 */
@InternalComposeUiApi
class ComposeSceneFocusManager internal constructor(
    private val focusOwner: () -> FocusOwner,
    private val measureAndLayout: () -> Unit,
) {
    /**
     * Returns whether any child has focus.
     */
    val hasFocus: Boolean get() = focusOwner().rootState.hasFocus

    private inline fun <T: Any?> measureAndLayoutThen(request: () -> T): T {
        measureAndLayout()
        return request()
    }

    /**
     * Searches for the currently focused node and returns its coordinates as a rect.
     * @param afterLayout If true, the coordinates are calculated after a layout pass.
     * Otherwise, returns current coordinates.
     */
    fun getFocusRect(afterLayout: Boolean): Rect? = if (afterLayout) {
        measureAndLayoutThen { focusOwner().getFocusRect() }
    } else {
        focusOwner().getFocusRect()
    }
    /**
     * Take focus to [ComposeScene] in specified [focusDirection].
     *
     * Returns `false` if there are no focusable elements in this direction:
     * - The scene is empty
     * - We are at the end of the scene and are asked to move forward
     * - We are at the beginning of the scene and are asked to move backward
     */
    fun takeFocus(focusDirection: FocusDirection): Boolean = measureAndLayoutThen {
        return focusOwner().takeFocus(focusDirection, previouslyFocusedRect = null)
    }

    /**
     * Release focus from [ComposeScene].
     */
    fun releaseFocus() = measureAndLayoutThen { focusOwner().releaseFocus() }

    /**
     * If [position] is outside the bounds of the currently focused node, clear focus.
     */
    fun clearFocusIfOutsideOfActiveFocusTargetNode(position: Offset) = measureAndLayoutThen {
        val focusOwner = focusOwner()
        val activeFocusTargetNode = focusOwner.activeFocusTargetNode
        if (activeFocusTargetNode != null) {
            val focusedNodeBounds =
                activeFocusTargetNode.requireLayoutCoordinates().boundsInRoot()
            // Only clear focus if the motion event doesn't fall inside the bounds
            // of the node that is currently focused
            // Note that we don't use the current focusRect which can be different
            // from the bounds of the currently focused node.
            // If a focusable node is choosing to have the focusRect be different from
            // its own bounds, we shouldn't clear focus from it if the down event
            // occurs over that node.
            if (!focusedNodeBounds.contains(position)) {
                focusOwner.clearFocus()
            }
        }

    }
}
