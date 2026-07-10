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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.internal.requirePrecondition
import kotlin.jvm.JvmName

/**
 * Configuration for a mesh gradient. Use [configure] to set up this MeshGradient using a
 * [MeshGradientScope] lambda.
 *
 * @param rows The number of patches along the vertical axis.
 * @param columns The number of patches along the horizontal axis.
 * @param hasBicubicColor When true, uses Catmull-Rom interpolation for colors, resulting in
 *   smoother transitions compared to bilinear interpolation.
 * @see MeshGradientScope
 * @see MeshGradientRenderer
 */
internal class MeshGradientConfig(
    val rows: Int,
    val columns: Int,
    @get:JvmName("hasBicubicColor") var hasBicubicColor: Boolean = false,
) {

    private val gradientScopeImpl = MeshGradientScopeImpl(rows, columns)

    val positions: FloatArray
        get() = gradientScopeImpl.positions

    val colors: IntArray
        get() = gradientScopeImpl.colors

    val leftBezierOffsets: FloatArray
        get() = gradientScopeImpl.leftBezierOffsets

    val rightBezierOffsets: FloatArray
        get() = gradientScopeImpl.rightBezierOffsets

    val topBezierOffsets: FloatArray
        get() = gradientScopeImpl.topBezierOffsets

    val bottomBezierOffsets: FloatArray
        get() = gradientScopeImpl.bottomBezierOffsets

    init {
        requirePrecondition(rows > 0 && columns > 0) {
            "Rows and Columns must be greater than 0. rows: $rows, columns: $columns"
        }
    }

    /**
     * Uses the provided [MeshGradientScope] to configure the mesh gradient. Any bezier offset that
     * is [Offset.Unspecified] will be inferred based on the point's neighbors.
     */
    fun configure(block: MeshGradientScope.() -> Unit) {
        resetScope()
        gradientScopeImpl.block()
        inferBezierControlPointsIfRequired()
    }

    private fun resetScope() {
        gradientScopeImpl.positions.fill(0f)
        gradientScopeImpl.colors.fill(Color.Transparent.toArgb())
        gradientScopeImpl.leftBezierOffsets.fill(Float.NaN)
        gradientScopeImpl.rightBezierOffsets.fill(Float.NaN)
        gradientScopeImpl.topBezierOffsets.fill(Float.NaN)
        gradientScopeImpl.bottomBezierOffsets.fill(Float.NaN)
    }

    /**
     * Infers bezier control points for a mesh gradient if they are not explicitly specified. This
     * method calculates default control points based on the positions of neighboring points.
     *
     * The inference is done by calculating the normalized vector between neighboring points and
     * scaling it by 1/3rd of the distance between the current point and the neighboring point. For
     * example, the left bezier control point is inferred by calculating the normalized vector
     * between the right and left neighboring points, scaling it by 1/3rd of the distance between
     * the current point and the left neighboring point, and then negating the result. This is done
     * for all four control points.
     */
    private fun inferBezierControlPointsIfRequired() {
        val columnsPlusOne = columns + 1

        for (row in 0..rows) {
            for (column in 0..columns) {
                val index = (row * columnsPlusOne + column) * 2
                val currentPosition = gradientScopeImpl.positions.getOffset(index)

                val leftNeighborIndex =
                    if (column > 0) (row * columnsPlusOne + column - 1) * 2 else index
                val rightNeighborIndex =
                    if (column < columns) (row * columnsPlusOne + column + 1) * 2 else index
                val leftNeighbor = gradientScopeImpl.positions.getOffset(leftNeighborIndex)
                val rightNeighbor = gradientScopeImpl.positions.getOffset(rightNeighborIndex)
                val horizontalVector = (rightNeighbor - leftNeighbor).normalized() * 0.33f

                val currentLeftBezierOffset = gradientScopeImpl.leftBezierOffsets.getOffset(index)
                if (currentLeftBezierOffset.isUnspecified) {
                    val distance = leftNeighbor.distanceFrom(currentPosition)
                    val offset = horizontalVector * distance * -1f
                    gradientScopeImpl.leftBezierOffsets.setOffset(index, offset)
                }

                val currentRightBezierOffset = gradientScopeImpl.rightBezierOffsets.getOffset(index)
                if (currentRightBezierOffset.isUnspecified) {
                    val distance = rightNeighbor.distanceFrom(currentPosition)
                    val offset = horizontalVector * distance
                    gradientScopeImpl.rightBezierOffsets.setOffset(index, offset)
                }

                val topNeighborIndex =
                    if (row > 0) ((row - 1) * columnsPlusOne + column) * 2 else index
                val bottomNeighborIndex =
                    if (row < rows) ((row + 1) * columnsPlusOne + column) * 2 else index
                val topNeighbor = gradientScopeImpl.positions.getOffset(topNeighborIndex)
                val bottomNeighbor = gradientScopeImpl.positions.getOffset(bottomNeighborIndex)
                val verticalVector = (bottomNeighbor - topNeighbor).normalized() * 0.33f

                val currentTopBezierOffset = gradientScopeImpl.topBezierOffsets.getOffset(index)

                if (currentTopBezierOffset.isUnspecified) {
                    val distance = topNeighbor.distanceFrom(currentPosition)
                    val offset = verticalVector * distance * -1f
                    gradientScopeImpl.topBezierOffsets.setOffset(index, offset)
                }

                val currentBottomBezierOffset =
                    gradientScopeImpl.bottomBezierOffsets.getOffset(index)
                if (currentBottomBezierOffset.isUnspecified) {
                    val distance = bottomNeighbor.distanceFrom(currentPosition)
                    val offset = verticalVector * distance
                    gradientScopeImpl.bottomBezierOffsets.setOffset(index, offset)
                }
            }
        }
    }
}

private class MeshGradientScopeImpl(override val rows: Int, override val columns: Int) :
    MeshGradientScope {

    val numberOfPoints = (rows + 1) * (columns + 1)
    val positions = FloatArray(numberOfPoints * 2)
    val colors = IntArray(numberOfPoints)
    val leftBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }
    val rightBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }
    val topBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }
    val bottomBezierOffsets = FloatArray(numberOfPoints * 2) { Float.NaN }

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
    override fun setVertex(
        row: Int,
        column: Int,
        position: Offset,
        color: Color,
        leftControlPoint: Offset,
        topControlPoint: Offset,
        rightControlPoint: Offset,
        bottomControlPoint: Offset,
    ) {
        requirePrecondition(row in 0..rows) { "row ($row) must be in range 0..$rows" }
        requirePrecondition(column in 0..columns) {
            "column ($column) must be in range 0..$columns"
        }

        val index = (row * (columns + 1) + column) * 2
        positions.setOffset(index, position)

        // This also converts the color to sRGB color space.
        colors[row * (columns + 1) + column] = color.toArgb()

        leftBezierOffsets.setOffset(index, leftControlPoint)
        rightBezierOffsets.setOffset(index, rightControlPoint)
        topBezierOffsets.setOffset(index, topControlPoint)
        bottomBezierOffsets.setOffset(index, bottomControlPoint)
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
