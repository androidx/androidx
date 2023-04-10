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

package androidx.camera.video.internal;

import static java.util.Collections.unmodifiableList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.core.util.Preconditions;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;

/**
 * VideoValidatedEncoderProfilesProxy is an implementation of {@link EncoderProfilesProxy} that
 * guarantees to provide video information.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@AutoValue
public abstract class VideoValidatedEncoderProfilesProxy implements EncoderProfilesProxy {

    /** Creates a VideoValidatedEncoderProfilesProxy instance from {@link EncoderProfilesProxy}. */
    @NonNull
    public static VideoValidatedEncoderProfilesProxy from(@NonNull EncoderProfilesProxy profiles) {
        return create(
                profiles.getDefaultDurationSeconds(),
                profiles.getRecommendedFileFormat(),
                profiles.getAudioProfiles(),
                profiles.getVideoProfiles()
        );
    }

    /** Creates a VideoValidatedEncoderProfilesProxy instance. */
    @NonNull
    public static VideoValidatedEncoderProfilesProxy create(
            int defaultDurationSeconds,
            int recommendedFileFormat,
            @NonNull List<AudioProfileProxy> audioProfiles,
            @NonNull List<VideoProfileProxy> videoProfiles) {
        Preconditions.checkArgument(!videoProfiles.isEmpty(),
                "Should contain at least one VideoProfile.");
        VideoProfileProxy defaultVideoProfile = videoProfiles.get(0);

        AudioProfileProxy defaultAudioProfile = null;
        if (!audioProfiles.isEmpty()) {
            defaultAudioProfile = audioProfiles.get(0);
        }

        return new AutoValue_VideoValidatedEncoderProfilesProxy(
                defaultDurationSeconds,
                recommendedFileFormat,
                unmodifiableList(new ArrayList<>(audioProfiles)),
                unmodifiableList(new ArrayList<>(videoProfiles)),
                defaultAudioProfile,
                defaultVideoProfile
        );
    }

    /** Returns the default {@link AudioProfileProxy} or null if not existed. */
    @Nullable
    public abstract AudioProfileProxy getDefaultAudioProfile();

    /** Returns the default {@link VideoProfileProxy}. */
    @NonNull
    public abstract VideoProfileProxy getDefaultVideoProfile();
}
