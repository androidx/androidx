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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.quirk.ProfileResolutionQuirk;

import java.util.List;

/**
 * An implementation that provides the {@link EncoderProfilesProxy} whose video resolutions are
 * validated.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ResolutionValidatedEncoderProfilesProvider implements EncoderProfilesProvider {

    private final EncoderProfilesProvider mProvider;
    private final EncoderProfilesResolutionValidator mEncoderProfilesResolutionValidator;

    public ResolutionValidatedEncoderProfilesProvider(@NonNull EncoderProfilesProvider provider,
            @NonNull Quirks quirks) {
        mProvider = provider;
        List<ProfileResolutionQuirk> resolutionQuirks = quirks.getAll(ProfileResolutionQuirk.class);
        mEncoderProfilesResolutionValidator = new EncoderProfilesResolutionValidator(
                resolutionQuirks);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasProfile(int quality) {
        if (!mProvider.hasProfile(quality)) {
            return false;
        }

        if (mEncoderProfilesResolutionValidator.hasQuirk()) {
            EncoderProfilesProxy profiles = mProvider.getAll(quality);
            return mEncoderProfilesResolutionValidator.hasValidVideoResolution(profiles);
        }

        return true;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public EncoderProfilesProxy getAll(int quality) {
        if (!mProvider.hasProfile(quality)) {
            return null;
        }

        EncoderProfilesProxy profiles = mProvider.getAll(quality);
        if (mEncoderProfilesResolutionValidator.hasQuirk()) {
            profiles = mEncoderProfilesResolutionValidator.filterInvalidVideoResolution(profiles);
        }

        return profiles;
    }
}
