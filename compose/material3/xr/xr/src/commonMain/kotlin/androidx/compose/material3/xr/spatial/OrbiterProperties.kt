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

package androidx.compose.material3.xr.spatial

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.xr.subspace.layout.SpatialRoundedCornerShape
import androidx.compose.material3.xr.subspace.layout.SpatialShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@RestrictTo(LIBRARY_GROUP)
@Immutable
public class HorizontalOrbiterProperties(
    public val position: ContentEdge.Horizontal,
    public val offset: Dp = 0.dp,
    public val offsetType: OrbiterOffsetType = OrbiterOffsetType.OuterEdge,
    public val alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    public val shape: SpatialShape = SpatialRoundedCornerShape(ZeroCornerSize),
    public val elevation: Dp = 16.dp,
) {
    public fun copy(
        position: ContentEdge.Horizontal = this.position,
        offset: Dp = this.offset,
        offsetType: OrbiterOffsetType = this.offsetType,
        alignment: Alignment.Horizontal = this.alignment,
        shape: SpatialShape = this.shape,
        elevation: Dp = this.elevation,
    ): HorizontalOrbiterProperties =
        HorizontalOrbiterProperties(
            position = position,
            offset = offset,
            offsetType = offsetType,
            alignment = alignment,
            shape = shape,
            elevation = elevation,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HorizontalOrbiterProperties

        if (position != other.position) return false
        if (offset != other.offset) return false
        if (offsetType != other.offsetType) return false
        if (alignment != other.alignment) return false
        if (shape != other.shape) return false
        if (elevation != other.elevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + offsetType.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + elevation.hashCode()
        return result
    }
}

@RestrictTo(LIBRARY_GROUP)
public class VerticalOrbiterProperties(
    public val position: ContentEdge.Vertical,
    public val offset: Dp = 0.dp,
    public val offsetType: OrbiterOffsetType = OrbiterOffsetType.OuterEdge,
    public val alignment: Alignment.Vertical = Alignment.CenterVertically,
    public val shape: SpatialShape = SpatialRoundedCornerShape(ZeroCornerSize),
    public val elevation: Dp = 16.dp,
) {
    public fun copy(
        position: ContentEdge.Vertical = this.position,
        offset: Dp = this.offset,
        offsetType: OrbiterOffsetType = this.offsetType,
        alignment: Alignment.Vertical = this.alignment,
        shape: SpatialShape = this.shape,
        elevation: Dp = this.elevation,
    ): VerticalOrbiterProperties =
        VerticalOrbiterProperties(
            position = position,
            offset = offset,
            offsetType = offsetType,
            alignment = alignment,
            shape = shape,
            elevation = elevation,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VerticalOrbiterProperties

        if (position != other.position) return false
        if (offset != other.offset) return false
        if (offsetType != other.offsetType) return false
        if (alignment != other.alignment) return false
        if (shape != other.shape) return false
        if (elevation != other.elevation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + offset.hashCode()
        result = 31 * result + offsetType.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + elevation.hashCode()
        return result
    }
}
