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

import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.Timebase
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.utils.DynamicRangeUtil
import androidx.core.util.Supplier

/**
 * A [VideoEncoderConfig] supplier that resolves requested encoder settings from a [VideoSpec] for
 * the given surface [Size] when no [VideoProfileProxy] matches the requested video format.
 *
 * The bitrate is derived from an optional reference [VideoProfileProxy] of another codec, but never
 * lower than the pre-defined default values, which are used when no reference is available.
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
 * @param isHighSpeedCapture Whether the frames come from a dedicated high-speed capture pipeline.
 * @param referenceVideoProfile An optional [VideoProfileProxy] of the same quality, possibly of a
 *   different codec, used to derive the default bitrate. The derived bitrate is never lower than
 *   the pre-defined default. If `null`, invalid, or its codec has no known relative coding
 *   efficiency, the pre-defined default is used.
 */
public constructor(
    private val mimeType: String,
    private val inputTimebase: Timebase,
    private val videoSpec: VideoSpec,
    private val surfaceSize: Size,
    private val dynamicRange: DynamicRange,
    private val expectedFrameRateRange: Range<Int>,
    private val isHighSpeedCapture: Boolean = false,
    private val referenceVideoProfile: VideoProfileProxy? = null,
) : Supplier<VideoEncoderConfig> {

    public companion object {
        private const val TAG = "VidEncCfgDefaultRslvr"
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
                val defaultBitrate =
                    VideoConfigUtil.getDefaultVideoBitrate(
                        mimeType = mimeType,
                        size = surfaceSize,
                        frameRate = resolvedFrameRates.encodeRate,
                        bitDepth = dynamicRange.bitDepth,
                    )
                val referenceBitrate = resolveReferenceBitrate(resolvedFrameRates.encodeRate)
                // Never go below the default. A reference may itself be synthesized from the
                // defaults (e.g. for cameras without EncoderProfiles), in which case a codec ratio
                // would only lower an already untuned value.
                if (referenceBitrate != null && referenceBitrate > defaultBitrate) {
                    Logger.d(TAG, "Using VIDEO bitrate derived from reference EncoderProfiles")
                    referenceBitrate
                } else {
                    Logger.d(TAG, "Using fallback VIDEO bitrate")
                    defaultBitrate
                }
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
            .setHighSpeedCapture(isHighSpeedCapture)
            .build()
    }

    private fun resolveReferenceBitrate(encodeFrameRate: Int): Int? {
        val profile = referenceVideoProfile ?: return null
        return VideoConfigUtil.getReferenceVideoBitrate(
            mimeType = mimeType,
            referenceProfile = profile,
            size = surfaceSize,
            frameRate = encodeFrameRate,
            bitDepth = dynamicRange.bitDepth,
        )
    }
}
