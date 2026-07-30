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

package androidx.camera.common

import android.annotation.SuppressLint
import android.graphics.Rect
import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.hardware.SyncFence
import android.media.Image
import java.nio.ByteBuffer

/**
 * A wrapper interface that mirrors the primary read-only properties of [Image].
 *
 * This wrapper abstracts the underlying platform image format to support common operations and ease
 * testing by allowing mocking.
 */
public interface ImageWrapper : UnsafeWrapper, AutoCloseable {
    /**
     * Closes the underlying image and releases any associated resources.
     *
     * Overridden to remove the checked [Exception] from the throws clause for Java callers.
     */
    override fun close()

    /**
     * The width of the image in pixels.
     *
     * @see Image.getWidth
     */
    public val width: Int

    /**
     * The height of the image in pixels.
     *
     * @see Image.getHeight
     */
    public val height: Int

    /**
     * The format of the image.
     *
     * The value returned is one of the constants defined in [ImageFormat].
     *
     * @see Image.getFormat
     * @see ImageFormat
     */
    @get:ImageFormat public val format: Int

    /**
     * The list of pixel planes for this image.
     *
     * The number of planes is determined by the format of the image. For example, YUV images
     * usually have three planes (Y, U, V), while RGB or JPEG images typically have a single plane.
     *
     * @see Image.getPlanes
     */
    public val imagePlanes: List<ImagePlane>

    /**
     * The timestamp associated with this image, in nanoseconds.
     *
     * @see Image.getTimestamp
     */
    public val timestamp: Long

    /**
     * The crop rectangle of the image.
     *
     * The crop rectangle defines the region of the image that contains valid pixel data. If not
     * set, it defaults to the full size of the image.
     *
     * @see Image.getCropRect
     */
    public val cropRect: Rect

    /**
     * The handle to the underlying image's hardware buffer, or `null` if this image does not
     * support hardware buffers.
     *
     * The [HardwareBuffer] follows the lifecycle of its associated [ImageWrapper]. It is not
     * required to be closed explicitly; however, the [ImageWrapper] must not be closed while the
     * hardware buffer is still in use.
     *
     * @see Image.getHardwareBuffer
     */
    public val hardwareBuffer: HardwareBuffer?
        get() = null

    /**
     * The sync fence associated with this image, or `null` if there is no fence.
     *
     * The sync fence is used to coordinate access to the image data, ensuring that the producer has
     * finished writing before the consumer starts reading, or vice versa.
     *
     * @see Image.getFence
     */
    public val syncFence: SyncFence?
        get() = null

    /**
     * The dataspace associated with this image.
     *
     * The dataspace defines how the color components of the image should be interpreted.
     *
     * The value returned is one of the constants defined in [ImageDataSpace].
     *
     * @see Image.getDataSpace
     * @see ImageDataSpace
     */
    @get:SuppressLint("MethodNameUnits")
    @ImageDataSpace
    public val dataSpace: Int
        get() = DataSpace.DATASPACE_UNKNOWN
}

/**
 * A mutable extension of [ImageWrapper] that allows modifying properties of the image.
 *
 * This interface is useful when modifying the metadata of an image, such as its crop rectangle,
 * timestamp, or synchronization fence.
 */
public interface MutableImageWrapper : ImageWrapper {
    /**
     * The crop rectangle of the image.
     *
     * @see Image.setCropRect
     */
    override var cropRect: Rect

    /**
     * The timestamp associated with this image, in nanoseconds.
     *
     * @see Image.setTimestamp
     */
    override var timestamp: Long

    /**
     * The sync fence associated with this image, or `null` if there is no fence.
     *
     * @see Image.setFence
     */
    override var syncFence: SyncFence?

    /**
     * The dataspace associated with this image.
     *
     * The value set must be one of the constants defined in [ImageDataSpace].
     *
     * @see Image.setDataSpace
     * @see ImageDataSpace
     */
    @get:SuppressLint("MethodNameUnits")
    @set:SuppressLint("MethodNameUnits")
    @get:ImageDataSpace
    @setparam:ImageDataSpace
    override var dataSpace: Int
}

/**
 * A wrapper interface that mirrors [Image.Plane].
 *
 * Represents a single color plane of image data.
 */
public interface ImagePlane : UnsafeWrapper {
    /**
     * The row stride, in bytes.
     *
     * This is the distance between the start of two consecutive rows of pixels in the image buffer.
     *
     * @see Image.Plane.getRowStride
     */
    public val rowStride: Int

    /**
     * The pixel stride, in bytes.
     *
     * This is the distance between two consecutive pixel values in a row of the image buffer.
     *
     * @see Image.Plane.getPixelStride
     */
    public val pixelStride: Int

    /**
     * A [ByteBuffer] containing the image data for this plane.
     *
     * The buffer's position will be set to the start of the plane data, and its limit will be set
     * to the end of the plane data.
     *
     * @see Image.Plane.getBuffer
     */
    public val buffer: ByteBuffer
}

/** Utility methods for [ImageWrapper]. */
public object ImageWrappers {
    /** Wraps a native [Image] into a [MutableImageWrapper]. */
    @JvmStatic
    @JvmName("wrap")
    @Suppress("INAPPLICABLE_JVM_NAME")
    public fun wrap(image: Image): MutableImageWrapper {
        return AndroidImage(image)
    }
}
