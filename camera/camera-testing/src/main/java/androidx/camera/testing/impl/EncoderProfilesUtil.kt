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
package androidx.camera.testing.impl

import android.media.EncoderProfiles
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Size
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.video.internal.utils.DynamicRangeUtil.dynamicRangeToVideoProfileHdrFormats

/**
 * Utility methods for testing [EncoderProfiles] related classes, including predefined resolutions,
 * attributes and [EncoderProfilesProxy], which can be used directly on the unit tests.
 */
public object EncoderProfilesUtil {
    /** Resolution for QCIF. */
    public val RESOLUTION_QCIF: Size = Size(176, 144)

    /** Resolution for QVGA. */
    public val RESOLUTION_QVGA: Size = Size(320, 240)

    /** Resolution for CIF. */
    public val RESOLUTION_CIF: Size = Size(352, 288)

    /** Resolution for VGA. */
    public val RESOLUTION_VGA: Size = Size(640, 480)

    /** Resolution for 480P. */
    public val RESOLUTION_480P: Size = Size(720, 480) /* 640, 704 or 720 x 480 */

    /** Resolution for 720P. */
    public val RESOLUTION_720P: Size = Size(1280, 720)

    /** Resolution for 1080P. */
    public val RESOLUTION_1080P: Size = Size(1920, 1080) /* 1920 x 1080 or 1088 */

    /** Resolution for 2K. */
    public val RESOLUTION_2K: Size = Size(2048, 1080)

    /** Resolution for QHD. */
    public val RESOLUTION_QHD: Size = Size(2560, 1440)

    /** Resolution for 2160P. */
    public val RESOLUTION_2160P: Size = Size(3840, 2160)

    /** Resolution for 4KDCI. */
    public val RESOLUTION_4KDCI: Size = Size(4096, 2160)

    /** Default duration. */
    public const val DEFAULT_DURATION: Int = 30

    /** Default output format. */
    public const val DEFAULT_OUTPUT_FORMAT: Int = MediaRecorder.OutputFormat.MPEG_4

    /** Default video codec. */
    public const val DEFAULT_VIDEO_CODEC: Int = MediaRecorder.VideoEncoder.H264

    /** Default media type. */
    public const val DEFAULT_VIDEO_MEDIA_TYPE: String = MediaFormat.MIMETYPE_VIDEO_AVC

    /** Default video bitrate. */
    public const val DEFAULT_VIDEO_BITRATE: Int = 8 * 1024 * 1024

    /** Default video high speed bitrate. */
    public const val DEFAULT_VIDEO_HIGH_SPEED_BITRATE: Int = DEFAULT_VIDEO_BITRATE * 2

    /** Default video frame rate. */
    public const val DEFAULT_VIDEO_FRAME_RATE: Int = 30

    /** Default video high speed frame rate. */
    public const val DEFAULT_VIDEO_HIGH_SPEED_FRAME_RATE: Int = 120

    /** Default video code profile. */
    public const val DEFAULT_VIDEO_PROFILE: Int = EncoderProfilesProxy.CODEC_PROFILE_NONE

    /** Default bit depth. */
    public const val DEFAULT_VIDEO_BIT_DEPTH: Int = VideoProfileProxy.BIT_DEPTH_8

    /** Default chroma subsampling. */
    public const val DEFAULT_VIDEO_CHROMA_SUBSAMPLING: Int = EncoderProfiles.VideoProfile.YUV_420

    /** Default hdr format. */
    public const val DEFAULT_VIDEO_HDR_FORMAT: Int = EncoderProfiles.VideoProfile.HDR_NONE

    /** Default audio codec. */
    public const val DEFAULT_AUDIO_CODEC: Int = MediaRecorder.AudioEncoder.AAC

    /** Default media type. */
    public const val DEFAULT_AUDIO_MEDIA_TYPE: String = MediaFormat.MIMETYPE_AUDIO_AAC

    /** Default audio bitrate. */
    public const val DEFAULT_AUDIO_BITRATE: Int = 192000

    /** Default audio sample rate. */
    public const val DEFAULT_AUDIO_SAMPLE_RATE: Int = 48000

    /** Default channel count. */
    public const val DEFAULT_AUDIO_CHANNELS: Int = 1

    /** Default audio code profile. */
    public const val DEFAULT_AUDIO_PROFILE: Int = EncoderProfilesProxy.CODEC_PROFILE_NONE

    /** EncoderProfiles */
    public val PROFILES_QCIF: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_QCIF)
    public val PROFILES_QVGA: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_QVGA)
    public val PROFILES_CIF: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_CIF)
    public val PROFILES_VGA: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_VGA)
    public val PROFILES_480P: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_480P)
    public val PROFILES_720P: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_720P)
    public val PROFILES_1080P: EncoderProfilesProxy =
        createFakeEncoderProfilesProxy(RESOLUTION_1080P)
    public val PROFILES_2K: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_2K)
    public val PROFILES_QHD: EncoderProfilesProxy = createFakeEncoderProfilesProxy(RESOLUTION_QHD)
    public val PROFILES_2160P: EncoderProfilesProxy =
        createFakeEncoderProfilesProxy(RESOLUTION_2160P)
    public val PROFILES_4KDCI: EncoderProfilesProxy =
        createFakeEncoderProfilesProxy(RESOLUTION_4KDCI)

    /** High speed EncoderProfiles with SDR dynamic range */
    public val PROFILES_HIGH_SPEED_480P: EncoderProfilesProxy =
        createFakeHighSpeedEncoderProfilesProxy(RESOLUTION_480P)
    public val PROFILES_HIGH_SPEED_720P: EncoderProfilesProxy =
        createFakeHighSpeedEncoderProfilesProxy(RESOLUTION_720P)
    public val PROFILES_HIGH_SPEED_1080P: EncoderProfilesProxy =
        createFakeHighSpeedEncoderProfilesProxy(RESOLUTION_1080P)
    public val PROFILES_HIGH_SPEED_2160P: EncoderProfilesProxy =
        createFakeHighSpeedEncoderProfilesProxy(RESOLUTION_2160P)

    /** A utility method to create an EncoderProfilesProxy with some default values. */
    public fun createFakeEncoderProfilesProxy(
        videoProfiles: List<VideoProfileProxy>,
        audioProfiles: List<AudioProfileProxy> = listOf(createFakeAudioProfileProxy()),
        defaultDurationSeconds: Int = DEFAULT_DURATION,
        recommendedFileFormat: Int = DEFAULT_OUTPUT_FORMAT,
    ): EncoderProfilesProxy {
        return ImmutableEncoderProfilesProxy.create(
            defaultDurationSeconds,
            recommendedFileFormat,
            audioProfiles,
            videoProfiles,
        )
    }

    /** A utility method to create an EncoderProfilesProxy with some default values. */
    public fun createFakeEncoderProfilesProxy(
        videoResolution: Size,
        videoFrameRate: Int = DEFAULT_VIDEO_FRAME_RATE,
        dynamicRanges: Set<DynamicRange> = setOf(SDR),
        videoProfileProvider: (Size, Int, Int, Int) -> VideoProfileProxy =
            { resolution, frameRate, bitDepth, hdrFormat ->
                createFakeVideoProfileProxy(
                    frameRate = frameRate,
                    videoResolution = resolution,
                    videoBitDepth = bitDepth,
                    videoHdrFormat = hdrFormat,
                )
            },
    ): EncoderProfilesProxy {
        val videoProfiles =
            dynamicRanges.map { dynamicRange ->
                val videoBitDepth = dynamicRange.bitDepth
                val videoHdrFormat = dynamicRangeToVideoProfileHdrFormats(dynamicRange).single()
                videoProfileProvider(videoResolution, videoFrameRate, videoBitDepth, videoHdrFormat)
            }
        return createFakeEncoderProfilesProxy(videoProfiles = videoProfiles)
    }

    /** A utility method to create a high speed EncoderProfilesProxy with some default values. */
    public fun createFakeHighSpeedEncoderProfilesProxy(
        videoResolution: Size,
        videoFrameRate: Int = DEFAULT_VIDEO_HIGH_SPEED_FRAME_RATE,
        dynamicRanges: Set<DynamicRange> = setOf(SDR),
    ): EncoderProfilesProxy {
        return createFakeEncoderProfilesProxy(
            videoResolution,
            videoFrameRate = videoFrameRate,
            dynamicRanges = dynamicRanges,
            videoProfileProvider = { resolution, frameRate, bitDepth, hdrFormat ->
                createFakeHighSpeedVideoProfileProxy(
                    videoResolution = resolution,
                    frameRate = frameRate,
                    videoBitDepth = bitDepth,
                    videoHdrFormat = hdrFormat,
                )
            },
        )
    }

    /** A utility method to create a VideoProfileProxy with some default values. */
    public fun createFakeVideoProfileProxy(
        videoResolution: Size,
        videoCodec: Int = DEFAULT_VIDEO_CODEC,
        videoMediaType: String = DEFAULT_VIDEO_MEDIA_TYPE,
        frameRate: Int = DEFAULT_VIDEO_FRAME_RATE,
        bitrate: Int = DEFAULT_VIDEO_BITRATE,
        videoBitDepth: Int = DEFAULT_VIDEO_BIT_DEPTH,
        videoHdrFormat: Int = DEFAULT_VIDEO_HDR_FORMAT,
    ): VideoProfileProxy {
        return VideoProfileProxy.create(
            videoCodec,
            videoMediaType,
            bitrate,
            frameRate,
            videoResolution.width,
            videoResolution.height,
            DEFAULT_VIDEO_PROFILE,
            videoBitDepth,
            DEFAULT_VIDEO_CHROMA_SUBSAMPLING,
            videoHdrFormat,
        )
    }

    /** A utility method to create a high speed VideoProfileProxy with some default values. */
    public fun createFakeHighSpeedVideoProfileProxy(
        videoResolution: Size,
        videoCodec: Int = DEFAULT_VIDEO_CODEC,
        videoMediaType: String = DEFAULT_VIDEO_MEDIA_TYPE,
        frameRate: Int = DEFAULT_VIDEO_HIGH_SPEED_FRAME_RATE,
        bitrate: Int = DEFAULT_VIDEO_HIGH_SPEED_BITRATE,
        videoBitDepth: Int = DEFAULT_VIDEO_BIT_DEPTH,
        videoHdrFormat: Int = DEFAULT_VIDEO_HDR_FORMAT,
    ): VideoProfileProxy =
        createFakeVideoProfileProxy(
            videoResolution,
            videoCodec = videoCodec,
            videoMediaType = videoMediaType,
            frameRate = frameRate,
            bitrate = bitrate,
            videoBitDepth = videoBitDepth,
            videoHdrFormat = videoHdrFormat,
        )

    /** A utility method to create an AudioProfileProxy with some default values. */
    public fun createFakeAudioProfileProxy(): AudioProfileProxy {
        return AudioProfileProxy.create(
            DEFAULT_AUDIO_CODEC,
            DEFAULT_AUDIO_MEDIA_TYPE,
            DEFAULT_AUDIO_BITRATE,
            DEFAULT_AUDIO_SAMPLE_RATE,
            DEFAULT_AUDIO_CHANNELS,
            DEFAULT_AUDIO_PROFILE,
        )
    }
}
