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
package androidx.camera.video.internal.utils

import android.media.EncoderProfiles
import android.media.EncoderProfiles.VideoProfile.HDR_DOLBY_VISION
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10
import android.media.EncoderProfiles.VideoProfile.HDR_HDR10PLUS
import android.media.EncoderProfiles.VideoProfile.HDR_HLG
import android.media.EncoderProfiles.VideoProfile.HDR_NONE
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Range
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.Logger
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.video.internal.config.VideoConfigUtil
import androidx.camera.video.internal.encoder.VideoEncoderInfo

/** Utility class for encoder profiles related operations. */
public object EncoderProfilesUtil {
    private const val TAG = "EncoderProfilesUtil"

    // Duration seconds value is observed from real devices.
    internal const val DEFAULT_DURATION_SECONDS: Int = 60
    internal const val DEFAULT_OUTPUT_FORMAT: Int = MediaRecorder.OutputFormat.MPEG_4
    internal const val DEFAULT_VIDEO_CODEC: Int = MediaRecorder.VideoEncoder.H264
    internal const val DEFAULT_VIDEO_FRAME_RATE: Int = 30
    internal const val DEFAULT_VIDEO_CHROMA_SUBSAMPLING: Int = EncoderProfiles.VideoProfile.YUV_420
    internal const val DEFAULT_AUDIO_CODEC: Int = MediaRecorder.AudioEncoder.AAC
    internal const val DEFAULT_AUDIO_MIME_TYPE: String = MediaFormat.MIMETYPE_AUDIO_AAC
    // Audio bitrate value is observed from real devices.
    internal const val DEFAULT_AUDIO_BITRATE: Int = 96000
    internal const val DEFAULT_AUDIO_SAMPLE_RATE: Int = 44100
    internal const val DEFAULT_AUDIO_CHANNELS: Int = 1
    internal const val DEFAULT_AUDIO_PROFILE: Int = MediaCodecInfo.CodecProfileLevel.AACObjectLC

    /**
     * Derives an HDR VideoProfile from an SDR VideoProfile.
     *
     * The codec, media type, codec profile, HDR format and bit depth are derived from
     * [dynamicRange], and the bitrate is scaled by the bit depth ratio. Other fields are copied
     * from [sdrProfile].
     *
     * @param sdrProfile the SDR VideoProfile to derive from.
     * @param dynamicRange the HDR dynamic range of the derived VideoProfile, e.g.
     *   [DynamicRange.HLG_10_BIT]. Must be fully specified.
     * @return the derived VideoProfile.
     */
    @JvmStatic
    public fun deriveHdrVideoProfile(
        sdrProfile: VideoProfileProxy,
        dynamicRange: DynamicRange,
    ): VideoProfileProxy {
        require(
            dynamicRange.isFullySpecified && dynamicRange.encoding != DynamicRange.ENCODING_SDR
        ) {
            "Not a fully specified HDR dynamic range: $dynamicRange"
        }
        val hdrFormat = DynamicRangeUtil.dynamicRangeToVideoProfileHdrFormats(dynamicRange).single()
        val bitDepth = DynamicRangeUtil.dynamicRangeToVideoProfileBitDepth(dynamicRange).single()

        // "Guess" codec, media type and profile.
        val codec = deriveCodec(hdrFormat)
        val mediaType = EncoderProfilesProxy.getVideoCodecMimeType(codec)
        val profile =
            DynamicRangeUtil.dynamicRangeToCodecProfileLevelForMime(mediaType, dynamicRange)

        // "Guess" bit rate.
        val bitrate =
            VideoConfigUtil.scaleBitrate(
                baseBitrate = sdrProfile.bitrate,
                actualBitDepth = bitDepth,
                baseBitDepth = sdrProfile.bitDepth,
                actualFrameRate = sdrProfile.frameRate,
                baseFrameRate = sdrProfile.frameRate,
                actualWidth = sdrProfile.width,
                baseWidth = sdrProfile.width,
                actualHeight = sdrProfile.height,
                baseHeight = sdrProfile.height,
            )

        return VideoProfileProxy.create(
            codec,
            mediaType,
            bitrate,
            sdrProfile.frameRate,
            sdrProfile.width,
            sdrProfile.height,
            profile,
            bitDepth,
            sdrProfile.chromaSubsampling,
            hdrFormat,
        )
    }

    /**
     * Checks if an encoder supports the video profile and adapts the bitrate if possible.
     *
     * A 10-bit HDR profile is only supported when the encoder is hardware-accelerated. See
     * [VideoConfigUtil.isVideoProfileDynamicRangeSupported].
     *
     * @return the validated (and possibly adapted) video profile, or `null` if no encoder supports
     *   it.
     */
    @JvmStatic
    public fun validateOrAdapt(
        profile: VideoProfileProxy?,
        videoEncoderInfoFinder: VideoEncoderInfo.Finder,
    ): VideoProfileProxy? {
        if (profile == null) {
            return null
        }
        val videoEncoderInfo = videoEncoderInfoFinder.find(profile.mediaType)
        if (
            videoEncoderInfo == null ||
                !videoEncoderInfo.isSizeSupportedAllowSwapping(profile.width, profile.height)
        ) {
            return null
        }
        // 10-bit HDR requires a hardware-accelerated encoder (b/567407353).
        if (!VideoConfigUtil.isVideoProfileDynamicRangeSupported(profile, videoEncoderInfo)) {
            Logger.d(TAG, "Encoder doesn't support dynamic range of profile: $profile")
            return null
        }
        val baseBitrate = profile.bitrate
        val newBitrate = videoEncoderInfo.supportedBitrateRange.clamp(baseBitrate)
        return if (newBitrate == baseBitrate) profile else modifyBitrate(profile, newBitrate)
    }

    private fun modifyBitrate(baseProfile: VideoProfileProxy, newBitrate: Int): VideoProfileProxy =
        VideoProfileProxy.create(
            baseProfile.codec,
            baseProfile.mediaType,
            newBitrate,
            baseProfile.frameRate,
            baseProfile.width,
            baseProfile.height,
            baseProfile.profile,
            baseProfile.bitDepth,
            baseProfile.chromaSubsampling,
            baseProfile.hdrFormat,
        )

    /**
     * Creates a default VideoProfile for when no other video profile information is available.
     *
     * The codec, media type and codec profile are resolved from [dynamicRange]: AVC for SDR, HEVC
     * for HLG, HDR10 and HDR10+, and Dolby Vision for Dolby Vision. The bitrate is resolved by
     * [VideoConfigUtil.getDefaultVideoBitrate].
     *
     * See https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf, 5.1.3.
     * Video Codecs and 5.2. Video Encoding.
     *
     * @param size the video size.
     * @param dynamicRange the dynamic range of the video. Must be fully specified.
     * @return the default VideoProfile.
     */
    @JvmStatic
    public fun createDefaultVideoProfile(
        size: Size,
        dynamicRange: DynamicRange,
    ): VideoProfileProxy {
        require(dynamicRange.isFullySpecified) { "Dynamic range must be fully specified." }
        val hdrFormat = DynamicRangeUtil.dynamicRangeToVideoProfileHdrFormats(dynamicRange).single()
        val bitDepth = DynamicRangeUtil.dynamicRangeToVideoProfileBitDepth(dynamicRange).single()
        val codec = if (hdrFormat == HDR_NONE) DEFAULT_VIDEO_CODEC else deriveCodec(hdrFormat)
        val mediaType = EncoderProfilesProxy.getVideoCodecMimeType(codec)
        return VideoProfileProxy.create(
            codec,
            mediaType,
            VideoConfigUtil.getDefaultVideoBitrate(
                mimeType = mediaType,
                size = size,
                frameRate = DEFAULT_VIDEO_FRAME_RATE,
                bitDepth = bitDepth,
            ),
            DEFAULT_VIDEO_FRAME_RATE,
            size.width,
            size.height,
            DynamicRangeUtil.dynamicRangeToCodecProfileLevelForMime(mediaType, dynamicRange),
            bitDepth,
            DEFAULT_VIDEO_CHROMA_SUBSAMPLING,
            hdrFormat,
        )
    }

    /**
     * Creates default EncoderProfiles with the given video profiles and a default audio profile.
     *
     * See https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf, 5.1. Media
     * Codecs, 5.1.1. Audio Codecs and 5.4. Audio Recording.
     */
    @JvmStatic
    public fun createDefaultEncoderProfiles(
        videoProfiles: List<VideoProfileProxy>
    ): EncoderProfilesProxy =
        EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
            DEFAULT_DURATION_SECONDS,
            DEFAULT_OUTPUT_FORMAT,
            listOf(
                AudioProfileProxy.create(
                    DEFAULT_AUDIO_CODEC,
                    DEFAULT_AUDIO_MIME_TYPE,
                    DEFAULT_AUDIO_BITRATE,
                    DEFAULT_AUDIO_SAMPLE_RATE,
                    DEFAULT_AUDIO_CHANNELS,
                    DEFAULT_AUDIO_PROFILE,
                )
            ),
            videoProfiles,
        )

    @VideoProfileProxy.VideoEncoder
    private fun deriveCodec(hdrFormat: Int): Int =
        when (hdrFormat) {
            HDR_HLG,
            HDR_HDR10,
            HDR_HDR10PLUS -> MediaRecorder.VideoEncoder.HEVC
            // 10-bit Dolby Vision is Dolby Vision profile 8 (HEVC based).
            HDR_DOLBY_VISION -> MediaRecorder.VideoEncoder.DOLBY_VISION
            else -> throw IllegalArgumentException("Unexpected HDR format: $hdrFormat")
        }

    /**
     * Derives a VideoProfile from a base VideoProfile and a new resolution.
     *
     * Most fields are directly copied from the base VideoProfile except the bitrate, which will be
     * scaled and clamped according to the new resolution and the given bitrate range.
     *
     * @param baseVideoProfile the VideoProfile to derive.
     * @param newResolution the new resolution.
     * @param bitrateRangeToClamp the bitrate range to clamp. This is usually the supported bitrate
     *   range of the target codec.
     * @return a derived VideoProfile.
     */
    @JvmStatic
    public fun deriveVideoProfile(
        baseVideoProfile: VideoProfileProxy,
        newResolution: Size,
        bitrateRangeToClamp: Range<Int>,
    ): VideoProfileProxy {
        // "Guess" bit rate.
        var derivedBitrate =
            VideoConfigUtil.scaleBitrate(
                baseVideoProfile.bitrate,
                baseVideoProfile.bitDepth,
                baseVideoProfile.bitDepth,
                baseVideoProfile.frameRate,
                baseVideoProfile.frameRate,
                newResolution.width,
                baseVideoProfile.width,
                newResolution.height,
                baseVideoProfile.height,
            )
        derivedBitrate = bitrateRangeToClamp.clamp(derivedBitrate)

        return VideoProfileProxy.create(
            baseVideoProfile.codec,
            baseVideoProfile.mediaType,
            derivedBitrate,
            baseVideoProfile.frameRate,
            newResolution.width,
            newResolution.height,
            baseVideoProfile.profile,
            baseVideoProfile.bitDepth,
            baseVideoProfile.chromaSubsampling,
            baseVideoProfile.hdrFormat,
        )
    }

    /**
     * Gets the first VideoProfile from the given EncoderProfileProxy. Returns null if
     * encoderProfiles is null or there is no VideoProfile.
     */
    @JvmStatic
    public fun getFirstVideoProfile(encoderProfiles: EncoderProfilesProxy?): VideoProfileProxy? {
        return encoderProfiles?.getVideoProfiles()?.firstOrNull()
    }
}
