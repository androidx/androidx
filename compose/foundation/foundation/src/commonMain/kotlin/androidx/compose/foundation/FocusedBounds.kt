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
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo

internal val ModifierLocalFocusedBoundsObserver =
    modifierLocalOf<((LayoutCoordinates?) -> Unit)?> { null }

/**
 * Calls [onPositioned] whenever the bounds of the currently-focused area changes.
 * If a child of this node has focus, [onPositioned] will be called immediately with a non-null
 * [LayoutCoordinates] that can be queried for the focused bounds, and again every time the focused
 * child changes or is repositioned. When a child loses focus, [onPositioned] will be passed `null`.
 *
 * When an event occurs, it is bubbled up from the focusable node, so the nearest parent gets the
 * event first, and then its parent, etc.
 *
 * Note that there may be some cases where the focused bounds change but the callback is _not_
 * invoked, but the last [LayoutCoordinates] will always return the most up-to-date bounds.
 */
@ExperimentalFoundationApi
fun Modifier.onFocusedBoundsChanged(onPositioned: (LayoutCoordinates?) -> Unit): Modifier =
    this then FocusedBoundsObserverElement(onPositioned)

private class FocusedBoundsObserverElement(
    val onPositioned: (LayoutCoordinates?) -> Unit
) : ModifierNodeElement<FocusedBoundsObserverNode>() {
    override fun create(): FocusedBoundsObserverNode = FocusedBoundsObserverNode(onPositioned)

    override fun update(node: FocusedBoundsObserverNode) {
        node.onPositioned = onPositioned
    }

    override fun hashCode(): Int = onPositioned.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? FocusedBoundsObserverElement ?: return false
        return onPositioned == otherModifier.onPositioned
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "onFocusedBoundsChanged"
        properties["onPositioned"] = onPositioned
    }
}

private class FocusedBoundsObserverNode(
    var onPositioned: (LayoutCoordinates?) -> Unit
) : Modifier.Node(), ModifierLocalModifierNode, (LayoutCoordinates?) -> Unit {
    private val parent: ((LayoutCoordinates?) -> Unit)?
        get() = if (isAttached) ModifierLocalFocusedBoundsObserver.current else null

    override val providedValues: ModifierLocalMap
        get() = modifierLocalMapOf(ModifierLocalFocusedBoundsObserver to this)

    /** Called when a child gains/loses focus or is focused and changes position. */
    override fun invoke(focusedBounds: LayoutCoordinates?) {
        if (!isAttached) return
        onPositioned(focusedBounds)
        parent?.invoke(focusedBounds)
    }
}

/**
 * Modifier used by [Modifier.focusable] to publish the location of the focused element.
 * Should only be applied to the node when it is actually focused. Right now this will keep
 * this node around, but once the un-delegate API lands we can remove this node entirely if it
 * is not focused. (b/276790428)
 */
internal class FocusedBoundsNode : Modifier.Node(), ModifierLocalModifierNode,
    GlobalPositionAwareModifierNode {
    private var isFocused: Boolean = false

    private val observer: ((LayoutCoordinates?) -> Unit)?
        get() = if (isAttached) {
            ModifierLocalFocusedBoundsObserver.current
        } else {
            null
        }

    private var layoutCoordinates: LayoutCoordinates? = null

    /**
     * This should be called from a [androidx.compose.ui.focus.FocusEventModifierNode.onFocusEvent]
     * where it is guarantee that an event will be dispatched during the lifecycle of the node. This
     * means that when the node is detached (and we should warn observers) we'll receive an event.
     */
    fun setFocus(focused: Boolean) {
        if (focused == isFocused) return
        if (!focused) {
            observer?.invoke(null)
        } else {
            notifyObserverWhenAttached()
        }
        isFocused = focused
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        layoutCoordinates = coordinates
        if (!isFocused) return
        if (coordinates.isAttached) {
            notifyObserverWhenAttached()
        } else {
            observer?.invoke(null)
        }
    }

    private fun notifyObserverWhenAttached() {
        if (layoutCoordinates != null && layoutCoordinates!!.isAttached) {
            observer?.invoke(layoutCoordinates)
        }
    }
}