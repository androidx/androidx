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

package androidx.camera.core.impl;

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

import static java.util.Collections.unmodifiableList;

import android.media.EncoderProfiles;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * EncoderProfilesProxy defines the get methods that is mapping to the fields of
 * {@link EncoderProfiles}.
 */
public interface EncoderProfilesProxy {

    /** Constant representing no codec profile. */
    int CODEC_PROFILE_NONE = -1;

    /** @see EncoderProfiles#getDefaultDurationSeconds() */
    int getDefaultDurationSeconds();

    /** @see EncoderProfiles#getRecommendedFileFormat() */
    int getRecommendedFileFormat();

    /** @see EncoderProfiles#getAudioProfiles() */
    @NonNull
    List<AudioProfileProxy> getAudioProfiles();

    /** @see EncoderProfiles#getVideoProfiles() */
    @NonNull
    List<VideoProfileProxy> getVideoProfiles();

    /**
     * VideoProfileProxy defines the get methods that is mapping to the fields of
     * {@link EncoderProfiles.VideoProfile}.
     */
    @AutoValue
    abstract class VideoProfileProxy {

        /** Constant representing no media type. */
        public static final String MEDIA_TYPE_NONE = "video/none";

        /** Constant representing bit depth 8. */
        public static final int BIT_DEPTH_8 = 8;

        /** Constant representing bit depth 10. */
        public static final int BIT_DEPTH_10 = 10;

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({H263, H264, HEVC, VP8, MPEG_4_SP, VP9, DOLBY_VISION, AV1,
                MediaRecorder.VideoEncoder.DEFAULT})
        public @interface VideoEncoder {
        }

        /** Creates a VideoProfileProxy instance. */
        @NonNull
        public static VideoProfileProxy create(
                @VideoEncoder int codec,
                @NonNull String mediaType,
                int bitrate,
                int frameRate,
                int width,
                int height,
                int profile,
                int bitDepth,
                int chromaSubsampling,
                int hdrFormat) {
            return new AutoValue_EncoderProfilesProxy_VideoProfileProxy(
                    codec,
                    mediaType,
                    bitrate,
                    frameRate,
                    width,
                    height,
                    profile,
                    bitDepth,
                    chromaSubsampling,
                    hdrFormat
            );
        }

        /** @see EncoderProfiles.VideoProfile#getCodec() */
        @VideoEncoder
        public abstract int getCodec();

        /** @see EncoderProfiles.VideoProfile#getMediaType() */
        @NonNull
        public abstract String getMediaType();

        /** @see EncoderProfiles.VideoProfile#getBitrate() */
        public abstract int getBitrate();

        /** @see EncoderProfiles.VideoProfile#getFrameRate() */
        public abstract int getFrameRate();

        /** @see EncoderProfiles.VideoProfile#getWidth() */
        public abstract int getWidth();

        /** @see EncoderProfiles.VideoProfile#getHeight() */
        public abstract int getHeight();

        /** @see EncoderProfiles.VideoProfile#getProfile() */
        public abstract int getProfile();

        /** @see EncoderProfiles.VideoProfile#getBitDepth() */
        public abstract int getBitDepth();

        /** @see EncoderProfiles.VideoProfile#getChromaSubsampling() */
        public abstract int getChromaSubsampling();

        /** @see EncoderProfiles.VideoProfile#getHdrFormat() */
        public abstract int getHdrFormat();
    }

    /**
     * AudioProfileProxy defines the get methods that is mapping to the fields of
     * {@link EncoderProfiles.AudioProfile}.
     */
    @AutoValue
    abstract class AudioProfileProxy {

        /** Constant representing no media type. */
        public static final String MEDIA_TYPE_NONE = "audio/none";

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({AAC, AAC_ELD, AMR_NB, AMR_WB, HE_AAC, OPUS, VORBIS,
                MediaRecorder.AudioEncoder.DEFAULT})
        public @interface AudioEncoder {
        }

        /** Creates an AudioProfileProxy instance. */
        @NonNull
        public static AudioProfileProxy create(
                @AudioEncoder int codec,
                @NonNull String mediaType,
                int bitRate,
                int sampleRate,
                int channels,
                int profile) {
            return new AutoValue_EncoderProfilesProxy_AudioProfileProxy(
                    codec,
                    mediaType,
                    bitRate,
                    sampleRate,
                    channels,
                    profile
            );
        }

        /** @see EncoderProfiles.AudioProfile#getCodec() */
        @AudioEncoder
        public abstract int getCodec();

        /** @see EncoderProfiles.AudioProfile#getMediaType() */
        @NonNull
        public abstract String getMediaType();

        /** @see EncoderProfiles.AudioProfile#getBitrate() */
        public abstract int getBitrate();

        /** @see EncoderProfiles.AudioProfile#getSampleRate() */
        public abstract int getSampleRate();

        /** @see EncoderProfiles.AudioProfile#getChannels() */
        public abstract int getChannels();

        /** @see EncoderProfiles.AudioProfile#getProfile() */
        public abstract int getProfile();
    }

    /**
     * An implementation of {@link EncoderProfilesProxy} that is immutable.
     */
    @AutoValue
    abstract class ImmutableEncoderProfilesProxy implements EncoderProfilesProxy {

        /** Creates an EncoderProfilesProxy instance. */
        @NonNull
        public static ImmutableEncoderProfilesProxy create(
                int defaultDurationSeconds,
                int recommendedFileFormat,
                @NonNull List<AudioProfileProxy> audioProfiles,
                @NonNull List<VideoProfileProxy> videoProfiles) {
            return new AutoValue_EncoderProfilesProxy_ImmutableEncoderProfilesProxy(
                    defaultDurationSeconds,
                    recommendedFileFormat,
                    unmodifiableList(new ArrayList<>(audioProfiles)),
                    unmodifiableList(new ArrayList<>(videoProfiles))
            );
        }
    }

    /**
     * Returns a mime-type string for the given video codec type.
     *
     * @return A mime-type string or {@link VideoProfileProxy#MEDIA_TYPE_NONE} if the codec type is
     * {@link MediaRecorder.VideoEncoder#DEFAULT}, as this type is under-defined and cannot be
     * resolved to a specific mime type without more information.
     */
    @NonNull
    static String getVideoCodecMimeType(
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
    static String getAudioCodecMimeType(@AudioProfileProxy.AudioEncoder int codec) {
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
    static int getRequiredAudioProfile(@AudioProfileProxy.AudioEncoder int codec) {
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
}
