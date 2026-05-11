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

import androidx.annotation.IntRange
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.internal.requirePrecondition

/**
 * A scope for configuring a mesh gradient.
 *
 * Use this scope to set the properties (position, color, and control points) of each vertex in the
 * mesh grid.
 */
sealed interface MeshGradientScope {

    /** The number of patches along the vertical axis. */
    val rows: Int

    /** The number of patches along the horizontal axis. */
    val columns: Int

    /**
     * Sets the properties for a vertex at the specified [row] and [column].
     *
     * @param row The row index of the vertex (0 to [rows]).
     * @param column The column index of the vertex (0 to [columns]).
     * @param position The normalized position of the vertex (0,0 to 1,1).
     * @param color The color associated with the vertex.
     * @param leftControlPoint The horizontal Bezier control point offset relative to [position] for
     *   the edge to the left.
     * @param topControlPoint The vertical Bezier control point offset relative to [position] for
     *   the edge above.
     * @param rightControlPoint The horizontal Bezier control point offset relative to [position]
     *   for the edge to the right.
     * @param bottomControlPoint The vertical Bezier control point offset relative to [position] for
     *   the edge below.
     */
    fun setVertex(
        row: Int,
        column: Int,
        position: Offset,
        color: Color,
        leftControlPoint: Offset = Offset.Unspecified,
        topControlPoint: Offset = Offset.Unspecified,
        rightControlPoint: Offset = Offset.Unspecified,
        bottomControlPoint: Offset = Offset.Unspecified,
    )
}

/**
 * A MeshGradient is a 2D grid of patches defined by vertices. Each vertex has a position, color,
 * and four optional Bezier control points (tangents) that define the curvature of the edges
 * connecting neighboring vertices.
 *
 * **Grid Dimensions:** For a given [rows] and [columns] (representing the number of patches), there
 * are a total of `(rows + 1) * (columns + 1)` vertices. For example, a 1x1 mesh consists of 4
 * vertices forming a single rectangular patch.
 *
 * **Coordinate System:** All positions and Bezier offsets use a **normalized coordinate system**
 * where (0,0) is the top-left and (1,1) is the bottom-right of the drawing bounds.
 *
 * **Bezier Tangents:** Bezier control points are provided as an [Offset] relative to the vertex
 * position. The default value of a Bezier control point is [Offset.Unspecified]. If a control point
 * is [Offset.Unspecified], the renderer automatically infers a tangent based on the neighboring
 * vertices to ensure C1 continuity (smooth transitions) across patches.
 *
 * @sample androidx.compose.ui.samples.MeshGradientPainterSample
 * @param rows The number of patches along the vertical axis. Must be at least 1.
 * @param columns The number of patches along the horizontal axis. Must be at least 1.
 * @param hasBicubicColor When true, uses Catmull-Rom interpolation for colors, resulting in
 *   smoother transitions compared to simpler and slightly faster bilinear interpolation.
 * @param block Lambda invoked to configure the mesh. Use the provided [MeshGradientScope] to set
 *   the properties of each vertex. This block is executed in a [DrawScope] and hence can observe
 *   reads to any mutable state. Any unconfigured vertex will have a default position of
 *   [Offset.Zero] and a default color of [Color.Transparent].
 */
class MeshGradientPainter
@RememberInComposition
constructor(
    @param:IntRange(from = 1) private val rows: Int,
    @param:IntRange(from = 1) private val columns: Int,
    private val hasBicubicColor: Boolean = false,
    private val block: MeshGradientScope.() -> Unit,
) : Painter() {
    init {
        requirePrecondition(rows > 0 && columns > 0) {
            "Rows and Columns must be greater than 0. rows: $rows, columns: $columns"
        }
    }

    private val gradientConfig = MeshGradientConfig(rows, columns, hasBicubicColor)
    private val gradientRenderer = MeshGradientRenderer()

    override val intrinsicSize: Size
        get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        gradientConfig.configure(block)
        gradientRenderer.apply { draw(gradientConfig) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshGradientPainter) return false

        if (rows != other.rows) return false
        if (columns != other.columns) return false
        if (hasBicubicColor != other.hasBicubicColor) return false
        if (block !== other.block) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        result = 31 * result + hasBicubicColor.hashCode()
        result = 31 * result + block.hashCode()
        return result
    }

    override fun toString(): String =
        "MeshGradientPainter(rows: $rows, columns: $columns, hasBicubicColor: $hasBicubicColor)"
}
