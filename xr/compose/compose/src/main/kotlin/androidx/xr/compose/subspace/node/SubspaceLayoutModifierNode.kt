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

package androidx.xr.compose.subspace.node

import androidx.xr.compose.subspace.layout.Measurable
import androidx.xr.compose.subspace.layout.MeasureResult
import androidx.xr.compose.subspace.layout.MeasureScope
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.unit.VolumeConstraints

/**
 * A specialized [SubspaceModifier.Node] responsible for modifying the measurement and layout
 * behavior of its wrapped content within the Subspace environment.
 *
 * Based on [androidx.compose.ui.node.LayoutModifierNode].
 */
public interface SubspaceLayoutModifierNode {

    /**
     * Defines the measurement and layout of the [Measurable] within the given [MeasureScope].
     *
     * The measurable is subject to the specified [VolumeConstraints].
     *
     * @param measurable the content to be measured.
     * @param constraints the constraints within which the measurement should occur.
     * @return a [MeasureResult] encapsulating the size and alignment lines of the measured layout.
     */
    public fun MeasureScope.measure(
        measurable: Measurable,
        constraints: VolumeConstraints,
    ): MeasureResult
}

/**
 * Requests a relayout of the [SubspaceLayoutModifierNode] composition tree.
 *
 * This is used to request a relayout in stateful layout modifiers that are impacted by events that
 * don't trigger a recomposition. *Do not* call this from [measure].
 */
public fun SubspaceLayoutModifierNode.requestRelayout() {
    coordinator.layoutNode?.requestRelayout()
}

/**
 * Returns the [SubspaceLayoutModifierNodeCoordinator] associated with this
 * [SubspaceLayoutModifierNode].
 *
 * This is used to traverse the modifier node tree to find the correct [SubspaceLayoutCoordinates]
 * for a given [SubspaceLayoutModifierNode].
 */
internal val SubspaceLayoutModifierNode.coordinator: SubspaceLayoutModifierNodeCoordinator
    get() {
        check(this is SubspaceModifier.Node && this.coordinator != null) {
            "SubspaceLayoutModifierNode must be a SubspaceModifier.Node and have a non-null coordinator."
        }
        return (this as SubspaceModifier.Node).coordinator!!
    }
