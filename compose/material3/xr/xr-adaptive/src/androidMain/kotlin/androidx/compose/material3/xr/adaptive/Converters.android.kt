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
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.OrbiterOffsetType
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SpatialShape

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun ContentEdgeStub.Vertical.toXrPositionVertical(): ContentEdge.Vertical =
    when (this) {
        ContentEdgeStub.Vertical.Start -> ContentEdge.Vertical.Start
        ContentEdgeStub.Vertical.End -> ContentEdge.Vertical.End
        else -> error("Unsupported ContentEdge.Vertical: $this")
    }

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun ContentEdgeStub.Horizontal.toXrPositionHorizontal(): ContentEdge.Horizontal =
    when (this) {
        ContentEdgeStub.Horizontal.Top -> ContentEdge.Horizontal.Top
        ContentEdgeStub.Horizontal.Bottom -> ContentEdge.Horizontal.Bottom
        else -> error("Unsupported ContentEdge.Horizontal: $this")
    }

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun OrbiterOffsetTypeStub.toXrOrbiterOffsetType(): OrbiterOffsetType =
    when (this) {
        OrbiterOffsetTypeStub.Overlap -> OrbiterOffsetType.Overlap
        OrbiterOffsetTypeStub.InnerEdge -> OrbiterOffsetType.InnerEdge
        OrbiterOffsetTypeStub.OuterEdge -> OrbiterOffsetType.OuterEdge
        else -> error("Unsupported OrbiterOffsetType: $this")
    }

@OptIn(ExperimentalMaterial3XrAdaptiveApi::class)
internal fun SpatialShapeStub.toXrSpatialShape(): SpatialShape =
    when (this) {
        is SpatialRoundedCornerShapeStub -> SpatialRoundedCornerShape(size)
        else -> error("Unsupported SpatialShape type: ${this::class}. Value: $this")
    }
