/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.layout.BeyondBoundsLayout.BeyondBoundsScope
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Above
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.After
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Before
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Below
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Left
import androidx.compose.ui.layout.BeyondBoundsLayout.LayoutDirection.Companion.Right
import androidx.compose.ui.node.Nodes
import androidx.compose.ui.node.nearestAncestor

internal fun <T> FocusTargetNode.searchBeyondBounds(
    direction: FocusDirection,
    block: BeyondBoundsScope.() -> T?
): T? {

    // We only want the focus target of the LazyList to perform a beyond bounds search, and want to
    // prevent intermediate focus targets (within a LazyList's item) from triggering a beyond-bounds
    // layout. LazyLists add their own beyondBoundsLayoutParent, so if a focus target has the same
    // beyondBoundsLayoutParent as its parent, that focusTarget is not a lazylist, and the beyond
    // bounds search needs to be ignored.
    nearestAncestor(Nodes.FocusTarget)?.let {
        if (it.beyondBoundsLayoutParent == beyondBoundsLayoutParent) {
            return null
        }
    }

    return beyondBoundsLayoutParent?.layout(
        direction = when (direction) {
            FocusDirection.Up -> Above
            FocusDirection.Down -> Below
            FocusDirection.Left -> Left
            FocusDirection.Right -> Right
            FocusDirection.Next -> After
            FocusDirection.Previous -> Before
            else -> error("Unsupported direction for beyond bounds layout")
        },
        block = block
    )
}
