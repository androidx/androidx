/*
 * Copyright 2018 The Android Open Source Project
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
import androidx.compose.ui.util.fastAny

/** A set of vertex data used by [Canvas.drawVertices]. */
public class Vertices /*extends NativeFieldWrapperClass2*/ {

    public val vertexMode: VertexMode
    public val positions: FloatArray
    public val textureCoordinates: FloatArray
    public val colors: IntArray
    public val indices: ShortArray

    public constructor(
        vertexMode: VertexMode,
        positions: List<Offset>,
        textureCoordinates: List<Offset>,
        colors: List<Color>,
        indices: List<Int>,
    ) {
        validateLists(positions, textureCoordinates, colors, indices)
        this.vertexMode = vertexMode
        this.positions = encodePointList(positions)
        this.textureCoordinates = encodePointList(textureCoordinates)
        this.colors = encodeColorList(colors)
        this.indices = ShortArray(indices.size) { i -> indices[i].toShort() }
    }

    /**
     * Creates a [Vertices] instance from the arrays. For performance reasons, this constructor does
     * not make defensive copies of the provided arrays, instead uses them directly. The [Vertices]
     * instance takes full ownership of the provided raw data. The caller must **not** mutate these
     * arrays after this instance is created, as modifying the data may lead to unpredictable
     * rendering behavior.
     *
     * @param vertexMode The [VertexMode] used to draw the vertices.
     * @param positions A [FloatArray] of x, y pairs representing vertex positions.
     * @param textureCoordinates A [FloatArray] of u, v pairs representing texture coordinates.
     * @param colors An [IntArray] of ARGB colors for each vertex.
     * @param indices A [ShortArray] of indices into the positions (texture, color) array.
     */
    public constructor(
        vertexMode: VertexMode,
        positions: FloatArray,
        textureCoordinates: FloatArray,
        colors: IntArray,
        indices: ShortArray,
    ) {
        validateArrays(positions, textureCoordinates, colors, indices)
        this.vertexMode = vertexMode
        this.positions = positions
        this.textureCoordinates = textureCoordinates
        this.colors = colors
        this.indices = indices
    }

    private fun validateArrays(
        positions: FloatArray,
        textureCoordinates: FloatArray,
        colors: IntArray,
        indices: ShortArray,
    ) {
        if (positions.size % 2 != 0) throwIllegalArgumentException("positions length must be even")

        val vertexCount = positions.size / 2

        if (textureCoordinates.size != positions.size)
            throwIllegalArgumentException("positions and textureCoordinates lengths must match.")

        if (colors.size != vertexCount)
            throwIllegalArgumentException("positions and colors lengths must match.")

        for (i in indices.indices) {
            val index = indices[i].toInt()
            if (index !in 0..<vertexCount)
                throwIllegalArgumentException(
                    "indices values must be valid indices in the positions list."
                )
        }
    }

    @Suppress("PrimitiveInCollection")
    private fun validateLists(
        positions: List<Offset>,
        textureCoordinates: List<Offset>,
        colors: List<Color>,
        indices: List<Int>,
    ) {
        if (textureCoordinates.size != positions.size)
            throwIllegalArgumentException("positions and textureCoordinates lengths must match.")
        if (colors.size != positions.size)
            throwIllegalArgumentException("positions and colors lengths must match.")
        if (indices.fastAny { it < 0 || it >= positions.size })
            throwIllegalArgumentException(
                "indices values must be valid indices " + "in the positions list."
            )
    }

    private fun encodeColorList(colors: List<Color>): IntArray {
        return IntArray(colors.size) { i -> colors[i].toArgb() }
    }

    private fun encodePointList(points: List<Offset>): FloatArray {
        return FloatArray(points.size * 2) { i ->
            val pointIndex = i / 2
            val point = points[pointIndex]
            if (i % 2 == 0) {
                point.x
            } else {
                point.y
            }
        }
    }
}
