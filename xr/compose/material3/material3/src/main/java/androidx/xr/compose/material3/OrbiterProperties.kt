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

package androidx.xr.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.subspace.layout.SpatialShape

/**
 * XR-specific properties for components that use aligned [androidx.xr.compose.spatial.Orbiter]s.
 *
 * These properties should be provided via a `CompositionLocal` for the given component.
 *
 * The component should also define a publicly-visible default instance of [OrbiterProperties] and
 * use it if the `CompositionLocal` is not set.
 */
@ExperimentalMaterial3XrApi
@Immutable
public class OrbiterProperties(
    public val position: OrbiterPosition,
    public val shape: SpatialShape,
) {
    /**
     * Returns a new [OrbiterProperties] with one or more properties changed.
     *
     * If `null` is provided for any value, the existing value will be used.
     */
    public fun copy(
        position: OrbiterPosition? = null,
        shape: SpatialShape? = null,
    ): OrbiterProperties =
        OrbiterProperties(position = position ?: this.position, shape = shape ?: this.shape)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OrbiterProperties) return false

        if (position != other.position) return false
        if (shape != other.shape) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + shape.hashCode()
        return result
    }

    override fun toString(): String {
        return "OrbiterProperties(alignment=$position, shape=$shape)"
    }
}

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
internal fun VerticalOrbiter(properties: OrbiterProperties, content: @Composable () -> Unit) {
    Orbiter(position = properties.position, shape = properties.shape, content = content)
}

@OptIn(ExperimentalMaterial3XrApi::class)
@Composable
internal fun HorizontalOrbiter(properties: OrbiterProperties, content: @Composable () -> Unit) {
    Orbiter(position = properties.position, shape = properties.shape, content = content)
}
