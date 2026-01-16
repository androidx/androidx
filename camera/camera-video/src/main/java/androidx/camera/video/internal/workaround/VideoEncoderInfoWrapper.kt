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
package androidx.camera.video.internal.workaround

import android.media.MediaCodecInfo
import android.util.Range
import android.util.Size
import androidx.camera.core.Logger
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaCodecInfoReportIncorrectInfoQuirk
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import kotlin.math.ceil

/**
 * Workaround to wrap the VideoEncoderInfo in order to fix the wrong information provided by
 * [MediaCodecInfo].
 *
 * One use case is VideoCapture resizing the crop to a size valid for the encoder.
 *
 * @see MediaCodecInfoReportIncorrectInfoQuirk
 */
public class VideoEncoderInfoWrapper
private constructor(private val videoEncoderInfo: VideoEncoderInfo) : VideoEncoderInfo {
    private val _supportedWidths: Range<Int>
    private val _supportedHeights: Range<Int>

    // Extra supported sizes is used to put resolutions that are actually supported on the device
    // but the MediaCodecInfo indicates the resolution is invalid. The most common one is
    // 1920x1080. For resolutions in this set, #isSizeSupported(w, h) should return true.
    private val extraSupportedSizes: MutableSet<Size> = HashSet()

    init {
        // Ideally we should find out supported widths/heights for each problematic device.
        // As a workaround, simply return a big enough size for video encoding. i.e.
        // CamcorderProfile.QUALITY_4KDCI. The size still need to follow the multiple of alignment.
        val widthAlignment = videoEncoderInfo.widthAlignment
        val maxWidth = ceil(WIDTH_4KDCI.toDouble() / widthAlignment).toInt() * widthAlignment
        _supportedWidths = Range.create(widthAlignment, maxWidth)
        val heightAlignment = videoEncoderInfo.heightAlignment
        val maxHeight = ceil(HEIGHT_4KDCI.toDouble() / heightAlignment).toInt() * heightAlignment
        _supportedHeights = Range.create(heightAlignment, maxHeight)
        extraSupportedSizes.addAll(MediaCodecInfoReportIncorrectInfoQuirk.getExtraSupportedSizes())
    }

    override fun getName(): String {
        return videoEncoderInfo.getName()
    }

    override fun canSwapWidthHeight(): Boolean {
        return videoEncoderInfo.canSwapWidthHeight()
    }

    override fun isSizeSupported(width: Int, height: Int): Boolean {
        if (videoEncoderInfo.isSizeSupported(width, height)) {
            return true
        }
        if (extraSupportedSizes.any { it.width == width && it.height == height }) {
            return true
        }
        return _supportedWidths.contains(width) &&
            _supportedHeights.contains(height) &&
            width % videoEncoderInfo.widthAlignment == 0 &&
            height % videoEncoderInfo.heightAlignment == 0
    }

    override fun getSupportedWidths(): Range<Int> {
        return _supportedWidths
    }

    override fun getSupportedHeights(): Range<Int> {
        return _supportedHeights
    }

    override fun getSupportedWidthsFor(height: Int): Range<Int> {
        require(
            _supportedHeights.contains(height) && height % videoEncoderInfo.heightAlignment == 0
        ) {
            "Not supported height: $height which is not in $_supportedHeights" +
                " or can not be divided by alignment ${videoEncoderInfo.heightAlignment}"
        }
        return _supportedWidths
    }

    override fun getSupportedHeightsFor(width: Int): Range<Int> {
        require(_supportedWidths.contains(width) && width % videoEncoderInfo.widthAlignment == 0) {
            "Not supported width: $width which is not in $_supportedWidths" +
                " or can not be divided by alignment ${videoEncoderInfo.widthAlignment}"
        }
        return _supportedHeights
    }

    override val widthAlignment: Int
        get() = videoEncoderInfo.widthAlignment

    override val heightAlignment: Int
        get() = videoEncoderInfo.heightAlignment

    override val supportedBitrateRange: Range<Int>
        get() = videoEncoderInfo.supportedBitrateRange

    private fun addExtraSupportedSize(size: Size) {
        extraSupportedSizes.add(size)
    }

    public companion object {
        private const val TAG = "VideoEncoderInfoWrapper"

        // The resolution of CamcorderProfile.QUALITY_4KDCI
        private const val WIDTH_4KDCI = 4096
        private const val HEIGHT_4KDCI = 2160

        /**
         * Check and wrap an input VideoEncoderInfo
         *
         * The input VideoEncoderInfo will be wrapped when
         * * The device is a quirk device determined in [MediaCodecInfoReportIncorrectInfoQuirk].
         * * The input `validSizeToCheck` is not supported by input VideoEncoderInfo.
         *
         * Otherwise, the input VideoEncoderInfo will be returned.
         *
         * Exception: if the input videoEncoderInfo is already a wrapper, then it will not be
         * wrapped again and will be returned directly.
         *
         * The `validSizeToCheck` will be taken as an extra supported size if this method returns a
         * wrapper.
         *
         * @param videoEncoderInfo the input VideoEncoderInfo.
         * @param validSizeToCheck a valid size to check or null if no valid size to check.
         * @return a wrapped VideoEncoderInfo or the input VideoEncoderInfo.
         */
        @JvmStatic
        public fun from(
            videoEncoderInfo: VideoEncoderInfo,
            validSizeToCheck: Size?,
        ): VideoEncoderInfo {
            var videoEncoderInfo = videoEncoderInfo
            val toWrap: Boolean =
                if (videoEncoderInfo is VideoEncoderInfoWrapper) {
                    false
                } else if (
                    DeviceQuirks.get(MediaCodecInfoReportIncorrectInfoQuirk::class.java) != null
                ) {
                    true
                } else if (
                    validSizeToCheck != null &&
                        !videoEncoderInfo.isSizeSupportedAllowSwapping(
                            validSizeToCheck.width,
                            validSizeToCheck.height,
                        )
                ) {
                    // If the device does not support a size that should be valid, assume the device
                    // reports incorrect information. This is used to detect devices that we haven't
                    // discovered incorrect information yet.
                    Logger.w(
                        TAG,
                        "Detected that the device does not support a size $validSizeToCheck that should be valid" +
                            " in widths/heights = ${videoEncoderInfo.getSupportedWidths()}/${videoEncoderInfo.getSupportedHeights()}",
                    )
                    true
                } else {
                    false
                }
            if (toWrap) {
                videoEncoderInfo = VideoEncoderInfoWrapper(videoEncoderInfo)
            }
            if (validSizeToCheck != null && videoEncoderInfo is VideoEncoderInfoWrapper) {
                videoEncoderInfo.addExtraSupportedSize(validSizeToCheck)
            }
            return videoEncoderInfo
        }
    }
}
