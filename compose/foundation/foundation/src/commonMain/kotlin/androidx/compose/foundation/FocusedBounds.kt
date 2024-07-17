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

package androidx.compose.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.platform.InspectorInfo

/**
 * Calls [onPositioned] whenever the bounds of the currently-focused area changes. If a child of
 * this node has focus, [onPositioned] will be called immediately with a non-null
 * [LayoutCoordinates] that can be queried for the focused bounds, and again every time the focused
 * child changes or is repositioned. When a child loses focus, [onPositioned] will be passed `null`.
 *
 * When an event occurs, it is bubbled up from the focusable node, so the nearest parent gets the
 * event first, and then its parent, etc.
 *
 * Note that there may be some cases where the focused bounds change but the callback is _not_
 * invoked, but the last [LayoutCoordinates] will always return the most up-to-date bounds.
 */
fun Modifier.onFocusedBoundsChanged(onPositioned: (LayoutCoordinates?) -> Unit): Modifier =
    this then FocusedBoundsObserverElement(onPositioned)

private class FocusedBoundsObserverElement(val onPositioned: (LayoutCoordinates?) -> Unit) :
    ModifierNodeElement<FocusedBoundsObserverNode>() {
    override fun create(): FocusedBoundsObserverNode = FocusedBoundsObserverNode(onPositioned)

    override fun update(node: FocusedBoundsObserverNode) {
        node.onPositioned = onPositioned
    }

    override fun hashCode(): Int = onPositioned.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? FocusedBoundsObserverElement ?: return false
        return onPositioned === otherModifier.onPositioned
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onFocusedBoundsChanged"
        properties["onPositioned"] = onPositioned
    }
}

internal class FocusedBoundsObserverNode(var onPositioned: (LayoutCoordinates?) -> Unit) :
    Modifier.Node(), TraversableNode {

    override val traverseKey: Any = TraverseKey

    /** Called when a child gains/loses focus or is focused and changes position. */
    fun onFocusBoundsChanged(focusedBounds: LayoutCoordinates?) {
        onPositioned(focusedBounds)
        findNearestAncestor()?.onFocusBoundsChanged(focusedBounds)
    }

    companion object TraverseKey
}
