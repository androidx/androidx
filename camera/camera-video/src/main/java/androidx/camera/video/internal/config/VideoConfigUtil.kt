/*
 * Copyright 2025 The Android Open Source Project
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

import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8
import android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvavSe
import android.media.MediaCodecInfo.CodecProfileLevel.DolbyVisionProfileDvheSt
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
import android.media.MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile0
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile1
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile2HDR10Plus
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR
import android.media.MediaCodecInfo.CodecProfileLevel.VP9Profile3HDR10Plus
import android.media.MediaFormat
import android.util.Range
import android.util.Rational
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.SurfaceRequest.FRAME_RATE_RANGE_UNSPECIFIED
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.Timebase
import androidx.camera.video.MediaSpec.Companion.OUTPUT_FORMAT_WEBM
import androidx.camera.video.MediaSpec.OutputFormat
import androidx.camera.video.VideoSpec
import androidx.camera.video.internal.compat.quirk.DeviceQuirks
import androidx.camera.video.internal.compat.quirk.MediaCodecDefaultDataSpaceQuirk
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_HLG
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT2020_PQ
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_BT709
import androidx.camera.video.internal.encoder.VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED
import androidx.camera.video.internal.utils.DynamicRangeUtil

/** A collection of utilities used for resolving and debugging video configurations. */
public object VideoConfigUtil {
    private const val TAG = "VideoConfigUtil"
    private val MIME_TO_DATA_SPACE_MAP: MutableMap<String, Map<Int, VideoEncoderDataSpace>>
    public const val VIDEO_FRAME_RATE_FIXED_DEFAULT: Int = 30
    private const val VIDEO_ENCODER_MIME_MPEG4_DEFAULT = MediaFormat.MIMETYPE_VIDEO_AVC
    private const val VIDEO_ENCODER_MIME_WEBM_DEFAULT = MediaFormat.MIMETYPE_VIDEO_VP8

    init {
        // --------------------------------------------------------------------------------------//
        // Mime and profile level to encoder data space map                                     //
        // --------------------------------------------------------------------------------------//
        val profHevcMap =
            mapOf<Int, VideoEncoderDataSpace>(
                // We treat SDR (main profile) as unspecified. Allow the encoder to use default data
                // space.
                HEVCProfileMain to ENCODER_DATA_SPACE_UNSPECIFIED,
                HEVCProfileMain10 to ENCODER_DATA_SPACE_BT2020_HLG,
                HEVCProfileMain10HDR10 to ENCODER_DATA_SPACE_BT2020_PQ,
                HEVCProfileMain10HDR10Plus to ENCODER_DATA_SPACE_BT2020_PQ,
            )
        val profAv1Map =
            mapOf<Int, VideoEncoderDataSpace>(
                // We treat SDR (main 8 profile) as unspecified. Allow the encoder to use default
                // data space.
                AV1ProfileMain8 to ENCODER_DATA_SPACE_UNSPECIFIED,
                AV1ProfileMain10 to ENCODER_DATA_SPACE_BT2020_HLG,
                AV1ProfileMain10HDR10 to ENCODER_DATA_SPACE_BT2020_PQ,
                AV1ProfileMain10HDR10Plus to ENCODER_DATA_SPACE_BT2020_PQ,
            )
        val profVp9Map =
            mapOf<Int, VideoEncoderDataSpace>(
                // We treat SDR (profile 0) as unspecified. Allow the encoder to use default data
                // space.
                VP9Profile0 to ENCODER_DATA_SPACE_UNSPECIFIED,
                VP9Profile2 to ENCODER_DATA_SPACE_BT2020_HLG,
                VP9Profile2HDR to ENCODER_DATA_SPACE_BT2020_PQ,
                VP9Profile2HDR10Plus to ENCODER_DATA_SPACE_BT2020_PQ,
                // Vp9 4:2:2 profiles
                VP9Profile1 to ENCODER_DATA_SPACE_UNSPECIFIED,
                VP9Profile3 to ENCODER_DATA_SPACE_BT2020_HLG,
                VP9Profile3HDR to ENCODER_DATA_SPACE_BT2020_PQ,
                VP9Profile3HDR10Plus to ENCODER_DATA_SPACE_BT2020_PQ,
            )
        val profDvMap =
            mapOf<Int, VideoEncoderDataSpace>(
                // For Dolby Vision profile 8, we only support 8.4 (10-bit HEVC HLG)
                DolbyVisionProfileDvheSt to ENCODER_DATA_SPACE_BT2020_HLG,
                // For Dolby Vision profile 9, we only support 9.2 (8-bit AVC SDR BT.709)
                DolbyVisionProfileDvavSe to ENCODER_DATA_SPACE_BT709,
            )
        // Combine all mime type maps
        MIME_TO_DATA_SPACE_MAP =
            mutableMapOf(
                MediaFormat.MIMETYPE_VIDEO_HEVC to profHevcMap,
                MediaFormat.MIMETYPE_VIDEO_AV1 to profAv1Map,
                MediaFormat.MIMETYPE_VIDEO_VP9 to profVp9Map,
                MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION to profDvMap,
            )
        // --------------------------------------------------------------------------------------//
    }

    /**
     * Resolves a compatible [VideoProfileProxy] from a list based on the provided MIME type and
     * [DynamicRange].
     *
     * This method attempts to find the first profile in the provided list that matches the
     * requested [videoMime] and the constraints (HDR format and bit depth) of the [dynamicRange].
     * If the [videoMime] is set to [VideoSpec.MIME_TYPE_UNSPECIFIED], it will return the first
     * profile that satisfies the [dynamicRange] requirements.
     *
     * @param videoMime The desired video MIME type.
     * @param dynamicRange The fully specified [DynamicRange] required for the profile.
     * @param videoProfiles A list of available [VideoProfileProxy]s.
     * @return The first matching [VideoProfileProxy], or `null` if no compatible profile is found.
     */
    public fun resolveCompatibleVideoProfile(
        videoMime: String,
        dynamicRange: DynamicRange,
        videoProfiles: List<VideoProfileProxy>,
    ): VideoProfileProxy? {
        val hdrFormats = DynamicRangeUtil.dynamicRangeToVideoProfileHdrFormats(dynamicRange)
        val bitDepths = DynamicRangeUtil.dynamicRangeToVideoProfileBitDepth(dynamicRange)

        return videoProfiles.firstOrNull {
            // is HDR compatible
            hdrFormats.contains(it.hdrFormat) &&
                bitDepths.contains(it.bitDepth) &&
                // is MIME type compatible
                (videoMime == VideoSpec.MIME_TYPE_UNSPECIFIED || it.mediaType == videoMime)
        }
    }

    /**
     * Maps a given [OutputFormat] to its default video MIME type.
     *
     * @param outputFormat The video recording output format.
     * @return The default video MIME type string associated with the output format.
     */
    public fun outputFormatToVideoMime(@OutputFormat outputFormat: Int): String {
        return when (outputFormat) {
            OUTPUT_FORMAT_WEBM -> VIDEO_ENCODER_MIME_WEBM_DEFAULT
            else -> VIDEO_ENCODER_MIME_MPEG4_DEFAULT
        }
    }

    /**
     * Returns a default mime for the given dynamic range.
     *
     * If the dynamic range is not supported, `null` will be returned.
     */
    public fun getDynamicRangeDefaultMime(dynamicRange: DynamicRange): String? {
        return when (dynamicRange.encoding) {
            DynamicRange.ENCODING_DOLBY_VISION ->
                // Dolby vision only supports dolby vision encoders
                MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION
            DynamicRange.ENCODING_HLG,
            DynamicRange.ENCODING_HDR10,
            DynamicRange.ENCODING_HDR10_PLUS ->
                // For now most hdr formats default to h265 (HEVC), though VP9 or AV1 may also be
                // supported.
                MediaFormat.MIMETYPE_VIDEO_HEVC
            DynamicRange.ENCODING_SDR ->
                // For SDR, default to h264 (AVC)
                MediaFormat.MIMETYPE_VIDEO_AVC
            else -> null
        }
    }

    /**
     * Returns a set of supported [DynamicRange]s for the given video MIME type.
     *
     * @param mime The video MIME type to query.
     * @return A [Set] of [DynamicRange] compatible with the video MIME type.
     */
    public fun getDynamicRangesForMime(mime: String): Set<DynamicRange> {
        return DynamicRangeFormatComboRegistry.getDynamicRangesForVideoMime(mime)
    }

    /**
     * Resolves video related information into a [VideoEncoderConfig].
     *
     * @param videoMimeInfo the video mime info.
     * @param videoSpec the video spec.
     * @param inputTimebase the timebase of the input frame.
     * @param surfaceSize the surface size.
     * @param expectedFrameRateRange the expected frame rate range.
     * @return a VideoEncoderConfig.
     */
    @JvmStatic
    public fun resolveVideoEncoderConfig(
        videoMimeInfo: VideoMimeInfo,
        inputTimebase: Timebase,
        videoSpec: VideoSpec,
        surfaceSize: Size,
        dynamicRange: DynamicRange,
        expectedFrameRateRange: Range<Int>,
    ): VideoEncoderConfig {
        val configSupplier =
            if (videoMimeInfo.compatibleVideoProfile != null) {
                VideoEncoderConfigVideoProfileResolver(
                    mimeType = videoMimeInfo.mimeType,
                    inputTimebase = inputTimebase,
                    videoSpec = videoSpec,
                    surfaceSize = surfaceSize,
                    videoProfile = videoMimeInfo.compatibleVideoProfile,
                    dynamicRange = dynamicRange,
                    expectedFrameRateRange = expectedFrameRateRange,
                )
            } else {
                VideoEncoderConfigDefaultResolver(
                    mimeType = videoMimeInfo.mimeType,
                    inputTimebase = inputTimebase,
                    videoSpec = videoSpec,
                    surfaceSize = surfaceSize,
                    dynamicRange = dynamicRange,
                    expectedFrameRateRange = expectedFrameRateRange,
                )
            }
        return configSupplier.get()
    }

    /**
     * Workarounds data space of [VideoEncoderConfig] if required.
     *
     * @param config the video encoder config.
     * @param hasGlProcessing whether OpenGL processing is involved.
     * @return VideoEncoderConfig.
     */
    @JvmStatic
    public fun workaroundDataSpaceIfRequired(
        config: VideoEncoderConfig,
        hasGlProcessing: Boolean,
    ): VideoEncoderConfig {
        // Not to modify data space if it is already specified.
        if (config.dataSpace != ENCODER_DATA_SPACE_UNSPECIFIED) {
            return config
        }

        // Apply workaround if required.
        val quirk = DeviceQuirks.get(MediaCodecDefaultDataSpaceQuirk::class.java)
        if (hasGlProcessing && quirk != null) {
            val dataSpace = quirk.suggestedDataSpace
            return config.toBuilder().setDataSpace(dataSpace).build()
        }
        return config
    }

    /**
     * Scales the bitrate based on the input conditions.
     *
     * @param baseBitrate the bitrate to be scaled.
     * @param actualBitDepth the actual bit depth.
     * @param baseBitDepth the base bit depth.
     * @param actualFrameRate the actual frame rate.
     * @param baseFrameRate the base frame rate.
     * @param actualWidth the actual video width.
     * @param baseWidth the base video width.
     * @param actualHeight the actual video height.
     * @param baseHeight the base video height.
     * @return the scaled bit rate.
     */
    @JvmStatic
    public fun scaleBitrate(
        baseBitrate: Int,
        actualBitDepth: Int,
        baseBitDepth: Int,
        actualFrameRate: Int,
        baseFrameRate: Int,
        actualWidth: Int,
        baseWidth: Int,
        actualHeight: Int,
        baseHeight: Int,
    ): Int {
        //  Scale bit depth to match new bit depth
        val bitDepthRatio = Rational(actualBitDepth, baseBitDepth)
        // Scale bitrate to match current frame rate
        val frameRateRatio = Rational(actualFrameRate, baseFrameRate)
        // Scale bitrate depending on number of actual pixels relative to profile's
        // number of pixels.
        // TODO(b/191678894): This should come from the eventual crop rectangle rather
        //  than the full surface size.
        val widthRatio = Rational(actualWidth, baseWidth)
        val heightRatio = Rational(actualHeight, baseHeight)
        val resolvedBitrate =
            (baseBitrate *
                    bitDepthRatio.toDouble() *
                    frameRateRatio.toDouble() *
                    widthRatio.toDouble() *
                    heightRatio.toDouble())
                .toInt()
        var debugString = ""
        if (Logger.isDebugEnabled(TAG)) {
            debugString =
                "Base Bitrate(${baseBitrate}bps) * " +
                    "Bit Depth Ratio ($actualBitDepth / $baseBitDepth) * " +
                    "Frame Rate Ratio($actualFrameRate / $baseFrameRate) * " +
                    "Width Ratio($actualWidth / $baseWidth) * " +
                    "Height Ratio($actualHeight / $baseHeight) = " +
                    "$resolvedBitrate"
        }
        Logger.d(TAG, debugString)
        return resolvedBitrate
    }

    /**
     * Returns the encoder data space for the given mime and profile.
     *
     * @return The data space for the given mime type and profile, or
     *   [VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED] if the profile represents SDR or is
     *   unsupported.
     */
    public fun mimeAndProfileToEncoderDataSpace(
        mimeType: String,
        codecProfileLevel: Int,
    ): VideoEncoderDataSpace {
        val profileToDataSpaceMap = MIME_TO_DATA_SPACE_MAP[mimeType]
        if (profileToDataSpaceMap != null) {
            val dataSpace = profileToDataSpaceMap[codecProfileLevel]
            if (dataSpace != null) {
                return dataSpace
            }
        }
        Logger.w(
            TAG,
            "Unsupported mime type $mimeType or profile level $codecProfileLevel. Data space is " +
                "unspecified.",
        )
        return ENCODER_DATA_SPACE_UNSPECIFIED
    }

    internal fun resolveFrameRates(
        videoSpec: VideoSpec,
        expectedCaptureFrameRateRange: Range<Int>,
    ): CaptureEncodeRates {
        val captureFrameRate: Int =
            if (expectedCaptureFrameRateRange == FRAME_RATE_RANGE_UNSPECIFIED) {
                VIDEO_FRAME_RATE_FIXED_DEFAULT
            } else {
                expectedCaptureFrameRateRange.upper
            }
        val encodeFrameRate: Int =
            if (videoSpec.encodeFrameRate != VideoSpec.ENCODE_FRAME_RATE_UNSPECIFIED) {
                videoSpec.encodeFrameRate
            } else {
                captureFrameRate
            }
        Logger.d(
            TAG,
            "Resolved capture/encode frame rate ${captureFrameRate}fps/${encodeFrameRate}fps, " +
                "[Expected operating range: " +
                if (expectedCaptureFrameRateRange == FRAME_RATE_RANGE_UNSPECIFIED) {
                    "<UNSPECIFIED>"
                } else {
                    "$expectedCaptureFrameRateRange"
                } +
                "]",
        )
        return CaptureEncodeRates(captureFrameRate, encodeFrameRate)
    }
}
