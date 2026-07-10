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
import android.hardware.DataSpace
import android.hardware.HardwareBuffer
import android.hardware.SyncFence
import androidx.camera.common.ImageDataSpace
import androidx.camera.common.ImagePlane
import androidx.camera.common.ImageWrapper as CommonImageWrapper
import androidx.camera.common.UnsafeWrapper

/**
 * Wrapper interfaces that mirrors the primary read-only properties of {@link android.media.Image}.
 */
public interface ImageWrapper : CommonImageWrapper, UnsafeWrapper, AutoCloseable {
    /** @see {@link android.media.Image.getWidth} */
    override val width: Int

    /** @see {@link android.media.Image.getHeight} */
    override val height: Int

    /** @see {@link android.media.Image.getFormat} */
    override val format: Int

    /** @see {@link android.media.Image.getPlanes} */
    @get:Deprecated("Use imagePlanes instead", ReplaceWith("imagePlanes"))
    public val planes: List<ImagePlane>
        get() = imagePlanes

    override val imagePlanes: List<ImagePlane>

    /** @see {@link android.media.Image.getTimestamp} */
    override val timestamp: Long

    /** @see {@link android.media.Image.getCropRect} */
    override var cropRect: Rect

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
    override val hardwareBuffer: HardwareBuffer?
        get() = null

    /** @see {@link android.media.Image.getFence} */
    override val syncFence: SyncFence?
        get() = null

    /** @see {@link android.media.Image.getDataSpace} */
    @get:ImageDataSpace
    @setparam:ImageDataSpace
    override var dataSpace: Int
        get() = DataSpace.DATASPACE_UNKNOWN
        set(value) {
            // No-op by default in the interface
        }
}
