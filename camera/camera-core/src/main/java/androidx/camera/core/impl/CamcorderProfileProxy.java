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

package androidx.camera.core.impl;

import static android.media.MediaRecorder.AudioEncoder.AAC;
import static android.media.MediaRecorder.AudioEncoder.AAC_ELD;
import static android.media.MediaRecorder.AudioEncoder.AMR_NB;
import static android.media.MediaRecorder.AudioEncoder.AMR_WB;
import static android.media.MediaRecorder.AudioEncoder.HE_AAC;
import static android.media.MediaRecorder.AudioEncoder.OPUS;
import static android.media.MediaRecorder.AudioEncoder.VORBIS;
import static android.media.MediaRecorder.VideoEncoder.H263;
import static android.media.MediaRecorder.VideoEncoder.H264;
import static android.media.MediaRecorder.VideoEncoder.HEVC;
import static android.media.MediaRecorder.VideoEncoder.MPEG_4_SP;
import static android.media.MediaRecorder.VideoEncoder.VP8;

import android.media.CamcorderProfile;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * CamcorderProfileProxy defines the get methods that is mapping to the fields of
 * {@link CamcorderProfile}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class CamcorderProfileProxy {

    /** Constant representing no codec profile. */
    public static int CODEC_PROFILE_NONE = -1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({H263, H264, HEVC, VP8, MPEG_4_SP, MediaRecorder.VideoEncoder.DEFAULT})
    @interface VideoEncoder {
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AAC, AAC_ELD, AMR_NB, AMR_WB, HE_AAC, OPUS, VORBIS,
            MediaRecorder.AudioEncoder.DEFAULT})
    @interface AudioEncoder {
    }

    /** Creates a CamcorderProfileProxy instance. */
    @NonNull
    public static CamcorderProfileProxy create(int duration,
            int quality,
            int fileFormat,
            @VideoEncoder int videoCodec,
            int videoBitRate,
            int videoFrameRate,
            int videoFrameWidth,
            int videoFrameHeight,
            @AudioEncoder int audioCodec,
            int audioBitRate,
            int audioSampleRate,
            int audioChannels) {
        return new AutoValue_CamcorderProfileProxy(
                duration,
                quality,
                fileFormat,
                videoCodec,
                videoBitRate,
                videoFrameRate,
                videoFrameWidth,
                videoFrameHeight,
                audioCodec,
                audioBitRate,
                audioSampleRate,
                audioChannels
        );
    }

    /** Creates a CamcorderProfileProxy instance from {@link CamcorderProfile}. */
    @NonNull
    public static CamcorderProfileProxy fromCamcorderProfile(
            @NonNull CamcorderProfile camcorderProfile) {
        return new AutoValue_CamcorderProfileProxy(
                camcorderProfile.duration,
                camcorderProfile.quality,
                camcorderProfile.fileFormat,
                camcorderProfile.videoCodec,
                camcorderProfile.videoBitRate,
                camcorderProfile.videoFrameRate,
                camcorderProfile.videoFrameWidth,
                camcorderProfile.videoFrameHeight,
                camcorderProfile.audioCodec,
                camcorderProfile.audioBitRate,
                camcorderProfile.audioSampleRate,
                camcorderProfile.audioChannels
        );
    }

    /** @see CamcorderProfile#duration */
    public abstract int getDuration();

    /** @see CamcorderProfile#quality */
    public abstract int getQuality();

    /** @see CamcorderProfile#fileFormat */
    public abstract int getFileFormat();

    /** @see CamcorderProfile#videoCodec */
    @VideoEncoder
    public abstract int getVideoCodec();

    /** @see CamcorderProfile#videoBitRate */
    public abstract int getVideoBitRate();

    /** @see CamcorderProfile#videoFrameRate */
    public abstract int getVideoFrameRate();

    /** @see CamcorderProfile#videoFrameWidth */
    public abstract int getVideoFrameWidth();

    /** @see CamcorderProfile#videoFrameHeight */
    public abstract int getVideoFrameHeight();

    /** @see CamcorderProfile#audioCodec */
    @AudioEncoder
    public abstract int getAudioCodec();

    /** @see CamcorderProfile#audioBitRate */
    public abstract int getAudioBitRate();

    /** @see CamcorderProfile#audioSampleRate */
    public abstract int getAudioSampleRate();

    /** @see CamcorderProfile#audioChannels */
    public abstract int getAudioChannels();

    /**
     * Returns a mime-type string for the video codec type returned by {@link #getVideoCodec()}.
     *
     * @return A mime-type string or {@code null} if the codec type is
     * {@link android.media.MediaRecorder.VideoEncoder#DEFAULT}, as this type is under-defined
     * and cannot be resolved to a specific mime type without more information.
     */
    @Nullable
    public String getVideoCodecMimeType() {
        // Mime-type definitions taken from
        // frameworks/av/media/libstagefright/foundation/MediaDefs.cpp
        switch (getVideoCodec()) {
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
            case MediaRecorder.VideoEncoder.DEFAULT:
                break;
        }

        return null;
    }

    /**
     * Returns a mime-type string for the audio codec type returned by {@link #getAudioCodec()}.
     *
     * @return A mime-type string or {@code null} if the codec type is
     * {@link android.media.MediaRecorder.AudioEncoder#DEFAULT}, as this type is under-defined
     * and cannot be resolved to a specific mime type without more information.
     */
    @Nullable
    public String getAudioCodecMimeType() {
        // Mime-type definitions taken from
        // frameworks/av/media/libstagefright/foundation/MediaDefs.cpp
        switch (getAudioCodec()) {
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

        return null;
    }

    /**
     * Returns the required audio profile for the audio encoder given by {@link #getAudioCodec()}.
     *
     * <p>For example, this can be used to differentiate between AAC encoders
     * {@link android.media.MediaRecorder.AudioEncoder#AAC},
     * {@link android.media.MediaRecorder.AudioEncoder#AAC_ELD},
     * and {@link android.media.MediaRecorder.AudioEncoder#HE_AAC}.
     * Should be used with the {@link MediaCodecInfo.CodecProfileLevel#profile} field.
     *
     * @return The profile required by the audio codec. If no profile is required, returns
     * {@link #CODEC_PROFILE_NONE}.
     */
    public int getRequiredAudioProfile() {
        switch (getAudioCodec()) {
            case AAC:
                return MediaCodecInfo.CodecProfileLevel.AACObjectLC;
            case AAC_ELD:
                return MediaCodecInfo.CodecProfileLevel.AACObjectELD;
            case HE_AAC:
                return MediaCodecInfo.CodecProfileLevel.AACObjectHE;
            default:
                return CODEC_PROFILE_NONE;
        }
    }
}
