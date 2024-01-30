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

package androidx.camera.video.internal.config;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.video.internal.encoder.EncoderConfig;

import com.google.auto.value.AutoValue;

/**
 * Data class containing information about a audio mime.
 *
 * <p>The information includes all information from {@link MimeInfo} as well as
 * compatible configuration types that can be used to resolve settings, such as
 * {@link EncoderProfilesProxy.AudioProfileProxy}.
 */
@SuppressWarnings("NullableProblems") // Problem from AutoValue generated class.
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class AudioMimeInfo extends MimeInfo {

    /**
     * Returns compatible {@link EncoderProfilesProxy.AudioProfileProxy} that can be used to
     * resolve settings.
     *
     * <p>If no AudioProfileProxy is provided, returns {@code null}.
     */
    @Nullable
    public abstract EncoderProfilesProxy.AudioProfileProxy getCompatibleAudioProfile();

    /** Creates a builder for the given mime type */
    @NonNull
    public static AudioMimeInfo.Builder builder(@NonNull String mimeType) {
        return new AutoValue_AudioMimeInfo.Builder()
                .setMimeType(mimeType)
                .setProfile(EncoderConfig.CODEC_PROFILE_NONE);
    }

    /** A Builder for an {@link AudioMimeInfo}. */
    @SuppressWarnings("NullableProblems") // Problem from AutoValue generated class.
    @AutoValue.Builder
    public abstract static class Builder extends MimeInfo.Builder<Builder> {
        /** Sets a compatible {@link EncoderProfilesProxy.AudioProfileProxy} */
        @NonNull
        public abstract Builder setCompatibleAudioProfile(
                @Nullable EncoderProfilesProxy.AudioProfileProxy audioProfile);

        /** Builds a AudioMimeInfo. */
        @Override
        @NonNull
        public abstract AudioMimeInfo build();
    }
}
