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

package androidx.xr.compose.subspace

import androidx.compose.runtime.Composable
import androidx.xr.compose.subspace.layout.Measurable
import androidx.xr.compose.subspace.layout.MeasurePolicy
import androidx.xr.compose.subspace.layout.MeasureResult
import androidx.xr.compose.subspace.layout.MeasureScope
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.VolumeConstraints

/**
 * A composable that represents an empty space layout. Its size can be controlled using modifiers
 * like [SubspaceModifier.width], [SubspaceModifier.height], etc.
 *
 * @param modifier Modifiers to apply to the spacer.
 */
@Composable
@SubspaceComposable
public fun SpatialLayoutSpacer(modifier: SubspaceModifier = SubspaceModifier) {
    SubspaceLayout(
        name = defaultSpatialLayoutSpacerName(),
        modifier = modifier,
        measurePolicy = SpacerMeasurePolicy,
    )
}

/**
 * A composable that represents an empty space layout. Its size can be controlled using modifiers
 * like [SubspaceModifier.width], [SubspaceModifier.height], etc.
 *
 * @param modifier Modifiers to apply to this spacer.
 * @param name The name of this SpatialLayoutSpacer element.
 */
@Composable
@SubspaceComposable
public fun SpatialLayoutSpacer(
    modifier: SubspaceModifier = SubspaceModifier,
    name: String = defaultSpatialLayoutSpacerName(),
) {
    SubspaceLayout(name = name, modifier = modifier, measurePolicy = SpacerMeasurePolicy)
}

private object SpacerMeasurePolicy : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: VolumeConstraints,
    ): MeasureResult {
        return with(constraints) {
            val width = if (hasBoundedWidth) maxWidth else 0
            val height = if (hasBoundedHeight) maxHeight else 0
            val depth = if (hasBoundedDepth) maxDepth else 0
            layout(width, height, depth) {}
        }
    }
}

private var spatialLayoutSpacerNamePart: Int = 0

private fun defaultSpatialLayoutSpacerName(): String {
    return "Spacer-${spatialLayoutSpacerNamePart++}"
}
