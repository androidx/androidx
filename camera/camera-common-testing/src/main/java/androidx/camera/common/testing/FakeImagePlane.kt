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

package androidx.camera.common.testing

import androidx.camera.common.ImagePlane
import java.lang.Class
import java.nio.ByteBuffer

/**
 * Fake implementation of [ImagePlane] for testing classes that consume camera image planes.
 *
 * This class provides lazy buffer allocation and multiple construction paradigms to support both
 * standalone dimension-based allocation and slicing from a shared contiguous parent buffer.
 *
 * ## Usage Examples
 *
 * ### 1. Standalone Allocation by Dimensions
 * Allocates a direct [ByteBuffer] of size `rowStride * rowCount` when accessed.
 *
 * ```
 * val plane = FakeImagePlane(rowStride = 640, rowCount = 480)
 * assertThat(plane.buffer.capacity()).isEqualTo(640 * 480)
 * ```
 *
 * ### 2. Custom Strides and Sub-sampling
 * Useful for mocking interleaved UV planes where pixel stride is 2.
 *
 * ```
 * val uvPlane = FakeImagePlane(rowStride = 640, rowCount = 240, pixelStride = 2)
 * ```
 *
 * ### 3. Backing by an Existing Buffer Slice
 * Avoids extra allocations by wrapping an existing buffer window (e.g. from a contiguous NV12
 * buffer). Ensure the slice maintains native byte ordering (`order(ByteOrder.nativeOrder())`).
 *
 * ```
 * val slice = parentBuffer.slice().order(ByteOrder.nativeOrder())
 * val plane = FakeImagePlane(rowStride = 640, pixelStride = 1, buffer = slice)
 * assertThat(plane.buffer).isSameInstanceAs(slice)
 * ```
 *
 * @param rowStride The row stride of the plane in bytes.
 * @param rowCount The height of the plane in rows. Used for lazy buffer allocation if [buffer] is
 *   null. Defaults to 1 (convenient for single-row compressed formats like JPEG).
 * @param pixelStride The distance between adjacent pixel samples in bytes. Defaults to 1.
 * @param buffer An optional pre-allocated [ByteBuffer] to back this plane. If null, a direct buffer
 *   of size `rowStride * rowCount` is allocated lazily upon first access.
 */
public class FakeImagePlane
@JvmOverloads
constructor(
    override val rowStride: Int,
    private val rowCount: Int = 1,
    override val pixelStride: Int = 1,
    buffer: ByteBuffer? = null,
) : ImagePlane {
    private val _providedBuffer = buffer

    override val buffer: ByteBuffer by lazy {
        _providedBuffer ?: FakeByteBuffers.allocateNative(rowStride * rowCount)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == ByteBuffer::class.java -> buffer as T
            else -> null
        }
}
