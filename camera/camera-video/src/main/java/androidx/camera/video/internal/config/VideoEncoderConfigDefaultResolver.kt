/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.camera.video.internal.config

import android.media.MediaFormat
import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.Timebase
import androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_720P
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.utils.DynamicRangeUtil
import androidx.core.util.Supplier

/**
 * A [VideoEncoderConfig] supplier that resolves requested encoder settings from a [VideoSpec] for
 * the given surface [Size] using pre-defined default values.
 */
public class VideoEncoderConfigDefaultResolver
/**
 * Constructor for a VideoEncoderConfigDefaultResolver.
 *
 * @param mimeType The mime type for the video encoder
 * @param inputTimebase The time base of the input frame.
 * @param videoSpec The [VideoSpec] which defines the settings that should be used with the video
 *   encoder.
 * @param surfaceSize The size of the surface required by the camera for the video encoder.
 * @param dynamicRange The dynamic range of input frames.
 * @param expectedFrameRateRange The expected source frame rate range. This should act as an
 *   envelope for any frame rate calculated from `videoSpec ` * and `videoProfile` since the source
 *   should not produce frames at a frame rate outside this range. If equal to
 *   [SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED], then no information about the source frame rate
 *   is available and it does not need to be used in calculations.
 */
public constructor(
    private val mimeType: String,
    private val inputTimebase: Timebase,
    private val videoSpec: VideoSpec,
    private val surfaceSize: Size,
    private val dynamicRange: DynamicRange,
    private val expectedFrameRateRange: Range<Int>,
) : Supplier<VideoEncoderConfig> {

    public companion object {
        private const val TAG = "VidEncCfgDefaultRslvr"

        private const val VIDEO_FRAME_RATE_BASE = 30
        private const val VIDEO_BIT_DEPTH_BASE = 8

        /**
         * Baseline video encoder configuration derived from Android CDD §5.2 specifications.
         *
         * @param bitrate Base video bitrate in bits per second.
         * @param size Canonical reference resolution (default: 720p).
         * @param frameRate Canonical reference frame rate in frames per second (default: 30 fps).
         * @param bitDepth Canonical reference bit depth (default: 8-bit).
         */
        private data class BaseVideoConfig(
            val bitrate: Int,
            val size: Size = RESOLUTION_720P,
            val frameRate: Int = VIDEO_FRAME_RATE_BASE,
            val bitDepth: Int = VIDEO_BIT_DEPTH_BASE,
        ) {
            fun scaleBitrate(
                targetSize: Size,
                targetFrameRate: Int,
                targetBitDepth: Int,
            ): Int =
                VideoConfigUtil.scaleBitrate(
                    baseBitrate = bitrate,
                    actualBitDepth = targetBitDepth,
                    baseBitDepth = bitDepth,
                    actualFrameRate = targetFrameRate,
                    baseFrameRate = frameRate,
                    actualWidth = targetSize.width,
                    baseWidth = size.width,
                    actualHeight = targetSize.height,
                    baseHeight = size.height,
                )
        }

        // Base configs are anchored at 720p (1280x720 @ 30fps, 8-bit) and scaled by actual source
        // settings.
        // Bitrates are aligned with Android CDD Section 5.2 (Video Encoding) for 720p @ 30fps:
        // - AVC (§5.2.2), VP8 (§5.2.3), VP9 (§5.2.4), HEVC (§5.2.5): 4 Mbps.
        //   Using 4 Mbps prevents overloading software encoder rate-control buffers on entry-level
        //   devices (e.g., libvpx assertion failure in b/559568631).
        // - AV1 (§5.2.6): 8 Mbps for 720p @ 30fps.
        // - H.263 / MPEG-4: Although CDD §5.2 does not define a table for these legacy formats,
        //   anchoring them at 4 Mbps for 720p scales down cleanly to ~1.5 Mbps at SD (720x480)
        //   and ~110 Kbps at QCIF (176x144 @ 15fps), which satisfies CDD §5.2.1 H.263 Baseline
        //   Profile Level 45 (max 128 Kbps).
        private val VIDEO_CONFIG_BASE_DEFAULT = BaseVideoConfig(bitrate = 4_000_000)
        private val VIDEO_CONFIG_BASE_AV1 = BaseVideoConfig(bitrate = 8_000_000)

        private fun getBaseConfig(mimeType: String): BaseVideoConfig =
            when (mimeType) {
                MediaFormat.MIMETYPE_VIDEO_AV1 -> VIDEO_CONFIG_BASE_AV1
                MediaFormat.MIMETYPE_VIDEO_AVC,
                MediaFormat.MIMETYPE_VIDEO_HEVC,
                MediaFormat.MIMETYPE_VIDEO_VP8,
                MediaFormat.MIMETYPE_VIDEO_VP9,
                MediaFormat.MIMETYPE_VIDEO_H263,
                MediaFormat.MIMETYPE_VIDEO_MPEG4 -> VIDEO_CONFIG_BASE_DEFAULT
                else -> VIDEO_CONFIG_BASE_DEFAULT
            }
    }

    override fun get(): VideoEncoderConfig {
        val resolvedFrameRates =
            VideoConfigUtil.resolveFrameRates(
                videoSpec = videoSpec,
                expectedCaptureFrameRateRange = expectedFrameRateRange,
            )
        Logger.d(
            TAG,
            "Resolved VIDEO frame rates: " +
                "Capture frame rate = ${resolvedFrameRates.captureRate}fps. " +
                "Encode frame rate = ${resolvedFrameRates.encodeRate}fps.",
        )

        val videoSpecBitrate = videoSpec.bitrate
        val resolvedBitrate: Int =
            if (videoSpecBitrate != VideoSpec.BITRATE_UNSPECIFIED) {
                videoSpecBitrate
            } else {
                Logger.d(TAG, "Using fallback VIDEO bitrate")
                // We have no other information to go off of. Scale based on fallback defaults.
                getBaseConfig(mimeType)
                    .scaleBitrate(
                        targetSize = surfaceSize,
                        targetFrameRate = resolvedFrameRates.encodeRate,
                        targetBitDepth = dynamicRange.bitDepth,
                    )
            }

        val resolvedProfile =
            DynamicRangeUtil.dynamicRangeToCodecProfileLevelForMime(mimeType, dynamicRange)
        val dataSpace =
            VideoConfigUtil.mimeAndProfileToEncoderDataSpace(
                mimeType = mimeType,
                codecProfileLevel = resolvedProfile,
            )

        return VideoEncoderConfig.builder()
            .setMimeType(mimeType)
            .setInputTimebase(inputTimebase)
            .setResolution(surfaceSize)
            .setBitrate(resolvedBitrate)
            .setCaptureFrameRate(resolvedFrameRates.captureRate)
            .setEncodeFrameRate(resolvedFrameRates.encodeRate)
            .setProfile(resolvedProfile)
            .setDataSpace(dataSpace)
            .build()
    }
}
