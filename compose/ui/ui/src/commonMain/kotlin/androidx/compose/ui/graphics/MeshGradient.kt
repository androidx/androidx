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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo

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
 * where (0,0) is the top-left and (1,1) is the bottom-right of the modifier's drawing bounds.
 *
 * **Bezier Tangents:** Bezier control points are provided as an [Offset] relative to the vertex
 * position. The default value of a Bezier control point is [Offset.Unspecified]. If a control point
 * is [Offset.Unspecified], the renderer automatically infers a tangent based on the neighboring
 * vertices to ensure C1 continuity (smooth transitions) across patches.
 *
 * @sample androidx.compose.ui.samples.MeshGradientModifierSample
 * @param rows The number of patches along the vertical axis. Must be at least 1.
 * @param columns The number of patches along the horizontal axis. Must be at least 1.
 * @param hasBicubicColor When true, uses Catmull-Rom interpolation for colors, resulting in
 *   smoother transitions compared to bilinear interpolation.
 * @param block Lambda invoked to configure the mesh. Use the provided [MeshGradientScope] to set
 *   the properties of each vertex.
 */
fun Modifier.meshGradient(
    @IntRange(from = 1) rows: Int,
    @IntRange(from = 1) columns: Int,
    hasBicubicColor: Boolean = false,
    block: MeshGradientScope.() -> Unit,
) = this.then(MeshGradientBlockModifierElement(rows, columns, hasBicubicColor, block))

/**
 * A scope for configuring a mesh gradient.
 *
 * Use this scope to set the properties (position, color, and control points) of each vertex in the
 * mesh grid.
 */
class MeshGradientScope(
    /** The number of patches along the vertical axis. */
    val rows: Int,
    /** The number of patches along the horizontal axis. */
    val columns: Int,
) {

    internal val numberOfPoints = (rows + 1) * (columns + 1)
    internal val positions = FloatArray(numberOfPoints * 2)
    internal val colors = IntArray(numberOfPoints)
    internal val leftBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }
    internal val rightBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }
    internal val topBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }
    internal val bottomBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }

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
    ) {
        validateIndices(row, column)

        val index = (row * (columns + 1) + column) * 2
        positions.setOffset(index, position)

        // This also converts the color to sRGB color space.
        colors[row * (columns + 1) + column] = color.toArgb()

        leftBezierOffsets.setOffset(index, leftControlPoint)
        rightBezierOffsets.setOffset(index, rightControlPoint)
        topBezierOffsets.setOffset(index, topControlPoint)
        bottomBezierOffsets.setOffset(index, bottomControlPoint)
    }

    private fun validateIndices(row: Int, column: Int) {
        requirePrecondition(row in 0..rows) { "row ($row) must be in range 0..$rows" }
        requirePrecondition(column in 0..columns) {
            "column ($column) must be in range 0..$columns"
        }
    }
}

private class MeshGradientBlockModifierElement(
    private val rows: Int,
    private val columns: Int,
    private val hasBicubicColor: Boolean,
    private val block: MeshGradientScope.() -> Unit,
) : ModifierNodeElement<MeshGradientBlockModifierNode>() {

    override fun create(): MeshGradientBlockModifierNode {
        return MeshGradientBlockModifierNode(rows, columns, hasBicubicColor, block)
    }

    override fun update(node: MeshGradientBlockModifierNode) {
        node.update(rows, columns, hasBicubicColor, block)
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        result = 31 * result + hasBicubicColor.hashCode()
        result = 31 * result + block.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshGradientBlockModifierElement) return false
        if (other.rows != this.rows) return false
        if (other.columns != this.columns) return false
        if (other.hasBicubicColor != this.hasBicubicColor) return false
        if (other.block !== this.block) return false

        return true
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "MeshGradient"
        properties["rows"] = rows
        properties["columns"] = columns
        properties["hasBicubicColor"] = hasBicubicColor
    }
}

private class MeshGradientBlockModifierNode(
    var rows: Int,
    var columns: Int,
    var hasBicubicColor: Boolean,
    var block: MeshGradientScope.() -> Unit,
) : Modifier.Node(), DrawModifierNode {

    init {
        require(rows > 0 && columns > 0) {
            "Rows and columns must be greater than 0! Rows: $rows, Columns: $columns"
        }
    }

    private var pointScope = MeshGradientScope(rows, columns)
    private val renderer = MeshGradientRenderer()

    fun update(
        rows: Int,
        columns: Int,
        hasBicubicColor: Boolean,
        block: MeshGradientScope.() -> Unit,
    ) {
        val needsNewData = this.rows != rows || this.columns != columns

        this.rows = rows
        this.columns = columns
        this.hasBicubicColor = hasBicubicColor
        this.block = block

        if (needsNewData) {
            pointScope = MeshGradientScope(rows, columns)
        }
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        populateBuffersFromBlock()
        renderer.apply {
            draw(
                rows,
                columns,
                pointScope.positions,
                pointScope.colors,
                pointScope.leftBezierOffsets,
                pointScope.topBezierOffsets,
                pointScope.rightBezierOffsets,
                pointScope.bottomBezierOffsets,
                hasBicubicColor,
            )
        }
        drawContent()
    }

    /** Populates the mesh gradient internal buffers by executing the configuration block. */
    private fun populateBuffersFromBlock() {
        pointScope.positions.fill(0f)
        pointScope.colors.fill(Color.Transparent.toArgb())
        pointScope.leftBezierOffsets.fill(Float.NaN)
        pointScope.rightBezierOffsets.fill(Float.NaN)
        pointScope.topBezierOffsets.fill(Float.NaN)
        pointScope.bottomBezierOffsets.fill(Float.NaN)

        pointScope.block()
    }
}

/**
 * Infers bezier control points for a mesh gradient if they are not explicitly specified. This
 * method calculates default control points based on the positions of neighboring points.
 *
 * The inference is done by calculating the normalized vector between neighboring points and scaling
 * it by 1/3rd of the distance between the current point and the neighboring point. For example, the
 * left bezier control point is inferred by calculating the normalized vector between the right and
 * left neighboring points, scaling it by 1/3rd of the distance between the current point and the
 * left neighboring point, and then negating the result. This is done for all four control points.
 */
internal fun inferBezierControlPointsIfRequired(
    rows: Int,
    columns: Int,
    positions: FloatArray,
    leftBezierOffsets: FloatArray?,
    topBezierOffsets: FloatArray?,
    rightBezierOffsets: FloatArray?,
    bottomBezierOffsets: FloatArray?,
) {
    val columnsPlusOne = columns + 1

    for (r in 0..rows) {
        for (c in 0..columns) {
            val index = (r * columnsPlusOne + c) * 2
            val currentPosition = positions.getOffset(index)

            val topNeighborIndex = if (r > 0) ((r - 1) * columnsPlusOne + c) * 2 else index
            val bottomNeighborIndex = if (r < rows) ((r + 1) * columnsPlusOne + c) * 2 else index
            val leftNeighborIndex = if (c > 0) (r * columnsPlusOne + c - 1) * 2 else index
            val rightNeighborIndex = if (c < columns) (r * columnsPlusOne + c + 1) * 2 else index

            val topNeighbor = positions.getOffset(topNeighborIndex)
            val bottomNeighbor = positions.getOffset(bottomNeighborIndex)
            val leftNeighbor = positions.getOffset(leftNeighborIndex)
            val rightNeighbor = positions.getOffset(rightNeighborIndex)

            val horizontalVector = (rightNeighbor - leftNeighbor).normalized() * 0.33f
            val verticalVector = (bottomNeighbor - topNeighbor).normalized() * 0.33f

            if (leftBezierOffsets != null) {
                val currentLeftBezierOffset = leftBezierOffsets.getOffset(index)
                if (currentLeftBezierOffset.isUnspecified) {
                    val distance = leftNeighbor.distanceFrom(currentPosition)
                    val offset = horizontalVector * distance * -1f
                    leftBezierOffsets.setOffset(index, offset)
                }
            }

            if (topBezierOffsets != null) {
                val currentTopBezierOffset = topBezierOffsets.getOffset(index)
                if (currentTopBezierOffset.isUnspecified) {
                    val distance = topNeighbor.distanceFrom(currentPosition)
                    val offset = verticalVector * distance * -1f
                    topBezierOffsets.setOffset(index, offset)
                }
            }

            if (bottomBezierOffsets != null) {
                val currentBottomBezierOffset = bottomBezierOffsets.getOffset(index)
                if (currentBottomBezierOffset.isUnspecified) {
                    val distance = bottomNeighbor.distanceFrom(currentPosition)
                    val offset = verticalVector * distance
                    bottomBezierOffsets.setOffset(index, offset)
                }
            }

            if (rightBezierOffsets != null) {
                val currentRightBezierOffset = rightBezierOffsets.getOffset(index)
                if (currentRightBezierOffset.isUnspecified) {
                    val distance = rightNeighbor.distanceFrom(currentPosition)
                    val offset = horizontalVector * distance
                    rightBezierOffsets.setOffset(index, offset)
                }
            }
        }
    }
}

private fun FloatArray.getOffset(index: Int): Offset = Offset(this[index], this[index + 1])

private fun FloatArray.setOffset(index: Int, offset: Offset) {
    this[index] = offset.x
    this[index + 1] = offset.y
}

private fun Offset.distanceFrom(other: Offset): Float = (this - other).getDistance()

private fun Offset.normalized(): Offset {
    val distance = getDistance()
    return if (distance == 0f) Offset.Zero else Offset(this.x / distance, this.y / distance)
}
