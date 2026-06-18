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
import androidx.camera.common.ImagePlane
import androidx.camera.common.MutableImageWrapper
import androidx.camera.common.testing.FakeByteBuffers.sliceNative
import java.lang.Class
import java.nio.ByteBuffer
import kotlinx.atomicfu.atomic

/**
 * Fake implementation of [MutableImageWrapper] for testing.
 *
 * This class supports optional injection of both a [ByteBuffer] and a [HardwareBuffer].
 *
 * ## HardwareBuffer & Backing Behavior
 * If a `hardwareBuffer` is provided, its width, height, and format metadata will be verified and
 * accessible. However, accessing pixel data directly from within the `HardwareBuffer` is not
 * supported on pure JVM. Accessing the buffer fields from [imagePlanes] will use data backed by the
 * `byteBuffer` parameter and/or `byteBuffer` field on this image.
 *
 * If provided, the `HardwareBuffer` will be closed when this [FakeImage] is closed.
 */
public class FakeImage
@JvmOverloads
constructor(
    override val width: Int,
    override val height: Int,
    @ImageFormat override val format: Int,
    override var timestamp: Long,
    byteBuffer: ByteBuffer? = null,
    hardwareBuffer: HardwareBuffer? = null,
    override var cropRect: Rect = Rect(0, 0, width, height),
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

    public val isClosed: Boolean
        get() = _closeCount.value > 0

    public val closeCount: Int
        get() = _closeCount.value

    override var syncFence: SyncFence? = null

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

    override val hardwareBuffer: HardwareBuffer?
        get() = lazyHardwareBuffer.value

    override val imagePlanes: List<ImagePlane> by lazy {
        check(!isClosed)
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
        when (format) {
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

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == ByteBuffer::class.java -> lazyByteBuffer as T?
            Build.VERSION.SDK_INT >= 26 && type == HardwareBuffer::class.java ->
                hardwareBuffer as T?
            else -> null
        }

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

    override fun toString(): String = "FakeImage-$debugId-$format-w${width}h$height-t$timestamp"

    private companion object {
        private val debugIds = atomic(0)
    }
}

private fun FakeImage.estimateMinimumByteBufferSize(): Int? =
    when (format) {
        GraphicsImageFormat.YUV_420_888,
        GraphicsImageFormat.NV21 -> (width * height * 3) / 2
        GraphicsImageFormat.JPEG,
        GraphicsImageFormat.HEIC,
        GraphicsImageFormat.DEPTH_JPEG,
        GraphicsImageFormat.JPEG_R,
        GraphicsImageFormat.DEPTH_POINT_CLOUD ->
            FakeHardwareBuffers.estimateBlobBufferSize(width, height)
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
        FakeImagePlane(rowStride = width, rowCount = height / 2, pixelStride = 2, buffer = p2Buffer),
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
