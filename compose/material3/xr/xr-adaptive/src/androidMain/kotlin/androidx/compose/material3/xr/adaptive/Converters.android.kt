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

package androidx.compose.material3.xr.adaptive

import androidx.compose.material3.xr.spatial.ContentEdge as ContentEdgeStub
import androidx.compose.material3.xr.spatial.OrbiterOffsetType as OrbiterOffsetTypeStub
import androidx.compose.material3.xr.subspace.layout.SpatialRoundedCornerShape as SpatialRoundedCornerShapeStub
import androidx.compose.material3.xr.subspace.layout.SpatialShape as SpatialShapeStub
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.OrbiterPosition.EdgeAlignment
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun OrbiterOffsetTypeStub.toXrOrbiterEdgeAlignment(): EdgeAlignment =
    when (this) {
        OrbiterOffsetTypeStub.Overlap -> EdgeAlignment.Center
        OrbiterOffsetTypeStub.InnerEdge -> EdgeAlignment.Inside
        OrbiterOffsetTypeStub.OuterEdge -> EdgeAlignment.Outside
        else -> error("Unsupported OrbiterOffsetType: $this")
    }

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun ContentEdgeStub.Vertical.toXrOrbiterAlignment(
    offset: Dp,
    offsetType: OrbiterOffsetTypeStub,
    alignment: Alignment.Vertical,
    elevation: Dp,
): OrbiterPosition {
    val edgeOffsetType = offsetType.toXrOrbiterEdgeAlignment()
    val volumeOffset = androidx.xr.compose.unit.DpVolumeOffset(x = offset, y = 0.dp, z = elevation)
    return when (this) {
        ContentEdgeStub.Vertical.Start ->
            when (alignment) {
                Alignment.Top ->
                    OrbiterPosition.TopStart(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                Alignment.CenterVertically ->
                    OrbiterPosition.CenterStart(edgeOffsetType, volumeOffset)
                Alignment.Bottom ->
                    OrbiterPosition.BottomStart(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                else -> throw IllegalArgumentException("Invalid alignment: $alignment")
            }
        ContentEdgeStub.Vertical.End ->
            when (alignment) {
                Alignment.Top ->
                    OrbiterPosition.TopEnd(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                Alignment.CenterVertically ->
                    OrbiterPosition.CenterEnd(edgeOffsetType, volumeOffset)
                Alignment.Bottom ->
                    OrbiterPosition.BottomEnd(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                else -> throw IllegalArgumentException("Invalid alignment: $alignment")
            }
        else -> error("Unsupported ContentEdge.Vertical: $this")
    }
}

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun ContentEdgeStub.Horizontal.toXrOrbiterAlignment(
    offset: Dp,
    offsetType: OrbiterOffsetTypeStub,
    alignment: Alignment.Horizontal,
    elevation: Dp,
): OrbiterPosition {
    val edgeOffsetType = offsetType.toXrOrbiterEdgeAlignment()
    val volumeOffset = androidx.xr.compose.unit.DpVolumeOffset(x = 0.dp, y = offset, z = elevation)
    return when (this) {
        ContentEdgeStub.Horizontal.Top ->
            when (alignment) {
                Alignment.Start ->
                    OrbiterPosition.TopStart(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                Alignment.CenterHorizontally ->
                    OrbiterPosition.TopCenter(edgeOffsetType, volumeOffset)
                Alignment.End ->
                    OrbiterPosition.TopEnd(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                AbsoluteAlignment.Left ->
                    OrbiterPosition.TopLeft(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                AbsoluteAlignment.Right ->
                    OrbiterPosition.TopRight(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                else -> throw IllegalArgumentException("Invalid alignment: $alignment")
            }
        ContentEdgeStub.Horizontal.Bottom ->
            when (alignment) {
                Alignment.Start ->
                    OrbiterPosition.BottomStart(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                Alignment.CenterHorizontally ->
                    OrbiterPosition.BottomCenter(edgeOffsetType, volumeOffset)
                Alignment.End ->
                    OrbiterPosition.BottomEnd(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                AbsoluteAlignment.Left ->
                    OrbiterPosition.BottomLeft(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                AbsoluteAlignment.Right ->
                    OrbiterPosition.BottomRight(
                        horizontalEdgeAlignment = edgeOffsetType,
                        verticalEdgeAlignment = edgeOffsetType,
                        offset = volumeOffset,
                    )
                else -> throw IllegalArgumentException("Invalid alignment: $alignment")
            }
        else -> error("Unsupported ContentEdge.Horizontal: $this")
    }
}

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun SpatialShapeStub.toXrSpatialShape(): SpatialShape =
    when (this) {
        is SpatialRoundedCornerShapeStub -> SpatialRoundedCornerShape(size)
        else -> error("Unsupported SpatialShape type: ${this::class}. Value: $this")
    }
