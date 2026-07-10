/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.encoder;

import android.media.MediaFormat;

import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.Timebase;

import org.jspecify.annotations.NonNull;

/**
 * The configuration represents the required parameters to configure an encoder.
 *
 * <p>An {@code EncoderConfig} is used to configure an {@link Encoder}.
 */
public interface EncoderConfig {

    /** Constant corresponding to no profile for the encoder */
    int CODEC_PROFILE_NONE = EncoderProfilesProxy.CODEC_PROFILE_NONE;

    /**
     * The mime type of the encoder.
     *
     * <p>For example, "video/avc" for a video encoder and "audio/mp4a-latm" for an audio encoder.
     *
     * @see MediaFormat
     */
    @NonNull String getMimeType();

    /**
     * The (optional) profile for the mime type returned by {@link #getMimeType()}.
     *
     * <p>For example, for AAC-ELD (enhanced low delay) audio encoder, the mime type is
     * "audio/mp4a-latm" and the profile needs to be
     * {@link android.media.MediaCodecInfo.CodecProfileLevel#AACObjectELD}.
     *
     * <p>Not all mime types require a profile, so this is optional.
     */
    int getProfile();

    /**
     * Gets the input timebase.
     */
    @NonNull Timebase getInputTimebase();

    /**
     * Transfers the config to a {@link MediaFormat}.
     *
     * @return the result {@link MediaFormat}
     */
    @NonNull MediaFormat toMediaFormat() throws InvalidConfigException;
}
