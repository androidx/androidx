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
import java.io.Closeable
import java.nio.FloatBuffer

/**
 * A list of vertex attribute data stored GPU-side.
 *
 * One or more [VertexBuffer]s are used when constructing a [Mesh] to describe vertex attribute data
 * for example, local coordinates, texture coordinates, vertex normals, etc.
 *
 * See
 * [glVertexAttribPointer](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glVertexAttribPointer.xhtml)
 */
class VertexBuffer(numberOfEntriesPerVertex: Int, val entries: FloatBuffer?) : Closeable {

    internal var numberOfEntriesPerVertex: Int = 0
    private val buffer: GpuBuffer
    internal val bufferId: Int
        get() = buffer.getBufferId()

    internal val numberOfVertices: Int
        get() = buffer.size / numberOfEntriesPerVertex

    /**
     * Construct a [VertexBuffer] populated with initial data.
     *
     * The GPU buffer will be filled with the data in the _direct_ buffer [entries], starting from
     * the beginning of the buffer (not the current cursor position). The cursor will be left in an
     * undefined position after this function returns.
     *
     * The number of vertices in the buffer can be expressed as
     * [entries.limit() / numberOfEntriesPerVertex]. Thus, The size of the buffer must be divisible
     * by [numberOfEntriesPerVertex].
     *
     * The [entries] buffer may be null, in which case an empty buffer is constructed instead.
     */
    init {
        this.numberOfEntriesPerVertex = numberOfEntriesPerVertex
        require(entries == null || entries.limit() % numberOfEntriesPerVertex == 0) {
            "If non-null, vertex buffer data must be divisible by the number of data points per vertex"
        }

        buffer = GpuBuffer(GLES30.GL_ARRAY_BUFFER, GpuBuffer.FLOAT_SIZE, entries)
    }

    /**
     * Populate with new data.
     *
     * The entire buffer is replaced by the contents of the _direct_ buffer [entries] starting from
     * the beginning of the buffer, not the current cursor position. The cursor will be left in an
     * undefined position after this function returns.
     *
     * The GPU buffer is reallocated automatically if necessary.
     *
     * The [entries] buffer may be null, in which case the buffer will become empty. Otherwise, the
     * size of [entries] must be divisible by the number of entries per vertex specified during
     * construction.
     */
    fun set(entries: FloatBuffer?) {
        require(entries == null || entries.limit() % numberOfEntriesPerVertex == 0) {
            "If non-null, vertex buffer data must be divisible by the number of data points per vertex"
        }
        buffer.set(entries)
    }

    override fun close() {
        buffer.free()
    }
}
