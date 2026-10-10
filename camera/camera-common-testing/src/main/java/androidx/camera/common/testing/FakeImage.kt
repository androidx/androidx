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

import android.annotation.SuppressLint
import android.graphics.ImageFormat as GraphicsImageFormat
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.hardware.SyncFence
import android.os.Build
import androidx.camera.common.ImageDataSpace
import androidx.camera.common.ImageFormat
import androidx.camera.common.ImageFormats
import androidx.camera.common.ImagePlane
import androidx.camera.common.MutableImageWrapper
import androidx.camera.common.testing.FakeByteBuffers.sliceNative
import java.lang.Class
import java.nio.ByteBuffer
import kotlinx.atomicfu.atomic

/**
 * Fake implementation of [MutableImageWrapper] for unit testing.
 *
 * `FakeImage` simulates an image in tests without requiring camera hardware or native `Image`
 * instances. You can configure pixel buffers, image planes, and hardware buffers directly through
 * the constructor.
 *
 * ## Plane and Buffer Backing
 * By default, accessing [imagePlanes] lazily allocates a native-order direct [ByteBuffer] and
 * slices it into format-specific [ImagePlane] instances. You can override this default behavior in
 * two ways:
 * - Provide a custom `byteBuffer` in the constructor to supply specific pixel bytes for automatic
 *   plane generation.
 * - Provide a non-empty `imagePlanes` list in the constructor or assign [imagePlanes] to define
 *   custom plane layouts, row strides, or unsupported image formats.
 *
 * ## HardwareBuffer Behavior
 * On API level 26 and higher, you can pass a [HardwareBuffer] to the constructor or let
 * [hardwareBuffer] lazily create one on demand. Because the JVM cannot read pixel data directly
 * from a `HardwareBuffer`, [imagePlanes] always reads from the backing `byteBuffer` or custom
 * [ImagePlane] instances. Calling [close] closes the associated `HardwareBuffer` on the first
 * invocation.
 *
 * @sample androidx.camera.common.testing.samples.fakeImageSample
 * @sample androidx.camera.common.testing.samples.fakeImageCustomPlanesSample
 */
public open class FakeImage
/**
 * Creates a [FakeImage] instance with the specified dimensions, format, and optional backing
 * buffers.
 *
 * You can configure plane and buffer backing through the following constructor options:
 * - **Default automatic planes**: Leave `byteBuffer` as `null` and `imagePlanes` empty. Accessing
 *   [imagePlanes] lazily allocates a native-order direct [ByteBuffer] and slices it into planes for
 *   the formats listed in [imagePlanes].
 * - **Custom backing buffer**: Pass a pre-populated `byteBuffer` and leave `imagePlanes` empty.
 *   Accessing [imagePlanes] slices your buffer into format-specific planes.
 * - **Custom image planes**: Pass a non-empty `imagePlanes` list to test custom row strides, pixel
 *   strides, or formats that do not support automatic plane generation.
 *
 * @param width The image width in pixels.
 * @param height The image height in pixels.
 * @param format The image format constant from [android.graphics.ImageFormat].
 * @param timestamp The capture timestamp in nanoseconds.
 * @param byteBuffer An optional [ByteBuffer] that backs automatic [imagePlanes] generation and
 *   `unwrapAs(ByteBuffer::class.java)`. If `null`, `FakeImage` lazily allocates a native-order
 *   direct buffer of [ImageFormats.bytesPerImage] bytes when needed. If you provide a buffer and
 *   leave `imagePlanes` empty, its capacity must be at least the minimum byte size for `format`,
 *   `width`, and `height`.
 * @param hardwareBuffer An optional [HardwareBuffer] associated with this image. On API level 26
 *   and higher, its dimensions must match `width` and `height`. If `null`, accessing
 *   [hardwareBuffer] lazily creates a synthetic [HardwareBuffer] when supported by the platform.
 *   Calling [close] closes this buffer on the first invocation.
 * @param cropRect The visible crop rectangle of the image. Defaults to `Rect(0, 0, width, height)`.
 * @param imagePlanes An optional list of pre-configured [ImagePlane] instances. If non-empty,
 *   [imagePlanes] returns this list directly without slicing `byteBuffer`. If empty, [imagePlanes]
 *   lazily generates format-specific planes from `byteBuffer` on first access.
 * @throws IllegalArgumentException If `hardwareBuffer` is non-null on API level 26 or higher and
 *   its width or height does not match `width` or `height`.
 * @sample androidx.camera.common.testing.samples.fakeImageSample
 * @sample androidx.camera.common.testing.samples.fakeImageCustomPlanesSample
 */
@JvmOverloads
constructor(
    override var width: Int,
    override var height: Int,
    @ImageFormat override var format: Int,
    override var timestamp: Long,
    byteBuffer: ByteBuffer? = null,
    hardwareBuffer: HardwareBuffer? = null,
    override var cropRect: Rect = Rect(0, 0, width, height),
    imagePlanes: List<ImagePlane> = emptyList(),
) : MutableImageWrapper {
    private val _providedByteBuffer = byteBuffer
    private val _providedHardwareBuffer = hardwareBuffer

    init {
        if (Build.VERSION.SDK_INT >= 26 && _providedHardwareBuffer != null) {
            require(_providedHardwareBuffer.width == width) {
                "Provided HardwareBuffer width (${_providedHardwareBuffer.width}) must match requested width ($width)"
            }
            require(_providedHardwareBuffer.height == height) {
                "Provided HardwareBuffer height (${_providedHardwareBuffer.height}) must match requested height ($height)"
            }
        }
    }

    private val debugId = debugIds.incrementAndGet()
    private val _closeCount = atomic(0)
    /** Returns `true` if this image has been closed, `false` otherwise. */
    public open val isClosed: Boolean
        get() = _closeCount.value > 0

    /**
     * The number of times [close] has been called on this image.
     *
     * Useful for verifying lifecycle management in tests.
     */
    public val closeCount: Int
        get() = _closeCount.value

    override var syncFence: SyncFence? = null

    /**
     * The dataspace associated with this image.
     *
     * Defaults to [android.hardware.DataSpace.DATASPACE_UNKNOWN].
     */
    @get:SuppressLint("MethodNameUnits", "WrongConstant")
    @set:SuppressLint("MethodNameUnits", "WrongConstant")
    @get:ImageDataSpace
    @setparam:ImageDataSpace
    override var dataSpace: Int = android.hardware.DataSpace.DATASPACE_UNKNOWN

    private val lazyByteBuffer: ByteBuffer? by lazy {
        _providedByteBuffer
            ?: estimateMinimumByteBufferSize()?.let { size -> FakeByteBuffers.allocateNative(size) }
    }

    internal val byteBuffer: ByteBuffer?
        get() = lazyByteBuffer

    private val lazyHardwareBuffer = lazy {
        check(!isClosed)
        if (Build.VERSION.SDK_INT >= 26 && _providedHardwareBuffer != null) {
            _providedHardwareBuffer
        } else {
            // This will return null if hardware buffers are not supported, or if the provided
            // format is not available on the current API level.
            FakeHardwareBuffers.createForImage(
                imageFormat = format,
                imageWidth = width,
                imageHeight = height,
                hardwareBufferLayers = 1,
                hardwareBufferUsage = HardwareBuffer.USAGE_CPU_READ_OFTEN,
            )
        }
    }

    /**
     * The hardware buffer for this image.
     *
     * If a [HardwareBuffer] was provided in the constructor, that buffer is returned. Otherwise, a
     * mock [HardwareBuffer] is generated dynamically based on the image format, width, and height.
     *
     * Throws [IllegalStateException] if this image is already closed.
     */
    override val hardwareBuffer: HardwareBuffer?
        get() = lazyHardwareBuffer.value

    private var _imagePlanes = imagePlanes

    /**
     * The image planes for this image.
     *
     * If no image planes were provided in the constructor, they are generated automatically based
     * on the image format and the backing [ByteBuffer].
     *
     * Supported formats for automatic plane generation:
     * - [android.graphics.ImageFormat.YUV_420_888]
     * - [android.graphics.ImageFormat.NV21]
     * - [android.graphics.ImageFormat.JPEG]
     * - [android.graphics.ImageFormat.HEIC]
     * - [android.graphics.ImageFormat.DEPTH_JPEG]
     * - [android.graphics.ImageFormat.JPEG_R]
     * - [android.graphics.ImageFormat.DEPTH_POINT_CLOUD]
     * - [android.graphics.ImageFormat.PRIVATE]
     * - [android.graphics.ImageFormat.UNKNOWN]
     *
     * Throws [IllegalStateException] if this image is already closed. Throws
     * [UnsupportedOperationException] if automatic generation is attempted on an unsupported
     * format.
     */
    override var imagePlanes: List<ImagePlane>
        get() {
            check(!isClosed)
            if (_imagePlanes.isEmpty()) {
                _imagePlanes = generatePlanes()
            }
            return _imagePlanes
        }
        set(value) {
            _imagePlanes = value
        }

    private fun generatePlanes(): List<ImagePlane> {
        val buf =
            lazyByteBuffer
                ?: throw UnsupportedOperationException(
                    "Format $format is not currently supported in FakeImage"
                )
        val minSize =
            estimateMinimumByteBufferSize()
                ?: throw UnsupportedOperationException(
                    "Format $format is not currently supported in FakeImage"
                )
        check(buf.capacity() >= minSize) {
            "Provided ByteBuffer capacity (${buf.capacity()}) is smaller than estimated minimum capacity ($minSize) for format $format"
        }
        return when (format) {
            GraphicsImageFormat.YUV_420_888 -> createYuvPlanes(buf, isNv21 = false)
            GraphicsImageFormat.NV21 -> createYuvPlanes(buf, isNv21 = true)
            GraphicsImageFormat.JPEG,
            GraphicsImageFormat.HEIC,
            GraphicsImageFormat.DEPTH_JPEG,
            GraphicsImageFormat.JPEG_R,
            GraphicsImageFormat.DEPTH_POINT_CLOUD -> createSinglePlane(buf)
            GraphicsImageFormat.PRIVATE,
            GraphicsImageFormat.UNKNOWN -> createPrivatePlanes()
            else ->
                throw UnsupportedOperationException(
                    "Format $format is not currently supported in FakeImage"
                )
        }
    }

    /**
     * Unwraps this fake image to retrieve the underlying type.
     *
     * Supports unwrapping to:
     * - The [FakeImage] class itself (or subclasses).
     * - [ByteBuffer], which returns the backing buffer.
     * - [HardwareBuffer], which returns the associated hardware buffer (on API level 26+).
     *
     * @param type The class of the object to return.
     * @return The unwrapped object, or `null` if the type is not supported.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == ByteBuffer::class.java -> lazyByteBuffer as T?
            Build.VERSION.SDK_INT >= 26 && type == HardwareBuffer::class.java ->
                hardwareBuffer as T?
            else -> null
        }

    /**
     * Closes the image.
     *
     * Increments [closeCount]. If this is the first call to [close], it will also close the
     * underlying [HardwareBuffer] if it was provided or created.
     */
    override fun close() {
        if (_closeCount.incrementAndGet() == 1) {
            if (Build.VERSION.SDK_INT >= 26) {
                if (_providedHardwareBuffer != null) {
                    _providedHardwareBuffer.close()
                } else if (lazyHardwareBuffer.isInitialized()) {
                    lazyHardwareBuffer.value?.close()
                }
            }
        }
    }

    override fun toString(): String =
        "FakeImage-$debugId-${ImageFormats.name(format)}-w${width}h$height-t$timestamp"

    private companion object {
        private val debugIds = atomic(0)
    }
}

private fun FakeImage.estimateMinimumByteBufferSize(): Int? =
    when (format) {
        GraphicsImageFormat.YUV_420_888,
        GraphicsImageFormat.NV21,
        GraphicsImageFormat.JPEG,
        GraphicsImageFormat.HEIC,
        GraphicsImageFormat.DEPTH_JPEG,
        GraphicsImageFormat.JPEG_R,
        GraphicsImageFormat.DEPTH_POINT_CLOUD ->
            ImageFormats.bytesPerImage(format, width, height).toInt()
        GraphicsImageFormat.PRIVATE,
        GraphicsImageFormat.UNKNOWN -> 0
        else -> null
    }

private fun FakeImage.createYuvPlanes(
    backingBuffer: ByteBuffer,
    isNv21: Boolean,
): List<ImagePlane> {
    val ySize = width * height
    val uvSize = width * (height / 2)
    val totalSize = ySize + uvSize

    // Plane 0: Y
    val p0Buffer = backingBuffer.sliceNative(0, ySize)

    // Offset for U and V
    val uOffset = if (isNv21) ySize + 1 else ySize
    val vOffset = if (isNv21) ySize else ySize + 1

    // Plane 1: U
    val p1Buffer = backingBuffer.sliceNative(uOffset, totalSize)

    // Plane 2: V
    val p2Buffer = backingBuffer.sliceNative(vOffset, totalSize)

    return listOf(
        FakeImagePlane(rowStride = width, rowCount = height, pixelStride = 1, buffer = p0Buffer),
        FakeImagePlane(
            rowStride = width,
            rowCount = height / 2,
            pixelStride = 2,
            buffer = p1Buffer,
        ),
        FakeImagePlane(
            rowStride = width,
            rowCount = height / 2,
            pixelStride = 2,
            buffer = p2Buffer,
        ),
    )
}

private fun FakeImage.createSinglePlane(backingBuffer: ByteBuffer): List<ImagePlane> {
    val slice = backingBuffer.sliceNative(0, backingBuffer.capacity())
    return listOf(
        FakeImagePlane(rowStride = width, rowCount = height, pixelStride = 1, buffer = slice)
    )
}

/**
 * Creates three zero-sized planes for PRIVATE or UNKNOWN formats.
 *
 * Note: PRIVATE and UNKNOWN formats are vendor-specific and have no guarantees regarding memory
 * layouts or file formats.
 */
private fun FakeImage.createPrivatePlanes(): List<ImagePlane> {
    val emptyBuf = FakeByteBuffers.allocateNative(0)
    return listOf(
        FakeImagePlane(rowStride = width, rowCount = 0, pixelStride = 1, buffer = emptyBuf),
        FakeImagePlane(rowStride = width, rowCount = 0, pixelStride = 1, buffer = emptyBuf),
        FakeImagePlane(rowStride = width, rowCount = 0, pixelStride = 1, buffer = emptyBuf),
    )
}
