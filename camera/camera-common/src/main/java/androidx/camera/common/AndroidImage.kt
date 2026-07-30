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
import androidx.annotation.RestrictTo
import androidx.camera.common.compat.Api28Compat
import androidx.camera.common.compat.Api33Compat
import java.lang.Class
import java.nio.ByteBuffer

/**
 * An [ImageWrapper] implementation that wraps an [android.media.Image] object.
 *
 * This wrapper provides thread-safe access to the underlying image properties. Immutable or
 * structural properties (such as [format], [width], [height], and plane data like strides and
 * buffers) are cached or extracted during initialization, allowing them to be read in a
 * thread-safe, lock-free manner. These properties also do not throw an [IllegalStateException] if
 * read after [close] has been called.
 *
 * Other properties (such as [timestamp], [cropRect], [dataSpace], and [syncFence]) are lazily
 * cached and synchronized using an internal lock. Accessing these properties after the underlying
 * image has been closed may throw an [IllegalStateException] if they were not cached prior to
 * closing.
 *
 * @param image The underlying [android.media.Image] to wrap.
 */
@SuppressLint("AutoBoxing")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class AndroidImage(private val image: Image) : MutableImageWrapper {
    private val lock = Any()

    /**
     * An [ImagePlane] implementation backed by an [android.media.Image.Plane].
     *
     * This class extracts and caches the [pixelStride], [rowStride], and [buffer] from the wrapped
     * [android.media.Image.Plane] at instantiation time. As a result, access to these properties is
     * thread-safe, lock-free, and will not throw an [IllegalStateException] if the parent
     * [AndroidImage] is closed.
     *
     * @param imagePlane The underlying [android.media.Image.Plane] to wrap.
     */
    public class Plane(private val imagePlane: Image.Plane) : ImagePlane {
        // Copying out the contents of the Image.Plane means that this Plane
        // implementation can be thread-safe (without requiring any locking)
        // and can have getters which do not throw a RuntimeException if
        // the underlying Image is closed.
        override val pixelStride: Int = imagePlane.pixelStride
        override val rowStride: Int = imagePlane.rowStride
        override val buffer: ByteBuffer = imagePlane.buffer

        /**
         * Unwraps this plane as the requested class type if possible.
         *
         * Supported types:
         * - [Plane] (this instance)
         * - [android.media.Image.Plane] (the wrapped image plane)
         *
         * @param type The [Class] object representing the target type.
         * @return The unwrapped plane instance of type [T], or `null` if the plane cannot be
         *   unwrapped as the requested type.
         */
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
    /**
     * The image format.
     *
     * This property is cached at instantiation time. It is thread-safe and will not throw if
     * accessed after [close] has been called.
     *
     * @see android.media.Image.getFormat
     */
    override val format: Int = image.format

    /**
     * The image width in pixels.
     *
     * This property is cached at instantiation time. It is thread-safe and will not throw if
     * accessed after [close] has been called.
     *
     * @see android.media.Image.getWidth
     */
    override val width: Int = image.width

    /**
     * The image height in pixels.
     *
     * This property is cached at instantiation time. It is thread-safe and will not throw if
     * accessed after [close] has been called.
     *
     * @see android.media.Image.getHeight
     */
    override val height: Int = image.height

    /**
     * The presentation timestamp associated with the image, in nanoseconds.
     *
     * Access to this property is synchronized. Setting this value will update both the underlying
     * image and the local cache. If the underlying image is closed, accessing this property may
     * throw an [IllegalStateException] if it was not previously cached.
     *
     * @see android.media.Image.getTimestamp
     * @see android.media.Image.setTimestamp
     */
    override var timestamp: Long
        get() = synchronized(lock) { _timestamp ?: image.timestamp.also { _timestamp = it } }
        set(value) =
            synchronized(lock) {
                image.timestamp = value
                _timestamp = value
            }

    /**
     * The crop rectangle specifying the valid sensor pixels for the image.
     *
     * Access to this property is synchronized. Setting this value will update both the underlying
     * image and the local cache. If the underlying image is closed, accessing this property may
     * throw an [IllegalStateException] if it was not previously cached.
     *
     * @see android.media.Image.getCropRect
     * @see android.media.Image.setCropRect
     */
    override var cropRect: Rect
        get() = synchronized(lock) { _cropRect ?: image.cropRect.also { _cropRect = it } }
        set(newRectValue: Rect) =
            synchronized(lock) {
                image.cropRect = newRectValue
                _cropRect = newRectValue
            }

    /**
     * The synchronization fence associated with this image.
     *
     * This property is only supported on Android T (API 33) and above. For older API versions,
     * getting will return `null` and setting will be a no-op. Access to this property is
     * synchronized.
     *
     * @see android.media.Image.getFence
     * @see android.media.Image.setFence
     */
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

    /**
     * The color space/dataspace associated with the image.
     *
     * This property is only supported on Android T (API 33) and above. For older API versions,
     * getting will return [android.hardware.DataSpace.DATASPACE_UNKNOWN] and setting will be a
     * no-op. Access to this property is synchronized. If the underlying image is closed, accessing
     * this property on API 33+ may throw an [IllegalStateException] if it was not previously
     * cached.
     *
     * @see android.media.Image.getDataSpace
     * @see android.media.Image.setDataSpace
     */
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

    /**
     * The hardware buffer associated with this image, or `null` if hardware buffers are not
     * supported.
     *
     * This property is only supported on Android P (API 28) and above. For older API versions, it
     * returns `null`. Access to this property is synchronized.
     *
     * @see android.media.Image.getHardwareBuffer
     */
    override val hardwareBuffer: HardwareBuffer?
        get() =
            if (Build.VERSION.SDK_INT >= 28) {
                synchronized(lock) { Api28Compat.getHardwareBuffer(image) }
            } else {
                null
            }

    /**
     * Unwraps this image wrapper as the requested class type if possible.
     *
     * Supported types:
     * - [AndroidImage] (this instance)
     * - [android.media.Image] (the wrapped image)
     * - [android.hardware.HardwareBuffer] (the underlying hardware buffer, on API 28+)
     *
     * @param type The [Class] object representing the target type.
     * @return The unwrapped image instance of type [T], or `null` if the image cannot be unwrapped
     *   as the requested type.
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: Class<T>): T? =
        when {
            type.isInstance(this) -> this as T
            type == Image::class.java -> image as T
            Build.VERSION.SDK_INT >= 28 && type == HardwareBuffer::class.java ->
                Api28Compat.getHardwareBuffer(image) as T?
            else -> null
        }

    /**
     * The list of image planes for this image.
     *
     * The planes are lazily initialized and cached on the first access. Access to this property is
     * synchronized. If the underlying image is closed before this property has been accessed,
     * reading it will throw an [IllegalStateException].
     *
     * @see android.media.Image.getPlanes
     */
    override val imagePlanes: List<ImagePlane>
        get() = readPlanes()

    override fun toString(): String =
        "Image-${ImageFormats.name(format)}-w${width}h$height-t$timestamp"

    /**
     * Closes the underlying [android.media.Image].
     *
     * @see android.media.Image.close
     */
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
