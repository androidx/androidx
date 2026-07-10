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
import java.nio.ByteBuffer

/** Wrapper interfaces that mirrors the primary read-only properties of [android.media.Image]. */
public interface ImageWrapper : UnsafeWrapper, AutoCloseable {
    /** @see [android.media.Image.getWidth] */
    public val width: Int

    /** @see [android.media.Image.getHeight] */
    public val height: Int

    /** @see [android.media.Image.getFormat] */
    @get:ImageFormat public val format: Int

    /** @see [android.media.Image.getPlanes] */
    public val imagePlanes: List<ImagePlane>

    /** @see [android.media.Image.getTimestamp] */
    public val timestamp: Long

    /** @see [android.media.Image.getCropRect] */
    public val cropRect: Rect

    /**
     * Returns a handle to the underlying image's hardware buffer, or `null` if this image does not
     * support hardware buffer.
     *
     * The [android.hardware.HardwareBuffer] follows the lifecycle of its associated image. It is
     * not required to be closed explicitly; however, the image needs to be closed after finishing
     * processing the hardware buffer. In other words, if the hardware buffer is being used, the
     * image cannot be closed.
     *
     * @see [android.media.Image.getHardwareBuffer]
     */
    public val hardwareBuffer: HardwareBuffer?
        get() = null

    /** @see [android.media.Image.getFence] */
    public val syncFence: SyncFence?
        get() = null

    /** @see [android.media.Image.getDataSpace] */
    @get:SuppressLint("MethodNameUnits")
    @ImageDataSpace
    public val dataSpace: Int
        get() = DataSpace.DATASPACE_UNKNOWN
}

/** A mutable extension of [ImageWrapper] that includes write properties. */
public interface MutableImageWrapper : ImageWrapper {
    /** @see [android.media.Image.setCropRect] */
    override var cropRect: Rect

    /** @see [android.media.Image.setTimestamp] */
    override var timestamp: Long

    /** @see [android.media.Image.setFence] */
    override var syncFence: SyncFence?

    /** @see [android.media.Image.setDataSpace] */
    @get:SuppressLint("MethodNameUnits")
    @set:SuppressLint("MethodNameUnits")
    @get:ImageDataSpace
    @setparam:ImageDataSpace
    override var dataSpace: Int
}

public interface ImagePlane : UnsafeWrapper {
    /** @see [android.media.Image.Plane.getRowStride] */
    public val rowStride: Int

    /** @see [android.media.Image.Plane.getPixelStride] */
    public val pixelStride: Int

    /** @see [android.media.Image.Plane.getBuffer] */
    public val buffer: ByteBuffer?
}
