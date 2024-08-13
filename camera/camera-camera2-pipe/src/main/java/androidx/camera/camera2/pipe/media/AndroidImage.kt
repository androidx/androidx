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

package androidx.camera.camera2.pipe.media

import android.media.Image
import androidx.camera.camera2.pipe.StreamFormat
import java.nio.ByteBuffer
import kotlin.reflect.KClass

/**
 * An [ImageWrapper] backed by an [Image].
 *
 * Note: [Image] is not thread-safe, so all interactions with the underlying properties must be
 * copied into local fields or guarded by a lock.
 */
public class AndroidImage(private val image: Image) : ImageWrapper {
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
        override fun <T : Any> unwrapAs(type: KClass<T>): T? =
            when (type) {
                Image.Plane::class -> imagePlane as T
                else -> null
            }
    }

    private val lock = Any()

    @Volatile private var _planes: List<ImagePlane>? = null

    // Copying out the contents of the Image means that this Image
    // implementation can be thread-safe (without requiring any locking)
    // and can have getters which do not throw a RuntimeException if
    // the underlying Image is closed.
    override val format: Int = image.format
    override val width: Int = image.width
    override val height: Int = image.height
    override val timestamp: Long = image.timestamp

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            Image::class -> image as T
            else -> null
        }

    override val planes: List<ImagePlane>
        get() = readPlanes()

    override fun toString(): String {
        // Image will be written as "Image-YUV_444_888w640h480-1234567890" with format, width,
        // height, and timestamp
        return "Image-${StreamFormat(format).name}-w${width}h$height-$timestamp"
    }

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
                        imagePlanes?.map { imagePlane -> Plane(imagePlane) } ?: emptyList()
                    _planes = wrappedPlanes
                    result = wrappedPlanes
                }
            }
        }
        return result!!
    }
}
