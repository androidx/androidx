/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * A renderer responsible for tessellating and drawing a 2D mesh gradient.
 *
 * A mesh gradient is defined by a grid of vertices, where each vertex has a position, color, and
 * four optional Bezier control points (tangents) that define the curvature of the edges connecting
 * neighboring vertices. Colors can be interpolated using either bilinear or bicubic interpolation.
 *
 * The renderer can be stateful (e.g. on Android) and maintain internal buffers (such as index and
 * vertex buffers) to optimize rendering performance and avoid per-frame allocations.
 *
 * @see Modifier.meshGradient
 */
interface MeshGradientRenderer {
    /**
     * Renders the mesh gradient onto the given [DrawScope].
     *
     * @param rows The number of rows in the mesh grid. Must be greater than 0.
     * @param columns The number of columns in the mesh grid. Must be greater than 0.
     * @param positions A flattened [FloatArray] containing the (x, y) coordinates for each vertex
     *   in the mesh. The array must contain at least `(rows + 1) * (columns + 1) * 2` elements. The
     *   coordinates should be normalized (0.0 to 1.0) relative to the [DrawScope.size].
     * @param colors A flattened [IntArray] containing the ARGB colors in sRGB color space for each
     *   vertex. The array must contain at least `(rows + 1) * (columns + 1)` elements.
     * @param leftBezierOffsets Optional flattened [FloatArray] of (x, y) offsets for the left
     *   Bezier control points relative to the vertex position. If null, or if values are
     *   Offset.Unspecified, the renderer will infer smooth control points based on neighbors.
     * @param topBezierOffsets Optional [FloatArray] for top Bezier control point offsets.
     * @param rightBezierOffsets Optional [FloatArray] for right Bezier control point offsets.
     * @param bottomBezierOffsets Optional [FloatArray] for bottom Bezier control point offsets.
     * @param hasBicubicColor Whether to use bicubic interpolation for colors (Catmull-Rom) or
     *   bilinear interpolation. Bicubic provides smoother transitions but is more computationally
     *   expensive.
     *
     * Note: If any Bezier offset is Unspecified, the renderer may modify the provided bezier offset
     * arrays to store the inferred values instead of Offset.Unspecified, avoiding redundant
     * calculations in subsequent calls.
     */
    fun DrawScope.draw(
        rows: Int,
        columns: Int,
        positions: FloatArray,
        colors: IntArray,
        leftBezierOffsets: FloatArray? = null,
        topBezierOffsets: FloatArray? = null,
        rightBezierOffsets: FloatArray? = null,
        bottomBezierOffsets: FloatArray? = null,
        hasBicubicColor: Boolean = false,
    )
}

/**
 * Creates a [MeshGradientRenderer].
 *
 * @sample androidx.compose.ui.samples.MeshGradientRendererSample
 */
expect fun MeshGradientRenderer(): MeshGradientRenderer
