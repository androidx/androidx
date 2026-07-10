/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.video.internal.encoder

import android.util.Range

/** VideoEncoderInfo provides video encoder related information and capabilities. */
public interface VideoEncoderInfo : EncoderInfo {
    /** Return if the supported width height can be swapped. */
    public fun canSwapWidthHeight(): Boolean

    /** Returns if the size is supported. */
    public fun isSizeSupported(width: Int, height: Int): Boolean

    /**
     * Returns if the size is supported when the width height is allowed swapping.
     *
     * This is basically equivalent to
     *
     * `isSizeSupport(width, height) || (canSwapWidthHeight() && isSizeSupported(height, width))`
     */
    public fun isSizeSupportedAllowSwapping(width: Int, height: Int): Boolean {
        return isSizeSupported(width, height) ||
            (canSwapWidthHeight() && isSizeSupported(height, width))
    }

    /** Returns the range of supported video widths. */
    public fun getSupportedWidths(): Range<Int>

    /** Returns the range of supported video heights. */
    public fun getSupportedHeights(): Range<Int>

    /**
     * Returns the range of supported video widths for a video height.
     *
     * @throws IllegalArgumentException if height is not supported.
     * @see getSupportedHeights
     * @see heightAlignment
     */
    public fun getSupportedWidthsFor(height: Int): Range<Int>

    /**
     * Returns the range of supported video heights for a video width.
     *
     * @throws IllegalArgumentException if width is not supported.
     * @see getSupportedWidths
     * @see widthAlignment
     */
    public fun getSupportedHeightsFor(width: Int): Range<Int>

    /**
     * Returns the alignment requirement for video width (in pixels).
     *
     * This is usually a power-of-2 value that video width must be a multiple of.
     */
    public val widthAlignment: Int

    /**
     * Returns the alignment requirement for video height (in pixels).
     *
     * This is usually a power-of-2 value that video height must be a multiple of.
     */
    public val heightAlignment: Int

    /** Returns the video encoder's bitrate range. */
    public val supportedBitrateRange: Range<Int>

    /** A finder that can find a [VideoEncoderInfo]. */
    public fun interface Finder {
        /** Finds a [VideoEncoderInfo] for the given MIME type. */
        public fun find(mimeType: String): VideoEncoderInfo?
    }
}
