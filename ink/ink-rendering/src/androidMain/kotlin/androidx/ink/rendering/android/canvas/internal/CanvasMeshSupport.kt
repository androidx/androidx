/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.rendering.android.canvas.internal

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Mesh
import android.graphics.MeshSpecification
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.WeakHashMap

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class CanvasMeshSupport {

    /**
     * Memoizes the results of [Canvas.drawMesh] in [canDrawMesh] to avoid trying that for every
     * coat of every stroke on every frame.
     */
    private val cachedDrawMeshResults = WeakHashMap<Canvas, Boolean>()

    /**
     * Normally, [Canvas.drawMesh] is only supported when [Canvas.isHardwareAccelerated] is true.
     * But screenshot tests use a hidden API to allow a [Canvas] instance to proceed with
     * hardware-only features in software rendering mode. This checks directly if [Canvas.drawMesh]
     * works with a simple mesh, because we cannot access the hidden API. Normal software rendering
     * that a developer can configure throws an error when calling [Canvas.drawMesh], so that
     * situation must be detected to fall back to other rendering approaches.
     *
     * TODO: b/444264199 - Update this logic if a better API to detect this is made available.
     */
    fun canDrawMesh(canvas: Canvas): Boolean {
        if (canvas.isHardwareAccelerated) return true
        val cachedDrawMeshResult = cachedDrawMeshResults[canvas]
        if (cachedDrawMeshResult != null) {
            return cachedDrawMeshResult
        }
        return runCatching { canvas.drawMesh(SIMPLE_MESH, BlendMode.DST, SIMPLE_PAINT) }
            .isSuccess
            .also { cachedDrawMeshResults[canvas] = it }
    }

    private companion object {
        /** As simple as possible for [canDrawMesh]. */
        val SIMPLE_MESH_SPECIFICATION by lazy {
            MeshSpecification.make(
                arrayOf(
                    MeshSpecification.Attribute(
                        /* type = */ MeshSpecification.TYPE_FLOAT2,
                        /* offset = */ 0,
                        /* name = */ "position",
                    )
                ),
                8,
                // `position` is included in varyings by default
                arrayOf<MeshSpecification.Varying>(),
                """
        Varyings main(const Attributes attributes) {
             Varyings varyings;
             varyings.position = attributes.position;
             return varyings;
        }
        """
                    .trimIndent(),
                """
        float2 main(const Varyings varyings, out float4 color) {
               color = vec4(0.0, 0.0, 0.0, 0.0);
               return varyings.position;
        }
        """
                    .trimIndent(),
            )
        }

        /** As simple as possible for [canDrawMesh]. */
        val SIMPLE_MESH by lazy {
            Mesh(
                SIMPLE_MESH_SPECIFICATION,
                Mesh.TRIANGLES,
                FloatBuffer.wrap(
                    floatArrayOf(
                        0F,
                        0F, // Vertex 0
                        1F,
                        1F, // Vertex 1
                        0F,
                        1F, // Vertex 2
                    )
                ),
                3,
                ShortBuffer.wrap(shortArrayOf(0, 1, 2)),
                RectF(0F, 0F, 1F, 1F),
            )
        }

        /** As simple as possible for [canDrawMesh]. */
        val SIMPLE_PAINT =
            Paint().apply {
                blendMode = BlendMode.DST
                color = Color.TRANSPARENT
            }
    }
}
