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

import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3

/**
 * SubspaceMeasurePolicy applied at the root of the compose tree.
 *
 * Based on [androidx.compose.ui.layout.RootMeasurePolicy].
 */
internal class SubspaceRootMeasurePolicy() : SubspaceMeasurePolicy {
    override fun SubspaceMeasureScope.measure(
        measurables: List<SubspaceMeasurable>,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        return when {
            measurables.isEmpty() -> {
                layout(constraints.minWidth, constraints.minHeight, constraints.minDepth) {}
            }
            measurables.size == 1 -> {
                val placeable = measurables[0].measure(constraints)
                layout(placeable.measuredWidth, placeable.measuredHeight, placeable.measuredDepth) {
                    placeable.place(Pose(Vector3.Zero, Quaternion.Identity))
                }
            }
            else -> {
                val placeables = measurables.fastMap { it.measure(constraints) }
                var maxWidth = 0
                var maxHeight = 0
                var maxDepth = 0
                placeables.fastForEach { placeable ->
                    maxWidth = maxOf(placeable.measuredWidth, maxWidth)
                    maxHeight = maxOf(placeable.measuredHeight, maxHeight)
                    maxDepth = maxOf(placeable.measuredDepth, maxDepth)
                }
                layout(maxWidth, maxHeight, maxDepth) {
                    placeables.fastForEach { placeable ->
                        placeable.place(Pose(Vector3.Zero, Quaternion.Identity))
                    }
                }
            }
        }
    }
}
