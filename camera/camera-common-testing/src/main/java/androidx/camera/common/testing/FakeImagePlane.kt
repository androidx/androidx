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
 * Fake implementation of [ImagePlane] for unit testing components that consume camera image planes.
 *
 * `FakeImagePlane` supports lazy buffer allocation and custom buffer backing through three
 * construction patterns:
 * - **Standalone allocation by dimensions**: Leave `buffer` as `null` to lazily allocate a
 *   native-order direct [ByteBuffer] of `rowStride * rowCount` bytes on first access.
 * - **Custom strides and sub-sampling**: Specify `pixelStride` to simulate interleaved chroma
 *   planes where adjacent samples are separated by 2 bytes.
 * - **Existing buffer slice backing**: Pass a pre-allocated native-order [ByteBuffer] slice to
 *   share a contiguous parent buffer across multiple planes without extra allocations.
 *
 * @param rowStride The row stride of the plane in bytes.
 * @param rowCount The height of the plane in rows. Used for lazy buffer allocation when `buffer` is
 *   `null`. Defaults to `1` for single-row compressed formats such as JPEG.
 * @param pixelStride The distance between adjacent pixel samples in bytes. Defaults to `1`.
 * @param buffer An optional pre-allocated [ByteBuffer] that backs this plane. If `null`, accessing
 *   [buffer] lazily allocates a native-order direct [ByteBuffer] of `rowStride * rowCount` bytes
 *   via [FakeByteBuffers.allocateNative].
 * @sample androidx.camera.common.testing.samples.fakeImagePlaneSample
 * @sample androidx.camera.common.testing.samples.fakeImageCustomPlanesSample
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

    /**
     * A [ByteBuffer] containing the image data for this plane.
     *
     * If a custom [buffer] was passed to the constructor, it is returned directly. Otherwise, a
     * direct [ByteBuffer] with native byte order and capacity equal to `rowStride * rowCount` is
     * allocated lazily upon first access.
     */
    override val buffer: ByteBuffer by lazy {
        _providedBuffer ?: FakeByteBuffers.allocateNative(rowStride * rowCount)
    }

    /**
     * Unwraps this fake image plane to its underlying implementation types.
     *
     * This implementation supports unwrapping to:
     * * [FakeImagePlane] (returns `this`)
     * * [ByteBuffer] (returns the backing [buffer])
     *
     * @param type The class representing the type to unwrap to.
     * @return The unwrapped object instance of type [T], or `null` if the requested type is not
     *   supported.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == ByteBuffer::class.java -> buffer as T
            else -> null
        }
}
