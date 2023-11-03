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

import static androidx.camera.video.internal.utils.DynamicRangeUtil.isHdrSettingsMatched;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation that provides {@link EncoderProfilesProxy} containing video information
 * matched with the target {@link DynamicRange}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class DynamicRangeMatchedEncoderProfilesProvider implements EncoderProfilesProvider {

    private final EncoderProfilesProvider mEncoderProfilesProvider;
    private final DynamicRange mDynamicRange;
    private final Map<Integer, EncoderProfilesProxy> mEncoderProfilesCache = new HashMap<>();

    public DynamicRangeMatchedEncoderProfilesProvider(@NonNull EncoderProfilesProvider provider,
            @NonNull DynamicRange dynamicRange) {
        mEncoderProfilesProvider = provider;
        mDynamicRange = dynamicRange;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasProfile(int quality) {
        if (!mEncoderProfilesProvider.hasProfile(quality)) {
            return false;
        }

        return getProfilesInternal(quality) != null;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public EncoderProfilesProxy getAll(int quality) {
        return getProfilesInternal(quality);
    }

    @Nullable
    private EncoderProfilesProxy getProfilesInternal(int quality) {
        if (mEncoderProfilesCache.containsKey(quality)) {
            return mEncoderProfilesCache.get(quality);
        }

        EncoderProfilesProxy profiles = null;
        if (mEncoderProfilesProvider.hasProfile(quality)) {
            EncoderProfilesProxy baseProfiles = mEncoderProfilesProvider.getAll(quality);
            profiles = filterUnmatchedDynamicRange(baseProfiles, mDynamicRange);
            mEncoderProfilesCache.put(quality, profiles);
        }

        return profiles;
    }

    @Nullable
    private static EncoderProfilesProxy filterUnmatchedDynamicRange(
            @Nullable EncoderProfilesProxy encoderProfiles, @NonNull DynamicRange dynamicRange) {
        if (encoderProfiles == null) {
            return null;
        }

        List<VideoProfileProxy> validVideoProfiles = new ArrayList<>();
        for (VideoProfileProxy videoProfile : encoderProfiles.getVideoProfiles()) {
            if (isHdrSettingsMatched(videoProfile, dynamicRange)) {
                validVideoProfiles.add(videoProfile);
            }
        }

        return validVideoProfiles.isEmpty() ? null : ImmutableEncoderProfilesProxy.create(
                encoderProfiles.getDefaultDurationSeconds(),
                encoderProfiles.getRecommendedFileFormat(),
                encoderProfiles.getAudioProfiles(),
                validVideoProfiles
        );
    }
}
