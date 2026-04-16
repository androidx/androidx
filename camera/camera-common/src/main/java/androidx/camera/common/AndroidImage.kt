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

@file:SuppressLint("AutoBoxing")

package androidx.camera.common

import android.annotation.SuppressLint
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.hardware.SyncFence
import android.media.Image
import android.os.Build
import androidx.camera.common.compat.Api28Compat
import androidx.camera.common.compat.Api33Compat
import java.lang.Class
import java.nio.ByteBuffer

/** [ImageWrapper] implementation that wraps an [android.media.Image] object. */
@SuppressLint("AutoBoxing")
public final class AndroidImage(private val image: Image) : MutableImageWrapper {
    private val lock = Any()

    /** A [Plane] backed by an [ImagePlane]. */
    public class Plane(private val imagePlane: Image.Plane) : ImagePlane {
        // Copying out the contents of the Image.Plane means that this Plane
        // implementation can be thread-safe (without requiring any locking)
        // and can have getters which do not throw a RuntimeException if
        // the underlying Image is closed.
        override val pixelStride: Int = imagePlane.pixelStride
        override val rowStride: Int = imagePlane.rowStride
        override val buffer: ByteBuffer = imagePlane.buffer

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> unwrapAs(type: Class<T>): T? =
            when {
                type.isInstance(this) -> this as T
                type == Image.Plane::class.java -> imagePlane as T
                else -> null
            }
    }

    private var _timestamp: Long? = null
    private var _cropRect: Rect? = null
    private var _dataSpace: Int? = null

    @Volatile private var _planes: List<ImagePlane>? = null

    // Copying out the contents of the Image means that this Image
    // implementation can be thread-safe (without requiring any locking)
    // and can have getters which do not throw a RuntimeException if
    // the underlying Image is closed.
    override val format: Int = image.format
    override val width: Int = image.width
    override val height: Int = image.height

    override var timestamp: Long
        get() = synchronized(lock) { _timestamp ?: image.timestamp.also { _timestamp = it } }
        set(value) =
            synchronized(lock) {
                image.timestamp = value
                _timestamp = value
            }

    override var cropRect: Rect
        get() = synchronized(lock) { _cropRect ?: image.cropRect.also { _cropRect = it } }
        set(newRectValue: Rect) =
            synchronized(lock) {
                image.cropRect = newRectValue
                _cropRect = newRectValue
            }

    override var syncFence: SyncFence?
        get() =
            if (Build.VERSION.SDK_INT >= 33) {
                synchronized(lock) { Api33Compat.getFence(image) }
            } else {
                null
            }
        set(value) {
            if (Build.VERSION.SDK_INT >= 33 && value != null) {
                synchronized(lock) { Api33Compat.setFence(image, value) }
            }
        }

    @get:ImageDataSpace
    @setparam:ImageDataSpace
    override var dataSpace: Int
        get() =
            if (Build.VERSION.SDK_INT >= 33) {
                synchronized(lock) {
                    _dataSpace ?: Api33Compat.getDataSpace(image).also { _dataSpace = it }
                }
            } else {
                android.hardware.DataSpace.DATASPACE_UNKNOWN
            }
        set(value) {
            if (Build.VERSION.SDK_INT >= 33) {
                synchronized(lock) {
                    Api33Compat.setDataSpace(image, value)
                    _dataSpace = value
                }
            }
        }

    override val hardwareBuffer: HardwareBuffer?
        get() =
            if (Build.VERSION.SDK_INT >= 28) {
                synchronized(lock) { Api28Compat.getHardwareBuffer(image) }
            } else {
                null
            }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == Image::class.java -> image as T
            Build.VERSION.SDK_INT >= 28 && type == HardwareBuffer::class.java ->
                Api28Compat.getHardwareBuffer(image) as T?
            else -> null
        }

    override val imagePlanes: List<ImagePlane>
        get() = readPlanes()

    override fun toString(): String = "Image-$format-w${width}h$height-t$timestamp"

    override fun close() {
        image.close()
    }

    /**
     * Read and cache the result of [Image.getPlanes]. Each [ImagePlane], in turn, reads out and
     * caches the buffer data for that specific plane.
     *
     * @return a list of [ImagePlane]
     */
    private fun readPlanes(): List<ImagePlane> {
        var result = _planes
        if (result == null) {
            // Double checked locking for reading planes with a fast volatile read.
            synchronized(lock) {
                result = _planes
                if (result == null) {
                    val imagePlanes = image.planes
                    val wrappedPlanes =
                        imagePlanes?.map { imagePlane -> Plane(imagePlane) as ImagePlane }
                            ?: emptyList()
                    _planes = wrappedPlanes
                    result = wrappedPlanes
                }
            }
        }
        return result!!
    }
}
