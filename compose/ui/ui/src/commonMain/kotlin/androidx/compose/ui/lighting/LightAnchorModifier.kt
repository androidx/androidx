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

package androidx.compose.ui.lighting

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.UnplacedAwareModifierNode
import androidx.compose.ui.platform.InspectorInfo

/**
 * Reports this composable's position to [state], so that lights positioned with
 * [LightPosition.Anchored] against the same state follow this composable.
 *
 * Create [state] with `remember` and pass the same instance to this modifier and to every
 * [LightPosition.Anchored] that should track it.
 *
 * A given [LightAnchorState] tracks one composable at a time. If this modifier is applied to more
 * than one composable simultaneously, the state reports whichever was placed most recently, which
 * is rarely what was intended. When the anchored composable leaves the composition the state is
 * cleared and the lights using it stop contributing.
 *
 * @param state the anchor to publish this composable's position to.
 * @sample androidx.compose.ui.samples.LightAnchorSample
 * @see illuminate
 * @see LightPosition.Anchored
 */
public fun Modifier.lightAnchor(state: LightAnchorState): Modifier =
    this then LightAnchorModifierElement(state)

private class LightAnchorModifierElement(val state: LightAnchorState) :
    ModifierNodeElement<LightAnchorModifierNode>() {
    override fun create(): LightAnchorModifierNode = LightAnchorModifierNode(state)

    override fun update(node: LightAnchorModifierNode) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "lightAnchor"
        properties["state"] = state
    }

    override fun hashCode(): Int = state.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LightAnchorModifierElement) return false
        if (other.state !== this.state) return false
        return true
    }
}

private class LightAnchorModifierNode(state: LightAnchorState) :
    Modifier.Node(), GlobalPositionAwareModifierNode, UnplacedAwareModifierNode {

    var state: LightAnchorState = state
        set(value) {
            if (field !== value) {
                val placedCoordinates = coordinates
                release()
                field = value
                if (placedCoordinates != null) publish(placedCoordinates)
            }
        }

    /** Where this composable was last placed, or `null` while it is not placed. */
    private var coordinates: LayoutCoordinates? = null

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        publish(coordinates)
    }

    override fun onUnplaced() {
        release()
    }

    override fun onDetach() {
        release()
    }

    private fun publish(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
        state.coordinates = coordinates
    }

    /** Clears [state], unless another composable anchored to it has been placed since. */
    private fun release() {
        val published = coordinates ?: return
        if (state.coordinates === published) state.coordinates = null
        coordinates = null
    }
}
