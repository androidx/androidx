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

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density

/** Base Spatial shape. */
public abstract class SpatialShape

/** A shape describing a rectangle with rounded corners in 3D space. */
public class SpatialRoundedCornerShape(private val size: CornerSize) : SpatialShape() {
    /**
     * Computes corner radius to be no larger than 50 percent of the smallest side.
     *
     * @return The corner radius in pixels.
     */
    internal fun computeCornerRadius(maxWidth: Float, maxHeight: Float, density: Density): Float {
        return size
            .toPx(Size(maxWidth, maxHeight), density)
            .coerceAtMost(maxWidth / 2f)
            .coerceAtMost(maxHeight / 2f)
    }
}
