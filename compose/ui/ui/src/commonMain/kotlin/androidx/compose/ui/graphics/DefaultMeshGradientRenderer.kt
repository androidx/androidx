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

/**
 * Fully platform-independent [BaseMeshGradientRenderer] that draws the tessellated triangle mesh
 * through the common [Canvas.drawVertices] API.
 *
 * This uses no platform-specific types, so any backend can use it as-is. Backends that can avoid
 * the per-frame [Vertices] allocation by calling their native canvas directly supply their own
 * [BaseMeshGradientRenderer] subclass instead.
 */
internal class DefaultMeshGradientRenderer : BaseMeshGradientRenderer() {
    private val paint = Paint()

    @Suppress("PrimitiveInCollection")
    override fun drawTriangles(
        canvas: Canvas,
        surfacePositions: FloatArray,
        surfaceColors: IntArray,
        indices: ShortArray,
        vertexCount: Int,
    ) {
        val vertexPositions =
            List(vertexCount) { i -> Offset(surfacePositions[i * 2], surfacePositions[i * 2 + 1]) }
        val vertexColors = List(vertexCount) { i -> Color(surfaceColors[i]) }
        val vertexIndices = List(indices.size) { i -> indices[i].toInt() }

        canvas.drawVertices(
            vertices =
                Vertices(
                    vertexMode = VertexMode.Triangles,
                    positions = vertexPositions,
                    textureCoordinates = vertexPositions,
                    colors = vertexColors,
                    indices = vertexIndices,
                ),
            blendMode = BlendMode.Dst,
            paint = paint,
        )
    }
}
