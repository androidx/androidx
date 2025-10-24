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
import java.nio.Buffer
import java.nio.IntBuffer
import java.nio.ShortBuffer

/**
 * A list of vertex indices stored GPU-side.
 *
 * When constructing a [Mesh], an [IndexBuffer] may be passed to describe the ordering of vertices
 * when drawing each primitive.
 *
 * The GPU buffer will be filled with the data in the _direct_ buffer [entries], starting from the
 * beginning of the buffer (not the current cursor position). The cursor will be left in an
 * undefined position after this function returns.
 *
 * The [entries] buffer may be null, in which case an empty buffer is constructed instead.
 *
 * See
 * [glDrawElements](https://www.khronos.org/registry/OpenGL-Refpages/es3.0/html/glDrawElements.xhtml)
 *
 * @param render The [SampleRender] instance to which this buffer belongs.
 * @param entries The optional [Buffer] containing the index data.
 */
class IndexBuffer(val render: SampleRender, val entries: Buffer?) : Closeable {
    private var buffer =
        GpuBuffer(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            if (entries is ShortBuffer) {
                GpuBuffer.SHORT_SIZE
            } else {
                GpuBuffer.INT_SIZE
            },
            entries,
        )

    internal val bufferId: Int
        get() = buffer.getBufferId()

    /**
     * Populate with new data.
     *
     * The entire buffer is replaced by the contents of the _direct_ buffer [entries] starting from
     * the beginning of the buffer, not the current cursor position. The cursor will be left in an
     * undefined position after this function returns.
     *
     * The GPU buffer is reallocated automatically if necessary.
     *
     * The [entries] buffer may be null, in which case the buffer will become empty.
     */
    fun set(entries: IntBuffer) {
        buffer.set(entries)
    }

    override fun close() {
        buffer.free()
    }

    internal fun getSize(): Int {
        return buffer.size
    }
}
