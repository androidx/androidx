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

import androidx.annotation.RestrictTo
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Utility functions for creating and manipulating native byte order [ByteBuffer] objects.
 *
 * ## Direct ByteBuffers & Native Endianness
 * Direct byte buffers are allocated using [ByteBuffer.allocateDirect] which stores data in native
 * memory outside the garbage-collected heap. This avoids JNI copying overhead when buffers are
 * shared with native components (such as a camera HAL or GPU).
 *
 * Direct buffers must explicitly configure their byte order to match the native platform order via
 * [ByteBuffer.order] with [ByteOrder.nativeOrder]. Modern Android architectures (ARM, x86) are
 * little-endian. If not configured, Java ByteBuffers default to big-endian, causing native code
 * (which expects little-endian) to read numbers incorrectly, resulting in image corruption or
 * crashes.
 *
 * For more details, see:
 * - [ByteBuffer Direct Buffers](https://developer.android.com/reference/java/nio/ByteBuffer#direct)
 * - [ByteBuffer Byte
 *   Order](https://developer.android.com/reference/java/nio/ByteBuffer#order(java.nio.ByteOrder))
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object FakeByteBuffers {

    /**
     * Allocates a new direct [ByteBuffer] configured with the native byte order.
     *
     * This is a convenience method that combines [ByteBuffer.allocateDirect] and
     * [toNativeByteOrder].
     *
     * @param size The size of the buffer in bytes. Must be non-negative.
     * @return A direct [ByteBuffer] with native byte order.
     * @throws IllegalArgumentException If [size] is negative.
     */
    fun allocateNative(size: Int): ByteBuffer {
        return ByteBuffer.allocateDirect(size).toNativeByteOrder()
    }

    /**
     * Configures this [ByteBuffer] to use the native byte order.
     *
     * This is an extension function that sets the byte order of the receiver [ByteBuffer] to
     * [ByteOrder.nativeOrder] and returns the buffer itself to allow for method chaining.
     *
     * @return The receiver [ByteBuffer] with its byte order set to native.
     */
    fun ByteBuffer.toNativeByteOrder(): ByteBuffer {
        return this.order(ByteOrder.nativeOrder())
    }

    /**
     * Slices the [ByteBuffer] from [position] to [limit] and returns a new buffer configured with
     * native byte order.
     *
     * The returned buffer's content will be a shared subsequence of this buffer's content.
     * Modifications to the returned buffer's content will be visible in this buffer, and vice
     * versa; the two buffers' position, limit, and mark values will be independent.
     *
     * This method temporarily modifies the position and limit of the receiver buffer to perform the
     * slice, but restores them before returning.
     *
     * @param position The start index (inclusive) in the receiver buffer. Must be non-negative and
     *   not larger than [limit].
     * @param limit The end index (exclusive) in the receiver buffer. Must be not smaller than
     *   [position] and not larger than the receiver buffer's capacity.
     * @return A sliced [ByteBuffer] configured with native byte order.
     * @throws IllegalArgumentException If [position] or [limit] is invalid.
     */
    fun ByteBuffer.sliceNative(position: Int, limit: Int): ByteBuffer {
        val originalPosition = this.position()
        val originalLimit = this.limit()
        try {
            this.position(position)
            this.limit(limit)
            return this.slice().toNativeByteOrder()
        } finally {
            this.position(0)
            this.limit(originalLimit)
            this.position(originalPosition)
        }
    }
}
