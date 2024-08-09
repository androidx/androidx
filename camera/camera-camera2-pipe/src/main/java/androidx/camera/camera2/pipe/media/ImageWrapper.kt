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
}

public interface ImagePlane : UnsafeWrapper {
    /** @see {@link android.media.Image.Plane.getRowStride */
    public val rowStride: Int

    /** @see {@link android.media.Image.Plane.getPixelStride */
    public val pixelStride: Int

    /** @see {@link android.media.Image.Plane.getBuffer */
    public val buffer: ByteBuffer?
}
