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

package androidx.camera.video.internal.config;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.video.internal.VideoValidatedEncoderProfilesProxy;
import androidx.camera.video.internal.encoder.EncoderConfig;

import com.google.auto.value.AutoValue;

/**
 * Data class containing information about a media mime.
 *
 * <p>The information included in this class can include the mime type, profile and any
 * compatible configuration types that can be used to resolve settings, such as
 * {@link VideoValidatedEncoderProfilesProxy}.
 */
@SuppressWarnings("NullableProblems") // Problem from AutoValue generated class.
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class MimeInfo {

    /** Returns the mime type. */
    @NonNull
    public abstract String getMimeType();

    /**
     * Returns the profile for the given mime.
     *
     * <p>The returned integer will generally come from
     * {@link android.media.MediaCodecInfo.CodecProfileLevel}, or if no profile is required,
     * {@link androidx.camera.video.internal.encoder.EncoderConfig#CODEC_PROFILE_NONE}.
     */
    public abstract int getProfile();

    /**
     * Returns compatible {@link VideoValidatedEncoderProfilesProxy} that can be used to resolve
     * settings.
     *
     * <p>If no EncoderProfiles is provided, returns {@code null}
     */
    @Nullable
    public abstract VideoValidatedEncoderProfilesProxy getCompatibleEncoderProfiles();

    /** Creates a builder for the given mime type */
    @NonNull
    public static Builder builder(@NonNull String mimeType) {
        return new AutoValue_MimeInfo.Builder()
                .setMimeType(mimeType)
                .setProfile(EncoderConfig.CODEC_PROFILE_NONE);
    }

    /** A Builder for a {@link androidx.camera.video.internal.config.MimeInfo} */
    @AutoValue.Builder
    public abstract static class Builder {

        // Package-private since this should be passed to builder factory method.
        @NonNull
        abstract Builder setMimeType(@NonNull String mimeType);

        /** Sets the mime profile */
        @NonNull
        public abstract Builder setProfile(int profile);

        /** Sets a compatible EncoderProfiles */
        @NonNull
        public abstract Builder setCompatibleEncoderProfiles(
                @Nullable VideoValidatedEncoderProfilesProxy encoderProfiles);

        /** Builds the {@link androidx.camera.video.internal.config.MimeInfo}. */
        @NonNull
        public abstract MimeInfo build();
    }

}
