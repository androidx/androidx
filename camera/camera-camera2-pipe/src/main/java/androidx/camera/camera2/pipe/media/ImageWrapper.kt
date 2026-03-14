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

import android.graphics.Rect
import android.hardware.HardwareBuffer
import androidx.camera.camera2.pipe.UnsafeWrapper
import java.nio.ByteBuffer

/**
 * Wrapper interfaces that mirrors the primary read-only properties of {@link android.media.Image}.
 */
public interface ImageWrapper : UnsafeWrapper, AutoCloseable {
    /** @see {@link android.media.Image.getWidth} */
    public val width: Int

    /** @see {@link android.media.Image.getHeight} */
    public val height: Int

    /** @see {@link android.media.Image.getFormat} */
    public val format: Int

    /** @see {@link android.media.Image.getPlanes} */
    public val planes: List<ImagePlane>

    /** @see {@link android.media.Image.getTimestamp} */
    public val timestamp: Long

    /** @see {@link android.media.Image.getCropRect} */
    public var cropRect: Rect

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

    /** @see {@link android.media.Image.getDataSpace} */
    public var dataSpace: Int?
        get() = null
        set(value) {}
}

public interface ImagePlane : UnsafeWrapper {
    /** @see {@link android.media.Image.Plane.getRowStride */
    public val rowStride: Int

    /** @see {@link android.media.Image.Plane.getPixelStride */
    public val pixelStride: Int

    /** @see {@link android.media.Image.Plane.getBuffer */
    public val buffer: ByteBuffer?
}
