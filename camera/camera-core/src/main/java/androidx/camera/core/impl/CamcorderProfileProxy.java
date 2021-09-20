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

import android.media.CamcorderProfile;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.auto.value.AutoValue;

/**
 * CamcorderProfileProxy defines the get methods that is mapping to the fields of
 * {@link CamcorderProfile}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class CamcorderProfileProxy {

    /** Creates a CamcorderProfileProxy instance. */
    @NonNull
    public static CamcorderProfileProxy create(int duration,
            int quality,
            int fileFormat,
            int videoCodec,
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
}

