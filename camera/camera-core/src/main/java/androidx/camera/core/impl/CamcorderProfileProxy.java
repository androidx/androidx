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

import static android.media.MediaRecorder.VideoEncoder.DEFAULT;
import static android.media.MediaRecorder.VideoEncoder.H263;
import static android.media.MediaRecorder.VideoEncoder.H264;
import static android.media.MediaRecorder.VideoEncoder.HEVC;
import static android.media.MediaRecorder.VideoEncoder.MPEG_4_SP;
import static android.media.MediaRecorder.VideoEncoder.VP8;

import android.media.CamcorderProfile;
import android.media.MediaFormat;

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

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({H263, H264, HEVC, VP8, MPEG_4_SP, DEFAULT})
    @interface VideoEncoder {
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
            int audioCodec,
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
            case DEFAULT:
                break;
        }

        return null;
    }
}
