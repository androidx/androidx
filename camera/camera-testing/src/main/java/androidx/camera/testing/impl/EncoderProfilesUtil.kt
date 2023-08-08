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
import androidx.annotation.RequiresApi
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy

/**
 * Utility methods for testing [EncoderProfiles] related classes, including predefined
 * resolutions, attributes and [EncoderProfilesProxy], which can be used directly on the
 * unit tests.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

object EncoderProfilesUtil {
    /** Resolution for QCIF.  */
    val RESOLUTION_QCIF = Size(176, 144)

    /** Resolution for QVGA.  */
    val RESOLUTION_QVGA = Size(320, 240)

    /** Resolution for CIF.  */
    val RESOLUTION_CIF = Size(352, 288)

    /** Resolution for VGA.  */
    val RESOLUTION_VGA = Size(640, 480)

    /** Resolution for 480P.  */
    val RESOLUTION_480P = Size(720, 480) /* 640, 704 or 720 x 480 */

    /** Resolution for 720P.  */
    val RESOLUTION_720P = Size(1280, 720)

    /** Resolution for 1080P.  */
    val RESOLUTION_1080P = Size(1920, 1080) /* 1920 x 1080 or 1088 */

    /** Resolution for 2K.  */
    val RESOLUTION_2K = Size(2048, 1080)

    /** Resolution for QHD.  */
    val RESOLUTION_QHD = Size(2560, 1440)

    /** Resolution for 2160P.  */
    val RESOLUTION_2160P = Size(3840, 2160)

    /** Resolution for 4KDCI.  */
    val RESOLUTION_4KDCI = Size(4096, 2160)

    /** Default duration.  */
    const val DEFAULT_DURATION = 30

    /** Default output format.  */
    const val DEFAULT_OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4

    /** Default video codec.  */
    const val DEFAULT_VIDEO_CODEC = MediaRecorder.VideoEncoder.H264

    /** Default media type.  */
    const val DEFAULT_VIDEO_MEDIA_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC

    /** Default video bitrate.  */
    const val DEFAULT_VIDEO_BITRATE = 8 * 1024 * 1024

    /** Default video frame rate.  */
    const val DEFAULT_VIDEO_FRAME_RATE = 30

    /** Default video code profile.  */
    const val DEFAULT_VIDEO_PROFILE = EncoderProfilesProxy.CODEC_PROFILE_NONE

    /** Default bit depth.  */
    const val DEFAULT_VIDEO_BIT_DEPTH = VideoProfileProxy.BIT_DEPTH_8

    /** Default chroma subsampling.  */
    const val DEFAULT_VIDEO_CHROMA_SUBSAMPLING = EncoderProfiles.VideoProfile.YUV_420

    /** Default hdr format.  */
    const val DEFAULT_VIDEO_HDR_FORMAT = EncoderProfiles.VideoProfile.HDR_NONE

    /** Default audio codec.  */
    const val DEFAULT_AUDIO_CODEC = MediaRecorder.AudioEncoder.AAC

    /** Default media type.  */
    const val DEFAULT_AUDIO_MEDIA_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC

    /** Default audio bitrate.  */
    const val DEFAULT_AUDIO_BITRATE = 192000

    /** Default audio sample rate.  */
    const val DEFAULT_AUDIO_SAMPLE_RATE = 48000

    /** Default channel count.  */
    const val DEFAULT_AUDIO_CHANNELS = 1

    /** Default audio code profile.  */
    const val DEFAULT_AUDIO_PROFILE = EncoderProfilesProxy.CODEC_PROFILE_NONE
    val PROFILES_QCIF = createFakeEncoderProfilesProxy(
        RESOLUTION_QCIF.width,
        RESOLUTION_QCIF.height
    )
    val PROFILES_QVGA = createFakeEncoderProfilesProxy(
        RESOLUTION_QVGA.width,
        RESOLUTION_QVGA.height
    )
    val PROFILES_CIF = createFakeEncoderProfilesProxy(
        RESOLUTION_CIF.width,
        RESOLUTION_CIF.height
    )
    val PROFILES_VGA = createFakeEncoderProfilesProxy(
        RESOLUTION_VGA.width,
        RESOLUTION_VGA.height
    )
    val PROFILES_480P = createFakeEncoderProfilesProxy(
        RESOLUTION_480P.width,
        RESOLUTION_480P.height
    )
    val PROFILES_720P = createFakeEncoderProfilesProxy(
        RESOLUTION_720P.width,
        RESOLUTION_720P.height
    )
    val PROFILES_1080P = createFakeEncoderProfilesProxy(
        RESOLUTION_1080P.width,
        RESOLUTION_1080P.height
    )
    val PROFILES_2K = createFakeEncoderProfilesProxy(
        RESOLUTION_2K.width,
        RESOLUTION_2K.height
    )
    val PROFILES_QHD = createFakeEncoderProfilesProxy(
        RESOLUTION_QHD.width,
        RESOLUTION_QHD.height
    )
    val PROFILES_2160P = createFakeEncoderProfilesProxy(
        RESOLUTION_2160P.width,
        RESOLUTION_2160P.height
    )
    val PROFILES_4KDCI = createFakeEncoderProfilesProxy(
        RESOLUTION_4KDCI.width,
        RESOLUTION_4KDCI.height
    )

    /** A utility method to create an EncoderProfilesProxy with some default values.  */
    fun createFakeEncoderProfilesProxy(
        videoFrameWidth: Int,
        videoFrameHeight: Int
    ): EncoderProfilesProxy {
        val videoProfile = createFakeVideoProfileProxy(
            videoFrameWidth,
            videoFrameHeight
        )
        val audioProfile = createFakeAudioProfileProxy()
        return ImmutableEncoderProfilesProxy.create(
            DEFAULT_DURATION,
            DEFAULT_OUTPUT_FORMAT,
            listOf(audioProfile),
            listOf(videoProfile)
        )
    }

    /** A utility method to create a VideoProfileProxy with some default values.  */
    fun createFakeVideoProfileProxy(
        videoFrameWidth: Int,
        videoFrameHeight: Int,
        videoCodec: Int = DEFAULT_VIDEO_CODEC,
        videoMediaType: String = DEFAULT_VIDEO_MEDIA_TYPE,
        videoBitDepth: Int = DEFAULT_VIDEO_BIT_DEPTH,
        videoHdrFormat: Int = DEFAULT_VIDEO_HDR_FORMAT
    ): VideoProfileProxy {
        return VideoProfileProxy.create(
            videoCodec,
            videoMediaType,
            DEFAULT_VIDEO_BITRATE,
            DEFAULT_VIDEO_FRAME_RATE,
            videoFrameWidth,
            videoFrameHeight,
            DEFAULT_VIDEO_PROFILE,
            videoBitDepth,
            DEFAULT_VIDEO_CHROMA_SUBSAMPLING,
            videoHdrFormat
        )
    }

    /** A utility method to create an AudioProfileProxy with some default values.  */
    fun createFakeAudioProfileProxy(): AudioProfileProxy {
        return AudioProfileProxy.create(
            DEFAULT_AUDIO_CODEC,
            DEFAULT_AUDIO_MEDIA_TYPE,
            DEFAULT_AUDIO_BITRATE,
            DEFAULT_AUDIO_SAMPLE_RATE,
            DEFAULT_AUDIO_CHANNELS,
            DEFAULT_AUDIO_PROFILE
        )
    }
}
