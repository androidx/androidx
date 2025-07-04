/*
 * Copyright 2024 The Android Open Source Project
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

import android.media.CamcorderProfile
import android.media.EncoderProfiles
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.core.impl.ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE
import androidx.camera.video.Quality
import androidx.camera.video.Quality.ConstantQuality
import androidx.camera.video.Quality.FHD
import androidx.camera.video.Quality.HD
import androidx.camera.video.Quality.QUALITY_SOURCE_REGULAR
import androidx.camera.video.Quality.SD
import androidx.camera.video.Quality.UHD
import androidx.camera.video.internal.encoder.VideoEncoderInfo

/**
 * Provides default encoder profiles for cameras without built-in profiles.
 *
 * This class serves as a fallback [EncoderProfilesProvider] when:
 * * An external camera lacks [CamcorderProfile] support.
 * * The device has no built-in cameras with [CamcorderProfile] data for reference.
 *
 * It generates generic encoder profiles with video/audio parameters that should be widely
 * compatible across Android devices.
 */
public class DefaultEncoderProfilesProvider(
    private val cameraInfo: CameraInfoInternal,
    private val targetQualities: List<Quality>,
    private val videoEncoderInfoFinder: VideoEncoderInfo.Finder,
) : EncoderProfilesProvider {

    private val supportedSizes by lazy {
        cameraInfo.getSupportedResolutions(INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE)
    }

    private val encoderProfilesMap = mutableMapOf<Int, EncoderProfilesProxy?>()

    override fun hasProfile(quality: Int): Boolean = getProfileInternal(quality) != null

    override fun getAll(quality: Int): EncoderProfilesProxy? = getProfileInternal(quality)

    private fun getProfileInternal(quality: Int): EncoderProfilesProxy? {
        if (encoderProfilesMap.containsKey(quality)) {
            return encoderProfilesMap[quality]
        }

        return generateEncoderProfiles(quality).also { encoderProfilesMap[quality] = it }
    }

    private fun generateEncoderProfiles(quality: Int): EncoderProfilesProxy? {
        val videoProfile = generateVideoProfiles(quality) ?: return null

        return createDefaultEncoderProfiles(
            videoProfile = videoProfile,
            audioProfile = createDefaultAudioProfile(),
        )
    }

    private fun generateVideoProfiles(quality: Int): VideoProfileProxy? {
        val qualityObj = targetQualities.find(quality) ?: return null

        for (size in qualityObj.getTypicalSizes()) {
            if (!supportedSizes.contains(size)) continue

            val videoProfile =
                resolveVideoProfile(
                    width = size.width,
                    height = size.height,
                    bitrate = qualityObj.getTypicalBitrate(),
                )

            if (videoProfile != null) {
                return videoProfile
            }
        }
        return null
    }

    private fun resolveVideoProfile(width: Int, height: Int, bitrate: Int): VideoProfileProxy? {
        val videoProfile =
            createDefaultVideoProfile(width = width, height = height, bitrate = bitrate)
        val encoderInfo = videoEncoderInfoFinder.find(videoProfile.mediaType) ?: return null

        if (!encoderInfo.isSizeSupportedAllowSwapping(width, height)) {
            return null
        }

        // Check if the bitrate is supported. If not, clamp it to the supported range.
        val resolvedBitrate = encoderInfo.supportedBitrateRange.clamp(bitrate)

        // Recreate the video profile if the bitrate is changed.
        return if (resolvedBitrate != bitrate) {
            createDefaultVideoProfile(width = width, height = height, bitrate = resolvedBitrate)
        } else {
            videoProfile
        }
    }

    /**
     * Creates default encoder profiles.
     *
     * See https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf, 5.1. Media
     * Codecs.
     */
    private fun createDefaultEncoderProfiles(
        defaultDurationSeconds: Int = DEFAULT_DURATION_SECONDS,
        recommendedFileFormat: Int = DEFAULT_OUTPUT_FORMAT,
        videoProfile: VideoProfileProxy,
        audioProfile: AudioProfileProxy,
    ): EncoderProfilesProxy {
        return EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
            defaultDurationSeconds,
            recommendedFileFormat,
            listOf(audioProfile),
            listOf(videoProfile),
        )
    }

    /**
     * Creates default video profile.
     *
     * See https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf, 5.1.3.
     * Video Codecs and 5.2. Video Encoding.
     */
    private fun createDefaultVideoProfile(
        codec: Int = DEFAULT_VIDEO_CODEC,
        mimeType: String = DEFAULT_VIDEO_MIME_TYPE,
        width: Int,
        height: Int,
        bitrate: Int,
        frameRate: Int = DEFAULT_VIDEO_FRAME_RATE,
        profile: Int = DEFAULT_VIDEO_PROFILE,
        bitDepth: Int = DEFAULT_VIDEO_BIT_DEPTH,
        chromaSubsampling: Int = DEFAULT_VIDEO_CHROMA_SUBSAMPLING,
        hdrFormat: Int = DEFAULT_VIDEO_HDR_FORMAT,
    ): VideoProfileProxy {
        return VideoProfileProxy.create(
            codec,
            mimeType,
            bitrate,
            frameRate,
            width,
            height,
            profile,
            bitDepth,
            chromaSubsampling,
            hdrFormat,
        )
    }

    /**
     * Creates default audio profile.
     *
     * See https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf, 5.1.1.
     * Audio Codecs and 5.4. Audio Recording.
     */
    private fun createDefaultAudioProfile(
        codec: Int = DEFAULT_AUDIO_CODEC,
        mimeType: String = DEFAULT_AUDIO_MIME_TYPE,
        bitRate: Int = DEFAULT_AUDIO_BITRATE,
        sampleRate: Int = DEFAULT_AUDIO_SAMPLE_RATE,
        channels: Int = DEFAULT_AUDIO_CHANNELS,
        profile: Int = DEFAULT_AUDIO_PROFILE,
    ): AudioProfileProxy {
        return AudioProfileProxy.create(codec, mimeType, bitRate, sampleRate, channels, profile)
    }

    /**
     * Returns common bitrate for 30 fps video for each [Quality].
     *
     * See https://source.android.com/static/docs/compatibility/5.1/android-5.1-cdd.pdf, 5.2. Video
     * Encoding.
     */
    private fun Quality.getTypicalBitrate(): Int =
        when (this) {
            UHD -> DEFAULT_VIDEO_BITRATE_UHD
            FHD -> DEFAULT_VIDEO_BITRATE_FHD
            HD -> DEFAULT_VIDEO_BITRATE_HD
            SD -> DEFAULT_VIDEO_BITRATE_SD
            else -> throw IllegalArgumentException("Undefined bitrate for quality: $this")
        }

    private fun List<Quality>.find(quality: Int): ConstantQuality? =
        find { (it as ConstantQuality).getQualityValue(QUALITY_SOURCE_REGULAR) == quality }
            as? ConstantQuality

    public companion object {
        // Duration seconds value is observed from real devices.
        internal const val DEFAULT_DURATION_SECONDS: Int = 60
        internal const val DEFAULT_OUTPUT_FORMAT: Int = MediaRecorder.OutputFormat.MPEG_4

        internal const val DEFAULT_VIDEO_CODEC: Int = MediaRecorder.VideoEncoder.H264
        internal const val DEFAULT_VIDEO_MIME_TYPE: String = MediaFormat.MIMETYPE_VIDEO_AVC
        internal const val DEFAULT_VIDEO_FRAME_RATE: Int = 30
        internal const val DEFAULT_VIDEO_PROFILE = EncoderProfilesProxy.CODEC_PROFILE_NONE
        internal const val DEFAULT_VIDEO_BIT_DEPTH = VideoProfileProxy.BIT_DEPTH_8
        internal const val DEFAULT_VIDEO_CHROMA_SUBSAMPLING = EncoderProfiles.VideoProfile.YUV_420
        internal const val DEFAULT_VIDEO_HDR_FORMAT = EncoderProfiles.VideoProfile.HDR_NONE

        internal const val DEFAULT_VIDEO_BITRATE_UHD =
            40_000_000 // 40 Mbps, scaled by FHD. See VideoConfigUtil.scaleAndClampBitrate()
        internal const val DEFAULT_VIDEO_BITRATE_FHD = 10_000_000 // 10 Mbps
        internal const val DEFAULT_VIDEO_BITRATE_HD = 4_000_000 // 4 Mbps
        internal const val DEFAULT_VIDEO_BITRATE_SD = 2_000_000 // 2 Mbps

        internal const val DEFAULT_AUDIO_CODEC: Int = MediaRecorder.AudioEncoder.AAC
        internal const val DEFAULT_AUDIO_MIME_TYPE: String = MediaFormat.MIMETYPE_AUDIO_AAC
        // Audio bitrate value is observed from real devices.
        internal const val DEFAULT_AUDIO_BITRATE: Int = 96000
        internal const val DEFAULT_AUDIO_SAMPLE_RATE: Int = 44100
        internal const val DEFAULT_AUDIO_CHANNELS: Int = 1
        internal const val DEFAULT_AUDIO_PROFILE: Int = MediaCodecInfo.CodecProfileLevel.AACObjectLC
    }
}
