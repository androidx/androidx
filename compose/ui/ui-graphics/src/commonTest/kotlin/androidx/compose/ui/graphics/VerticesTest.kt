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
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class VerticesTest {

    @Test
    fun testVertices_listConstructor_valid() {
        val positions = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(10f, 10f))
        val textures = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(1f, 1f))
        val colors = listOf(Color.Red, Color.Green, Color.Blue)
        val indices = listOf(0, 1, 2)
        val vertices = Vertices(VertexMode.Triangles, positions, textures, colors, indices)

        assertEquals(6, vertices.positions.size) // 3 vertices * 2 floats
        assertEquals(6, vertices.textureCoordinates.size)
        assertEquals(3, vertices.colors.size)
        assertEquals(3, vertices.indices.size)
    }

    @Test
    fun testVertices_createFromRawArrays_valid() {
        val vertices =
            Vertices(
                vertexMode = VertexMode.Triangles,
                positions = FloatArray(6),
                textureCoordinates = FloatArray(6),
                colors = IntArray(3),
                indices = shortArrayOf(0, 1, 2),
            )
        assertEquals(6, vertices.positions.size)
    }

    @Test
    fun testVertices_constructorsProduceSameDrawData() {
        val positionsList = listOf(Offset(0f, 0f), Offset(10f, 0f), Offset(5f, 10f))
        val texturesList = listOf(Offset(0f, 0f), Offset(1f, 0f), Offset(0.5f, 1f))
        val colorsList = listOf(Color.Red, Color.Green, Color.Blue)
        val indicesList = listOf(0, 1, 2)

        val verticesFromList =
            Vertices(VertexMode.Triangles, positionsList, texturesList, colorsList, indicesList)

        val rawPositions = positionsList.flatMap { listOf(it.x, it.y) }.toFloatArray()
        val rawTextures = texturesList.flatMap { listOf(it.x, it.y) }.toFloatArray()
        val rawColors = colorsList.map { it.toArgb() }.toIntArray()
        val rawIndices = indicesList.map { it.toShort() }.toShortArray()

        val verticesFromRaw =
            Vertices(VertexMode.Triangles, rawPositions, rawTextures, rawColors, rawIndices)

        assertEquals(verticesFromList.vertexMode, verticesFromRaw.vertexMode)
        assertContentEquals(verticesFromList.positions, verticesFromRaw.positions)
        assertContentEquals(verticesFromList.textureCoordinates, verticesFromRaw.textureCoordinates)
        assertContentEquals(verticesFromList.colors, verticesFromRaw.colors)
        assertContentEquals(verticesFromList.indices, verticesFromRaw.indices)
    }

    @Test
    fun testVertices_oddPositionsLength_throwsException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                Vertices(
                    vertexMode = VertexMode.Triangles,
                    positions = FloatArray(3),
                    textureCoordinates = FloatArray(3),
                    colors = IntArray(1),
                    indices = ShortArray(0),
                )
            }
        assertEquals("positions length must be even", exception.message)
    }

    @Test
    fun testVertices_mismatchedColors_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            Vertices(
                vertexMode = VertexMode.Triangles,
                positions = FloatArray(4),
                textureCoordinates = FloatArray(4),
                colors = IntArray(1),
                indices = ShortArray(0),
            )
        }
    }

    @Test
    fun testVertices_outOfBoundsIndex_throwsException() {
        assertFailsWith<IllegalArgumentException> {
            Vertices(
                vertexMode = VertexMode.Triangles,
                positions = FloatArray(4),
                textureCoordinates = FloatArray(4),
                colors = IntArray(2),
                indices = shortArrayOf(2),
            )
        }
    }
}
