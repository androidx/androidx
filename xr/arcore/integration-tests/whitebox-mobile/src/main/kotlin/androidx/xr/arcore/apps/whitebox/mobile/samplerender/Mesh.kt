/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.apps.whitebox.mobile.samplerender

import android.opengl.GLES30
import androidx.xr.runtime.XrLog
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import java.io.Closeable
import java.io.InputStream
import java.nio.ShortBuffer

/**
 * A collection of vertices, faces, and other attributes that define how to render a 3D object.
 *
 * To render the mesh, use [SampleRender.draw].
 *
 * The data in the given [IndexBuffer] and [VertexBuffer]s does not need to be finalized; they may
 * be freely changed throughout the lifetime of a [Mesh] using their respective set() methods.
 *
 * The ordering of the vertexBuffers is significant. Their array indices will correspond to their
 * attribute locations, which must be taken into account in shader code. The
 * [layout qualifier](https://www.khronos.org/opengl/wiki/Layout_Qualifier_(GLSL)) must be used in
 * the vertex shader code to explicitly associate attributes with these indices.
 *
 * @param render The [SampleRender] instance to use for rendering.
 * @param primitiveMode The kind of primitive to render.
 * @param indexBuffer The index buffer to use for rendering.
 * @param vertexBuffers The vertex buffers to use for rendering. Must be non-null and non-empty.
 */
class Mesh(
    val render: SampleRender,
    private val primitiveMode: PrimitiveMode,
    private val indexBuffer: IndexBuffer?,
    private val vertexBuffers: Array<VertexBuffer>?,
) : Closeable {

    private val vertexArrayId: IntArray = intArrayOf(0)

    init {
        require(!vertexBuffers.isNullOrEmpty()) { "Must pass at least one vertex buffer" }

        try {
            // Create vertex array
            GLES30.glGenVertexArrays(1, vertexArrayId, 0)
            maybeThrowGLException("Failed to generate a vertex array", "glGenVertexArrays")

            // Bind vertex array
            GLES30.glBindVertexArray(vertexArrayId[0])
            maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray")

            if (indexBuffer != null) {
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.bufferId)
            }

            for (i in vertexBuffers.indices) {
                // Bind each vertex buffer to vertex array
                GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBuffers[i].bufferId)
                maybeThrowGLException("Failed to bind vertex buffer", "glBindBuffer")
                GLES30.glVertexAttribPointer(
                    i,
                    vertexBuffers[i].numberOfEntriesPerVertex,
                    GLES30.GL_FLOAT,
                    false,
                    0,
                    0,
                )
                maybeThrowGLException(
                    "Failed to associate vertex buffer with vertex array",
                    "glVertexAttribPointer",
                )
                GLES30.glEnableVertexAttribArray(i)
                maybeThrowGLException("Failed to enable vertex buffer", "glEnableVertexAttribArray")
            }
        } catch (t: Throwable) {
            close()
            throw t
        }
    }

    /**
     * The kind of primitive to render.
     *
     * This determines how the data in [VertexBuffer]s are interpreted. See
     * [here](https://www.khronos.org/opengl/wiki/Primitive) for more on how primitives behave.
     */
    enum class PrimitiveMode(val glesEnum: Int) {
        POINTS(GLES30.GL_POINTS),
        LINE_STRIP(GLES30.GL_LINE_STRIP),
        LINE_LOOP(GLES30.GL_LINE_LOOP),
        LINES(GLES30.GL_LINES),
        TRIANGLE_STRIP(GLES30.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GLES30.GL_TRIANGLE_FAN),
        TRIANGLES(GLES30.GL_TRIANGLES),
    }

    override fun close() {
        if (vertexArrayId[0] != 0) {
            GLES30.glDeleteVertexArrays(1, vertexArrayId, 0)
            maybeLogGLError(
                XrLog.Level.WARN,
                "Failed to free vertex array object",
                "glDeleteVertexArrays",
            )
        }
    }

    /**
     * Draws the mesh. Don't call this directly unless you are doing low level OpenGL code; instead,
     * prefer [SampleRender.draw].
     */
    fun lowLevelDraw() {
        if (vertexArrayId[0] == 0) {
            throw IllegalStateException("Tried to draw a freed Mesh")
        }

        GLES30.glBindVertexArray(vertexArrayId[0])
        maybeThrowGLException("Failed to bind vertex array object", "glBindVertexArray")
        if (indexBuffer == null) {
            val vertexCount = vertexBuffers!![0].numberOfVertices
            for (i in vertexBuffers.indices) {
                val iterCount = vertexBuffers[i].numberOfVertices
                if (iterCount != vertexCount) {
                    throw IllegalStateException(
                        String.format(
                            "Vertex buffers have mismatching numbers of vertices ([0] has %d but [%d] has" +
                                " %d)",
                            vertexCount,
                            i,
                            iterCount,
                        )
                    )
                }
            }
            GLES30.glDrawArrays(primitiveMode.glesEnum, 0, vertexCount)
            maybeThrowGLException("Failed to draw vertex array object", "glDrawArrays")
        } else {
            GLES30.glDrawElements(
                primitiveMode.glesEnum,
                indexBuffer.getSize(),
                if (indexBuffer.entries is ShortBuffer) {
                    GLES30.GL_UNSIGNED_SHORT
                } else {
                    GLES30.GL_UNSIGNED_INT
                },
                0,
            )
            maybeThrowGLException(
                "Failed to draw vertex array object with indices",
                "glDrawElements",
            )
        }
    }

    companion object {
        private const val TAG: String = "Mesh"

        /**
         * Constructs a [Mesh] from the given Wavefront OBJ file.
         *
         * The [Mesh] will be constructed with three attributes, indexed in the order of local
         * coordinates (location 0, vec3), texture coordinates (location 1, vec2), and vertex
         * normals (location 2, vec3).
         */
        fun createFromAsset(render: SampleRender, assetFileName: String): Mesh {
            val inputStream: InputStream = render.getAssets().open(assetFileName)
            inputStream.use {
                val obj = ObjUtils.convertToRenderable(ObjReader.read(inputStream))

                // Obtain the data from the OBJ, as direct buffers:
                val vertexIndices = ObjData.getFaceVertexIndices(obj, /* numVerticesPerFace= */ 3)
                val localCoordinates = ObjData.getVertices(obj)
                val textureCoordinates = ObjData.getTexCoords(obj, /* dimensions= */ 2)
                val normals = ObjData.getNormals(obj)

                val vertexBuffers =
                    arrayOf(
                        VertexBuffer(3, localCoordinates),
                        VertexBuffer(2, textureCoordinates),
                        VertexBuffer(3, normals),
                    )

                val indexBuffer = IndexBuffer(render, vertexIndices)

                return Mesh(render, Mesh.PrimitiveMode.TRIANGLES, indexBuffer, vertexBuffers)
            }
        }
    }
}
