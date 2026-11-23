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

import static android.media.EncoderProfiles.VideoProfile.HDR_NONE;

import static androidx.camera.video.internal.utils.EncoderProfilesUtil.deriveHdrVideoProfile;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.validateOrAdapt;

import androidx.camera.core.DynamicRange;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An implementation that provides the {@link EncoderProfilesProxy} with backup HDR video
 * information added.
 *
 * <p>Since there are issues that device supports HLG recording via Camera2 and MediaCodec, but has
 * no available HDR {@link VideoProfileProxy}. To handle these types of issues more generally, a
 * backup HDR {@link VideoProfileProxy} is added in case it's needed.
 *
 * <p>The class attempts to derive a HDR {@link VideoProfileProxy} from the SDR profile under the
 * same quality and adds the derived profile to the provided {@link EncoderProfilesProxy} if it is
 * verified by the {@code validator} passed to the constructor.
 */
public class BackupHdrProfileEncoderProfilesProvider implements EncoderProfilesProvider {

    private final EncoderProfilesProvider mEncoderProfilesProvider;
    private final VideoEncoderInfo.Finder mVideoEncoderInfoFinder;
    private final Map<Integer, EncoderProfilesProxy> mEncoderProfilesCache = new HashMap<>();

    /**
     * Creates a BackupHdrProfileEncoderProfilesProvider.
     *
     * @param provider               the {@link EncoderProfilesProvider}.
     * @param videoEncoderInfoFinder a {@link VideoEncoderInfo.Finder} to find a VideoEncoderInfo.
     */
    public BackupHdrProfileEncoderProfilesProvider(@NonNull EncoderProfilesProvider provider,
            VideoEncoderInfo.@NonNull Finder videoEncoderInfoFinder) {
        mEncoderProfilesProvider = provider;
        mVideoEncoderInfoFinder = videoEncoderInfoFinder;
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
    @Override
    public @Nullable EncoderProfilesProxy getAll(int quality) {
        return getProfilesInternal(quality);
    }

    private @Nullable EncoderProfilesProxy getProfilesInternal(int quality) {
        if (mEncoderProfilesCache.containsKey(quality)) {
            return mEncoderProfilesCache.get(quality);
        }

        EncoderProfilesProxy profiles = null;
        if (mEncoderProfilesProvider.hasProfile(quality)) {
            EncoderProfilesProxy baseProfiles = mEncoderProfilesProvider.getAll(quality);

            // In the initial version, only backup HLG10 profile is appended.
            profiles = appendBackupVideoProfile(baseProfiles, DynamicRange.HLG_10_BIT);
            mEncoderProfilesCache.put(quality, profiles);
        }

        return profiles;
    }

    private @Nullable EncoderProfilesProxy appendBackupVideoProfile(
            @Nullable EncoderProfilesProxy baseProfiles, @NonNull DynamicRange targetDynamicRange) {
        if (baseProfiles == null) {
            return null;
        }

        List<VideoProfileProxy> videoProfiles = new ArrayList<>(baseProfiles.getVideoProfiles());

        // Find SDR profile and generate backup profile.
        VideoProfileProxy sdrProfile = null;
        for (VideoProfileProxy videoProfile : baseProfiles.getVideoProfiles()) {
            if (videoProfile.getHdrFormat() == HDR_NONE) {
                sdrProfile = videoProfile;
                break;
            }
        }
        VideoProfileProxy backupProfile = sdrProfile == null ? null
                : deriveHdrVideoProfile(sdrProfile, targetDynamicRange);

        // Check if the media codec supports the generated backup profile and adapt bitrate if
        // possible.
        backupProfile = validateOrAdapt(backupProfile, mVideoEncoderInfoFinder);

        if (backupProfile != null) {
            videoProfiles.add(backupProfile);
        }

        return videoProfiles.isEmpty() ? null : ImmutableEncoderProfilesProxy.create(
                baseProfiles.getDefaultDurationSeconds(),
                baseProfiles.getRecommendedFileFormat(),
                baseProfiles.getAudioProfiles(),
                videoProfiles
        );
    }
}
