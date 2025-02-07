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
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.unit.IntVolumeSize
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.ContentlessEntity
import androidx.xr.scenecore.Entity

/**
 * A composable that represents a 3D volume of space within which an application can fill content.
 *
 * This composable provides a [Entity] through the [onVolumeEntity] lambda, allowing the caller to
 * attach child Jetpack XR Entities to it.
 *
 * @param modifier SubspaceModifiers to apply to the Volume.
 * @param name A name associated with this Volume entity, useful for debugging.
 * @param onVolumeEntity A lambda function that will be invoked when the [Entity] becomes available.
 */
@Composable
@SubspaceComposable
public fun Volume(
    modifier: SubspaceModifier = SubspaceModifier,
    name: String = defaultVolumeName(),
    onVolumeEntity: (Entity) -> Unit,
) {
    val defaultWidthPx = 400
    val defaultHeightPx = 400
    val defaultDepthPx = 400

    SubspaceLayout(
        modifier = modifier,
        name = name,
        coreEntity =
            rememberCoreContentlessEntity {
                ContentlessEntity.create(this, name = name, pose = Pose.Identity)
                    .apply(onVolumeEntity)
            },
    ) { measurables, constraints ->
        val initialWidth = defaultWidthPx.coerceIn(constraints.minWidth, constraints.maxWidth)
        val initialHeight = defaultHeightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
        val initialDepth = defaultDepthPx.coerceIn(constraints.minDepth, constraints.maxDepth)

        val placeables = measurables.map { it.measure(constraints) }

        val maxSize =
            placeables.fold(IntVolumeSize(initialWidth, initialHeight, initialDepth)) {
                currentMax,
                placeable ->
                IntVolumeSize(
                    width = maxOf(currentMax.width, placeable.measuredWidth),
                    height = maxOf(currentMax.height, placeable.measuredHeight),
                    depth = maxOf(currentMax.depth, placeable.measuredDepth),
                )
            }

        // Reserve space in the original composition
        layout(maxSize.width, maxSize.height, maxSize.depth) {
            placeables.forEach { it.place(Pose()) }
        }
    }
}

private var volumeNamePart: Int = 0

private fun defaultVolumeName(): String {
    return "Volume-${volumeNamePart++}"
}
