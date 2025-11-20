/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.LayoutCoordinatesAwareModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement

/**
 * Invoke [onGloballyPositioned] with the [SubspaceLayoutCoordinates] of the element when the global
 * position or the orientation of the content may have changed. Note that it will be called
 * **after** a composition when the coordinates are finalized.
 *
 * This callback executes after composition once the final coordinates are determined. It will be
 * invoked at least once when the [SubspaceLayoutCoordinates] become available and subsequently
 * whenever the composable's transform (position, rotation, scale) is updated relative to the
 * subspace's origin.
 *
 * The callback information will be relative to its subspace. For instance, When a nested subspace
 * is moved by the global subspace its Pose will reflect its position in the nested subspace. Its
 * position value will not be updated despite moving locations in the Global Subspace.
 */
public fun SubspaceModifier.onGloballyPositioned(
    onGloballyPositioned: (SubspaceLayoutCoordinates) -> Unit
): SubspaceModifier = this then OnGloballyPositionedVolumeElement(onGloballyPositioned)

private class OnGloballyPositionedVolumeElement(
    public val onGloballyPositioned: (SubspaceLayoutCoordinates) -> Unit
) : SubspaceModifierNodeElement<OnGloballyPositionedNode>() {
    override fun create(): OnGloballyPositionedNode {
        return OnGloballyPositionedNode(onGloballyPositioned)
    }

    override fun update(node: OnGloballyPositionedNode) {
        node.callback = onGloballyPositioned
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OnGloballyPositionedVolumeElement) return false
        return onGloballyPositioned === other.onGloballyPositioned
    }

    override fun hashCode(): Int {
        return onGloballyPositioned.hashCode()
    }
}

/** Node associated with [onGloballyPositioned]. */
private class OnGloballyPositionedNode(public var callback: (SubspaceLayoutCoordinates) -> Unit) :
    SubspaceModifier.Node(), LayoutCoordinatesAwareModifierNode {
    override fun onLayoutCoordinates(coordinates: SubspaceLayoutCoordinates) {
        callback(coordinates)
    }
}
