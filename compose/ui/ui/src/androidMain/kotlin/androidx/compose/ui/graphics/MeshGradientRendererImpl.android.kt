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

import android.os.Build

/**
 * [BaseMeshGradientRenderer] that draws the tessellated mesh through the framework
 * [android.graphics.Canvas.drawVertices], which is hardware accelerated from API 29 and above.
 *
 * Unlike [DefaultMeshGradientRenderer], this feeds the primitive vertex buffers straight to the
 * platform canvas, avoiding the per-frame collection allocations.
 */
internal class MeshGradientRendererImpl : BaseMeshGradientRenderer() {

    private val paint = android.graphics.Paint()

    override fun createColorsBuffer(vertexCount: Int): IntArray =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) IntArray(vertexCount)
        else IntArray(vertexCount * 2)

    override fun drawTriangles(
        canvas: Canvas,
        surfacePositions: FloatArray,
        surfaceColors: IntArray,
        indices: ShortArray,
        vertexCount: Int,
    ) {
        canvas.nativeCanvas.drawVertices(
            android.graphics.Canvas.VertexMode.TRIANGLES,
            surfacePositions.size,
            surfacePositions,
            0,
            null,
            0,
            surfaceColors,
            0,
            indices,
            0,
            indices.size,
            paint,
        )
    }
}
