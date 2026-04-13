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

package androidx.xr.scenecore

import androidx.annotation.IntRange
import java.nio.ByteBuffer

/**
 * A container holding a reference to a [ByteBuffer], along with an offset and size.
 *
 * This is used to define a specific slice or region within a buffer.
 *
 * @property buffer containing the data
 * @property offset starting position within the buffer in bytes
 * @property size number of bytes in the region
 * @throws IllegalArgumentException if `offset` or `size` is negative, or if `offset + size` exceeds
 *   `buffer.capacity()`.
 */
@ExperimentalCustomMeshApi
public class ByteBufferRegion(
    public val buffer: ByteBuffer,
    @IntRange(from = 0) public val offset: Int,
    @IntRange(from = 0) public val size: Int,
) {
    init {
        require(offset >= 0) { "offset must not be negative" }
        require(size >= 0) { "size must not be negative" }
        require(size <= buffer.capacity() - offset) { "size + offset must not exceed capacity" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteBufferRegion

        if (buffer !== other.buffer) return false
        if (offset != other.offset) return false
        if (size != other.size) return false

        return true
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(buffer)
        result = 31 * result + offset
        result = 31 * result + size
        return result
    }
}
