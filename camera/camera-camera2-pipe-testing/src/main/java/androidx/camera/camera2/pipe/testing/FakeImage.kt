/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.testing

import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.camera.camera2.pipe.media.ImagePlane
import androidx.camera.camera2.pipe.media.ImageWrapper
import java.lang.Class
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.atomicfu.atomic

/** FakeImage that can be used for testing classes that accept [ImageWrapper]. */
public class FakeImage(
    override val width: Int,
    override val height: Int,
    override val format: Int,
    override val timestamp: Long,
    private val providedHardwareBuffer: HardwareBuffer? = null,
    override var cropRect: Rect = Rect(0, 0, width, height),
) : ImageWrapper {
    private val debugId = debugIds.incrementAndGet()
    private val closed = atomic(false)
    public val isClosed: Boolean
        get() = closed.value

    override val hardwareBuffer: HardwareBuffer? by lazy {
        // Create default hardware buffer only after API 34
        if (
            providedHardwareBuffer == null &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        ) {
            HardwareBuffer.create(width, height, format, 1, HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE)
        }
        providedHardwareBuffer
    }
    public var numberOfTimesClosed: Int = 0
        private set

    override val planes: List<ImagePlane> by lazy {
        // TODO(b/507590815): Support other formats as needed
        if (format == ImageFormat.YUV_420_888) {
            listOf(
                FakeImagePlane(pixelStride = 1, rowStride = width, planeHeight = height),
                FakeImagePlane(pixelStride = 2, rowStride = width, planeHeight = height / 2),
                FakeImagePlane(pixelStride = 2, rowStride = width, planeHeight = height / 2),
            )
        } else {
            listOf()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? {
        if (
            Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1 && type == HardwareBuffer::class.java
        ) {
            return hardwareBuffer as T?
        }
        return null
    }

    override fun close() {
        numberOfTimesClosed++
        if (closed.compareAndSet(expect = false, update = true)) {
            if (Build.VERSION.SDK_INT > VERSION_CODES.UPSIDE_DOWN_CAKE) {
                hardwareBuffer?.close()
            }
        }
    }

    override fun toString(): String = "FakeImage-$debugId"

    public companion object {
        private val debugIds = atomic(0)
    }
}

public class FakeImagePlane : ImagePlane {
    override val pixelStride: Int
    override val rowStride: Int
    override val buffer: ByteBuffer

    public constructor(
        planeWidth: Int,
        planeHeight: Int,
    ) : this(pixelStride = 1, rowStride = planeWidth, planeHeight = planeHeight)

    public constructor(pixelStride: Int, rowStride: Int, planeHeight: Int) {
        this.pixelStride = pixelStride
        this.rowStride = rowStride
        this.buffer =
            ByteBuffer.allocateDirect(rowStride * planeHeight).order(ByteOrder.nativeOrder())
    }

    public constructor(buffer: ByteBuffer, pixelStride: Int, rowStride: Int) {
        this.pixelStride = pixelStride
        this.rowStride = rowStride
        this.buffer = buffer
    }

    override fun <T : Any> unwrapAs(type: Class<T>): T? = null
}
