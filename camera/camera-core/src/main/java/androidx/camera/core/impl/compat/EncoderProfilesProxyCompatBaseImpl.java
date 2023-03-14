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

package androidx.camera.core.impl.compat;

import static android.media.MediaRecorder.AudioEncoder.AAC;
import static android.media.MediaRecorder.AudioEncoder.AAC_ELD;
import static android.media.MediaRecorder.AudioEncoder.AMR_NB;
import static android.media.MediaRecorder.AudioEncoder.AMR_WB;
import static android.media.MediaRecorder.AudioEncoder.HE_AAC;
import static android.media.MediaRecorder.AudioEncoder.OPUS;
import static android.media.MediaRecorder.AudioEncoder.VORBIS;
import static android.media.MediaRecorder.VideoEncoder.AV1;
import static android.media.MediaRecorder.VideoEncoder.DOLBY_VISION;
import static android.media.MediaRecorder.VideoEncoder.H263;
import static android.media.MediaRecorder.VideoEncoder.H264;
import static android.media.MediaRecorder.VideoEncoder.HEVC;
import static android.media.MediaRecorder.VideoEncoder.MPEG_4_SP;
import static android.media.MediaRecorder.VideoEncoder.VP8;
import static android.media.MediaRecorder.VideoEncoder.VP9;

import android.media.CamcorderProfile;
import android.media.EncoderProfiles;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class EncoderProfilesProxyCompatBaseImpl {

    /** Creates an EncoderProfilesProxy instance from {@link CamcorderProfile}. */
    @NonNull
    public static EncoderProfilesProxy from(
            @NonNull CamcorderProfile camcorderProfile) {
        return ImmutableEncoderProfilesProxy.create(
                camcorderProfile.duration,
                camcorderProfile.fileFormat,
                toAudioProfiles(camcorderProfile),
                toVideoProfiles(camcorderProfile)
        );
    }

    /** Creates VideoProfileProxy instances from {@link CamcorderProfile}. */
    @NonNull
    private static List<VideoProfileProxy> toVideoProfiles(
            @NonNull CamcorderProfile camcorderProfile) {
        List<VideoProfileProxy> proxies = new ArrayList<>();
        proxies.add(VideoProfileProxy.create(
                camcorderProfile.videoCodec,
                getVideoCodecMimeType(camcorderProfile.videoCodec),
                camcorderProfile.videoBitRate,
                camcorderProfile.videoFrameRate,
                camcorderProfile.videoFrameWidth,
                camcorderProfile.videoFrameHeight,
                EncoderProfilesProxy.CODEC_PROFILE_NONE,
                VideoProfileProxy.BIT_DEPTH_8,
                EncoderProfiles.VideoProfile.YUV_420,
                EncoderProfiles.VideoProfile.HDR_NONE
        ));
        return proxies;
    }

    /** Creates AudioProfileProxy instances from {@link CamcorderProfile}. */
    @NonNull
    private static List<AudioProfileProxy> toAudioProfiles(
            @NonNull CamcorderProfile camcorderProfile) {
        List<AudioProfileProxy> proxies = new ArrayList<>();
        proxies.add(AudioProfileProxy.create(
                camcorderProfile.audioCodec,
                getAudioCodecMimeType(camcorderProfile.audioCodec),
                camcorderProfile.audioBitRate,
                camcorderProfile.audioSampleRate,
                camcorderProfile.audioChannels,
                getRequiredAudioProfile(camcorderProfile.audioCodec)
        ));
        return proxies;
    }

    /**
     * Returns a mime-type string for the given video codec type.
     *
     * @return A mime-type string or {@link VideoProfileProxy#MEDIA_TYPE_NONE} if the codec type is
     * {@link MediaRecorder.VideoEncoder#DEFAULT}, as this type is under-defined and cannot be
     * resolved to a specific mime type without more information.
     */
    @NonNull
    private static String getVideoCodecMimeType(
            @VideoProfileProxy.VideoEncoder int codec) {
        switch (codec) {
            // Mime-type definitions taken from
            // frameworks/av/media/libstagefright/foundation/MediaDefs.cpp
            case H263:
                return MediaFormat.MIMETYPE_VIDEO_H263;
            case H264:
                return MediaFormat.MIMETYPE_VIDEO_AVC;
            case HEVC:
                return MediaFormat.MIMETYPE_VIDEO_HEVC;
            case VP8:
                return MediaFormat.MIMETYPE_VIDEO_VP8;
            case MPEG_4_SP:
                return MediaFormat.MIMETYPE_VIDEO_MPEG4;
            case VP9:
                return MediaFormat.MIMETYPE_VIDEO_VP9;
            case DOLBY_VISION:
                return MediaFormat.MIMETYPE_VIDEO_DOLBY_VISION;
            case AV1:
                return MediaFormat.MIMETYPE_VIDEO_AV1;
            case MediaRecorder.VideoEncoder.DEFAULT:
                break;
        }

        return VideoProfileProxy.MEDIA_TYPE_NONE;
    }

    /**
     * Returns a mime-type string for the given audio codec type.
     *
     * @return A mime-type string or {@link AudioProfileProxy#MEDIA_TYPE_NONE} if the codec type is
     * {@link android.media.MediaRecorder.AudioEncoder#DEFAULT}, as this type is under-defined
     * and cannot be resolved to a specific mime type without more information.
     */
    @NonNull
    private static String getAudioCodecMimeType(@AudioProfileProxy.AudioEncoder int codec) {
        // Mime-type definitions taken from
        // frameworks/av/media/libstagefright/foundation/MediaDefs.cpp
        switch (codec) {
            case AAC: // Should use aac-profile LC
            case HE_AAC: // Should use aac-profile HE
            case AAC_ELD: // Should use aac-profile ELD
                return MediaFormat.MIMETYPE_AUDIO_AAC;
            case AMR_NB:
                return MediaFormat.MIMETYPE_AUDIO_AMR_NB;
            case AMR_WB:
                return MediaFormat.MIMETYPE_AUDIO_AMR_WB;
            case OPUS:
                return MediaFormat.MIMETYPE_AUDIO_OPUS;
            case VORBIS:
                return MediaFormat.MIMETYPE_AUDIO_VORBIS;
            case MediaRecorder.AudioEncoder.DEFAULT:
                break;
        }

        return AudioProfileProxy.MEDIA_TYPE_NONE;
    }

    /**
     * Returns the required audio profile for the given audio encoder.
     *
     * <p>For example, this can be used to differentiate between AAC encoders
     * {@link android.media.MediaRecorder.AudioEncoder#AAC},
     * {@link android.media.MediaRecorder.AudioEncoder#AAC_ELD},
     * and {@link android.media.MediaRecorder.AudioEncoder#HE_AAC}.
     * Should be used with the {@link MediaCodecInfo.CodecProfileLevel#profile} field.
     *
     * @return The profile required by the audio codec. If no profile is required, returns
     * {@link EncoderProfilesProxy#CODEC_PROFILE_NONE}.
     */
    private static int getRequiredAudioProfile(@AudioProfileProxy.AudioEncoder int codec) {
        switch (codec) {
            case AAC:
                return MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            case AAC_ELD:
                return MediaCodecInfo.CodecProfileLevel.AACObjectELD;
            case HE_AAC:
                return MediaCodecInfo.CodecProfileLevel.AACObjectHE;
            default:
                return EncoderProfilesProxy.CODEC_PROFILE_NONE;
        }
    }

    // Class should not be instantiated.
    private EncoderProfilesProxyCompatBaseImpl() {
    }
}
