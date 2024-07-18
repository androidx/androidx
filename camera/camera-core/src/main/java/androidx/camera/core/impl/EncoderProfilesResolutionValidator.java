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

import android.media.EncoderProfiles;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.core.impl.quirk.ProfileResolutionQuirk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates the video resolution of {@link EncoderProfiles}.
 *
 * @see ProfileResolutionQuirk
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class EncoderProfilesResolutionValidator {

    @NonNull
    private final List<ProfileResolutionQuirk> mQuirks;
    @NonNull
    private final Set<Size> mSupportedResolutions;

    public EncoderProfilesResolutionValidator(@Nullable List<ProfileResolutionQuirk> quirks) {
        mQuirks = new ArrayList<>();
        if (quirks != null) {
            mQuirks.addAll(quirks);
        }

        mSupportedResolutions = generateSupportedResolutions(quirks);
    }

    @NonNull
    private Set<Size> generateSupportedResolutions(@Nullable List<ProfileResolutionQuirk> quirks) {
        if (quirks == null || quirks.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Size> supportedResolutions = new HashSet<>(quirks.get(0).getSupportedResolutions());
        for (int i = 1; i < quirks.size(); i++) {
            supportedResolutions.retainAll(quirks.get(i).getSupportedResolutions());
        }

        return supportedResolutions;
    }

    /** Checks if this validator contains quirk. */
    public boolean hasQuirk() {
        return !mQuirks.isEmpty();
    }

    /** Checks if any video resolution of EncoderProfiles is valid. */
    public boolean hasValidVideoResolution(@Nullable EncoderProfilesProxy profiles) {
        if (profiles == null) {
            return false;
        }

        if (!hasQuirk()) {
            return !profiles.getVideoProfiles().isEmpty();
        }

        boolean hasValidResolution = false;
        for (VideoProfileProxy videoProfile : profiles.getVideoProfiles()) {
            Size videoSize = new Size(videoProfile.getWidth(), videoProfile.getHeight());
            if (mSupportedResolutions.contains(videoSize)) {
                hasValidResolution = true;
                break;
            }
        }

        return hasValidResolution;
    }

    /** Returns an {@link EncoderProfilesProxy} that filters out invalid resolutions. */
    @Nullable
    public EncoderProfilesProxy filterInvalidVideoResolution(
            @Nullable EncoderProfilesProxy profiles) {
        if (profiles == null) {
            return null;
        }

        if (!hasQuirk()) {
            return profiles;
        }

        List<VideoProfileProxy> validVideoProfiles = new ArrayList<>();
        for (VideoProfileProxy videoProfile : profiles.getVideoProfiles()) {
            Size videoSize = new Size(videoProfile.getWidth(), videoProfile.getHeight());
            if (mSupportedResolutions.contains(videoSize)) {
                validVideoProfiles.add(videoProfile);
            }
        }

        return validVideoProfiles.isEmpty() ? null : ImmutableEncoderProfilesProxy.create(
                profiles.getDefaultDurationSeconds(),
                profiles.getRecommendedFileFormat(),
                profiles.getAudioProfiles(),
                validVideoProfiles
        );
    }
}
