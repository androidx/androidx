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

/** A VideoEncoderInfo wrapper that swaps the width and height constraints internally. */
public class SwappedVideoEncoderInfo(private val videoEncoderInfo: VideoEncoderInfo) :
    VideoEncoderInfo by videoEncoderInfo {

    init {
        require(videoEncoderInfo.canSwapWidthHeight())
    }

    override fun isSizeSupported(width: Int, height: Int): Boolean {
        return videoEncoderInfo.isSizeSupported(height, width)
    }

    override fun isSizeSupportedAllowSwapping(width: Int, height: Int): Boolean {
        return videoEncoderInfo.isSizeSupportedAllowSwapping(height, width)
    }

    override fun getSupportedWidths(): Range<Int> {
        return videoEncoderInfo.getSupportedHeights()
    }

    override fun getSupportedHeights(): Range<Int> {
        return videoEncoderInfo.getSupportedWidths()
    }

    override fun getSupportedWidthsFor(height: Int): Range<Int> {
        return videoEncoderInfo.getSupportedHeightsFor(height)
    }

    override fun getSupportedHeightsFor(width: Int): Range<Int> {
        return videoEncoderInfo.getSupportedWidthsFor(width)
    }

    override val widthAlignment: Int
        get() = videoEncoderInfo.heightAlignment

    override val heightAlignment: Int
        get() = videoEncoderInfo.widthAlignment
}
