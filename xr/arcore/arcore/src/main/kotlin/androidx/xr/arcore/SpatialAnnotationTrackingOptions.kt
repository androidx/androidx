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

package androidx.xr.arcore

import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Quad
import java.nio.ByteBuffer

/**
 * Configuration options required to start tracking Spatial Annotations.
 *
 * Use the nested [Builder] to create an instance.
 *
 * @param imageBuffer the [ByteBuffer] containing the input image data
 * @param imageSize the [IntSize2d] of the input image
 * @param rowStride the row stride of the input image in bytes
 * @param format the format of the input image
 * @param alignment the requested tracking alignment
 * @param quads a map of [SpatialAnnotationId]s to bounding quads
 * @param timestampNanos the system timestamp of the input image in nanoseconds, using the
 *   [System.nanoTime] clock base
 */
@ExperimentalSpatialAnnotationsApi
public class SpatialAnnotationTrackingOptions
private constructor(
    public val imageBuffer: ByteBuffer,
    public val imageSize: IntSize2d,
    public val rowStride: Int,
    public val format: SpatialAnnotationImageFormat,
    public val alignment: SpatialAnnotationQuadAlignment,
    public val quads: Map<SpatialAnnotationId, Quad>,
    public val timestampNanos: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpatialAnnotationTrackingOptions) return false

        if (imageBuffer !== other.imageBuffer) return false
        if (imageSize != other.imageSize) return false
        if (rowStride != other.rowStride) return false
        if (format != other.format) return false
        if (alignment != other.alignment) return false
        if (quads != other.quads) return false
        if (timestampNanos != other.timestampNanos) return false

        return true
    }

    override fun hashCode(): Int {
        var result = System.identityHashCode(imageBuffer)
        result = 31 * result + imageSize.hashCode()
        result = 31 * result + rowStride
        result = 31 * result + format.hashCode()
        result = 31 * result + alignment.hashCode()
        result = 31 * result + quads.hashCode()
        result = 31 * result + timestampNanos.hashCode()
        return result
    }

    override fun toString(): String {
        return "SpatialAnnotationTrackingOptions(" +
            "imageSize=$imageSize, " +
            "rowStride=$rowStride, " +
            "format=$format, " +
            "alignment=$alignment, " +
            "quads=$quads, " +
            "timestampNanos=$timestampNanos)"
    }

    /**
     * Builder for [SpatialAnnotationTrackingOptions].
     *
     * @param imageBuffer the raw image byte buffer from the camera frame
     * @param imageSize the 2D pixel dimensions of the image buffer
     * @param timestampNanos the system timestamp in nanoseconds correlating to the camera frame,
     *   using the [System.nanoTime] clock base
     */
    public class Builder(imageBuffer: ByteBuffer, imageSize: IntSize2d, timestampNanos: Long) {
        private var imageBuffer: ByteBuffer = imageBuffer
        private var imageSize: IntSize2d = imageSize
        private var timestampNanos: Long = timestampNanos
        private var rowStride: Int? = null
        private var quads: Map<SpatialAnnotationId, Quad> = emptyMap()
        private var format: SpatialAnnotationImageFormat = SpatialAnnotationImageFormat.RGBA
        private var alignment: SpatialAnnotationQuadAlignment =
            SpatialAnnotationQuadAlignment.SCREEN

        /** Sets the raw image byte buffer from the camera frame. */
        public fun setImageBuffer(imageBuffer: ByteBuffer): Builder = apply {
            this.imageBuffer = imageBuffer
        }

        /** Sets the 2D pixel dimensions of the image buffer. */
        public fun setImageSize(imageSize: IntSize2d): Builder = apply {
            this.imageSize = imageSize
        }

        // TODO(b/542273862): Understand how developers interact with rowStride and the
        // requirements.
        /** Sets the row stride (bytes per row) of the image buffer. */
        public fun setRowStride(rowStride: Int): Builder = apply { this.rowStride = rowStride }

        /**
         * Sets the system timestamp in nanoseconds correlating to the camera frame, using the
         * [System.nanoTime] clock base.
         */
        public fun setTimestampNanos(timestampNanos: Long): Builder = apply {
            this.timestampNanos = timestampNanos
        }

        /** Sets the bounding quadrilaterals mapped to their unique tracking IDs. */
        public fun setQuads(quads: Map<SpatialAnnotationId, Quad>): Builder = apply {
            this.quads = quads.toMap()
        }

        /** Sets the pixel format encoding. Defaults to RGBA. */
        public fun setFormat(format: SpatialAnnotationImageFormat): Builder = apply {
            this.format = format
        }

        /** Sets the physical alignment behavior of the resulting 3D quad. Defaults to SCREEN. */
        public fun setAlignment(alignment: SpatialAnnotationQuadAlignment): Builder = apply {
            this.alignment = alignment
        }

        /**
         * Builds a new [SpatialAnnotationTrackingOptions] instance.
         *
         * @throws IllegalStateException if required fields were not explicitly set
         */
        public fun build(): SpatialAnnotationTrackingOptions {
            val buffer = imageBuffer
            val size = imageSize
            val w = size.width
            val h = size.height
            val timestamp = timestampNanos

            check(w > 0 && h > 0) { "Width and height must be strictly positive." }
            check(quads.isNotEmpty()) { "At least one Quad must be provided to start tracking." }

            val bytesPerPixel =
                when (format.value) {
                    SpatialAnnotationImageFormat.RGBA.value -> 4
                    SpatialAnnotationImageFormat.GRAYSCALE.value -> 1
                    else -> throw IllegalArgumentException("Unknown format: $format")
                }

            val stride = rowStride ?: (w * bytesPerPixel)

            check(stride >= w * bytesPerPixel) {
                "Row stride ($stride) must be at least width ($w) * bytesPerPixel ($bytesPerPixel)."
            }

            val minBufferSize = stride * (h - 1) + w * bytesPerPixel
            check(buffer.remaining() >= minBufferSize) {
                "Image buffer remaining size (${buffer.remaining()}) must be at least $minBufferSize bytes."
            }

            return SpatialAnnotationTrackingOptions(
                imageBuffer = buffer,
                imageSize = size,
                rowStride = stride,
                format = format,
                alignment = alignment,
                quads = quads,
                timestampNanos = timestamp,
            )
        }
    }
}
