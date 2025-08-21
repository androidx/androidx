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

package androidx.compose.ui.focus

import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner

/**
 * Attempts to request focus for the most suitable focusable child node that overlaps with the given
 * rect area ([left], [top], [right], [bottom]).
 *
 * The rectangle is interpreted in the coordinate space **relative to the compose root**. See
 * [androidx.compose.ui.layout.LayoutCoordinates.localToRoot] for converting local coordinates to
 * the root coordinates.
 *
 * @param left is the left edge of the rectangle, in pixels relative to the compose root.
 * @param top is the top edge of the rectangle, in pixels relative to the compose root.
 * @param right is the right edge of the rectangle, in pixels relative to the compose root.
 * @param bottom is the bottom edge of the rectangle, in pixels relative to the compose root.
 * @return `true` if a matching child was found and focus was granted; `false` if no such child
 *   exists, it is already focused, or the focus request failed.
 */
fun DelegatableNode.requestFocusForChildInRootBounds(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
): Boolean {
    val containerId = requireLayoutNode().semanticsId
    val childNode =
        requireOwner()
            .rectManager
            .findFocusableNodeFromRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                containerId = containerId,
            )
    return childNode?.requestFocus() ?: false
}
