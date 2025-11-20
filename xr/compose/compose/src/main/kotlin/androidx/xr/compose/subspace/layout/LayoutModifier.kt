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

package androidx.xr.compose.subspace.layout

import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.unit.VolumeConstraints

/** Creates a node that allows changing how the wrapped element is measured and laid out. */
public fun SubspaceModifier.layout(
    measure: SubspaceMeasureScope.(SubspaceMeasurable, VolumeConstraints) -> SubspaceMeasureResult
): SubspaceModifier = this then LayoutElement(measure)

private data class LayoutElement(
    val measure:
        SubspaceMeasureScope.(SubspaceMeasurable, VolumeConstraints) -> SubspaceMeasureResult
) : SubspaceModifierNodeElement<SubspaceLayoutModifierImpl>() {
    override fun create() = SubspaceLayoutModifierImpl(measure)

    override fun update(node: SubspaceLayoutModifierImpl) {
        node.measureBlock = measure
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LayoutElement

        return measure === other.measure
    }

    override fun hashCode(): Int {
        return measure.hashCode()
    }
}

private class SubspaceLayoutModifierImpl(
    var measureBlock:
        SubspaceMeasureScope.(SubspaceMeasurable, VolumeConstraints) -> SubspaceMeasureResult
) : SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult = measureBlock(measurable, constraints)
}
