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

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlin.math.ceil
import kotlin.math.sqrt

private const val MinSubdivision = 4
private const val MaxSubdivision = 64
private const val TargetPxPerSegment = 8f

/**
 * Calculates the flat index into a vertex-based array (like positions or colors) based on the [row]
 * and [col] in a grid with a specific number of [columns].
 *
 * Since a mesh with N columns has N+1 vertices horizontally, the stride used is (columns + 1).
 */
internal fun meshGradientPointIndex(row: Int, col: Int, columns: Int): Int =
    row * (columns + 1) + col

/**
 * Dynamically calculates the number of subdivisions (segments) for the mesh grid based on the
 * physical size of the largest patch. This is to avoid over tessellations when a higher LOD is not
 * necessarily required.
 *
 * @param rows The number of rows in the mesh.
 * @param columns The number of columns in the mesh.
 * @param positions The array of mesh positions.
 * @param size The total size of the area where the gradient is being drawn.
 */
internal fun calculateMeshGradientSubdivisions(
    rows: Int,
    columns: Int,
    positions: FloatArray,
    size: Size,
): IntSize {
    var maxW = 0f
    var maxH = 0f
    for (patchIdx in 0 until rows * columns) {
        val patchRow = patchIdx / columns
        val patchColumn = patchIdx % columns
        val topLeft = meshGradientPointIndex(patchRow, patchColumn, columns) * 2
        val topRight = meshGradientPointIndex(patchRow, patchColumn + 1, columns) * 2
        val bottomLeft = meshGradientPointIndex(patchRow + 1, patchColumn, columns) * 2
        val bottomRight = meshGradientPointIndex(patchRow + 1, patchColumn + 1, columns) * 2

        val patchWidth =
            (dist(
                positions[topLeft] * size.width,
                positions[topLeft + 1] * size.height,
                positions[topRight] * size.width,
                positions[topRight + 1] * size.height,
            ) +
                dist(
                    positions[bottomLeft] * size.width,
                    positions[bottomLeft + 1] * size.height,
                    positions[bottomRight] * size.width,
                    positions[bottomRight + 1] * size.height,
                )) * 0.5f
        val patchHeight =
            (dist(
                positions[topLeft] * size.width,
                positions[topLeft + 1] * size.height,
                positions[bottomLeft] * size.width,
                positions[bottomLeft + 1] * size.height,
            ) +
                dist(
                    positions[topRight] * size.width,
                    positions[topRight + 1] * size.height,
                    positions[bottomRight] * size.width,
                    positions[bottomRight + 1] * size.height,
                )) * 0.5f

        maxW = maxOf(maxW, patchWidth)
        maxH = maxOf(maxH, patchHeight)
    }

    val subdivisionsU =
        ceil(maxW / TargetPxPerSegment).toInt().coerceIn(MinSubdivision, MaxSubdivision)
    val subdivisionsV =
        ceil(maxH / TargetPxPerSegment).toInt().coerceIn(MinSubdivision, MaxSubdivision)
    return IntSize(subdivisionsU, subdivisionsV)
}

private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}
