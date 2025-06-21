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

package androidx.compose.ui.layout

import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.spatial.RelativeLayoutBounds

/**
 * An object which holds on to a (potentially) mutating [RelativeLayoutBounds] of a specific node.
 * This is meant to be used alongside the [layoutBounds] modifier and one of the visibility
 * modifiers [onFirstVisible] or [onVisibilityChanged] in situations where you want to understand
 * the visibility of a node with respect to a specific viewport instead of with the window.
 *
 * @see layoutBounds
 * @see onFirstVisible
 * @see onVisibilityChanged
 */
class LayoutBoundsHolder {
    /**
     * The bounds of the node this holder is referencing. This is backed by
     * [androidx.compose.runtime.MutableState] and might change frequently, so reading it during
     * composition directly is discouraged.
     */
    @get:FrequentlyChangingValue
    var bounds: RelativeLayoutBounds? by mutableStateOf(null)
        internal set
}

internal data class LayoutBoundsElement(val holder: LayoutBoundsHolder) :
    ModifierNodeElement<LayoutBoundsNode>() {
    override fun create() = LayoutBoundsNode(holder)

    override fun update(node: LayoutBoundsNode) {
        node.holder = holder
        node.forceUpdate()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "layoutBounds"
        properties["holder"] = holder
    }
}

internal class LayoutBoundsNode(var holder: LayoutBoundsHolder) : Modifier.Node() {
    var handle: DelegatableNode.RegistrationHandle? = null
    var lastBounds: RelativeLayoutBounds? = null
    val rectChanged = { bounds: RelativeLayoutBounds ->
        holder.bounds = bounds
        lastBounds = bounds
    }

    fun forceUpdate() {
        holder.bounds = lastBounds
    }

    override fun onAttach() {
        handle?.unregister()
        handle = registerOnLayoutRectChanged(0, 0, rectChanged)
    }

    override fun onDetach() {
        handle?.unregister()
        holder.bounds = null
    }
}

/**
 * This will map the [RelativeLayoutBounds] of the modifier into the provided [LayoutBoundsHolder].
 * A given instance of [LayoutBoundsHolder] should not be passed into more than one of these
 * modifiers.
 *
 * @see LayoutBoundsHolder
 * @see onVisibilityChanged
 * @see onFirstVisible
 */
fun Modifier.layoutBounds(holder: LayoutBoundsHolder): Modifier =
    this then LayoutBoundsElement(holder)
