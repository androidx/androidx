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

import static android.media.EncoderProfiles.VideoProfile.HDR_DOLBY_VISION;
import static android.media.EncoderProfiles.VideoProfile.HDR_HDR10;
import static android.media.EncoderProfiles.VideoProfile.HDR_HDR10PLUS;
import static android.media.EncoderProfiles.VideoProfile.HDR_HLG;
import static android.media.EncoderProfiles.VideoProfile.HDR_NONE;

import static androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy.BIT_DEPTH_10;
import static androidx.camera.core.impl.EncoderProfilesProxy.getVideoCodecMimeType;
import static androidx.camera.video.internal.config.VideoConfigUtil.toVideoEncoderConfig;

import android.media.MediaCodecInfo;
import android.media.MediaRecorder;
import android.util.Rational;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.arch.core.util.Function;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class BackupHdrProfileEncoderProfilesProvider implements EncoderProfilesProvider {

    private static final String TAG = "BackupHdrProfileEncoderProfilesProvider";

    private final EncoderProfilesProvider mEncoderProfilesProvider;
    private final Function<VideoEncoderConfig, VideoEncoderInfo> mVideoEncoderInfoFinder;
    private final Map<Integer, EncoderProfilesProxy> mEncoderProfilesCache = new HashMap<>();

    /**
     * Creates a BackupHdrProfileEncoderProfilesProvider.
     *
     * @param provider               the {@link EncoderProfilesProvider}.
     * @param videoEncoderInfoFinder a {@link Function} to find a VideoEncoderInfo from a
     *                               VideoEncoderConfig.
     */
    public BackupHdrProfileEncoderProfilesProvider(@NonNull EncoderProfilesProvider provider,
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder) {
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

            // In the initial version, only backup HLG10 profile is appended.
            profiles = appendBackupVideoProfile(baseProfiles, HDR_HLG, BIT_DEPTH_10);
            mEncoderProfilesCache.put(quality, profiles);
        }

        return profiles;
    }

    @Nullable
    private EncoderProfilesProxy appendBackupVideoProfile(
            @Nullable EncoderProfilesProxy baseProfiles, int targetHdrFormat, int targetBitDepth) {
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
        VideoProfileProxy backupProfile = generateBackupProfile(sdrProfile, targetHdrFormat,
                targetBitDepth);

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

    @Nullable
    private static VideoProfileProxy generateBackupProfile(@Nullable VideoProfileProxy baseProfile,
            int targetHdrFormat, int targetBitDepth) {
        if (baseProfile == null) {
            return null;
        }

        // "Guess" codec, media type and profile.
        int derivedCodec = baseProfile.getCodec();
        String derivedMediaType = baseProfile.getMediaType();
        int derivedProfile = baseProfile.getProfile();
        if (targetHdrFormat != baseProfile.getHdrFormat()) {
            derivedCodec = deriveCodec(targetHdrFormat);
            derivedMediaType = deriveMediaType(derivedCodec);
            derivedProfile = deriveProfile(targetHdrFormat);
        }

        // "Guess" bit rate.
        int derivedBitrate = scaleBitrate(baseProfile.getBitrate(), targetBitDepth,
                baseProfile.getBitDepth());

        return VideoProfileProxy.create(
                derivedCodec,
                derivedMediaType,
                derivedBitrate,
                baseProfile.getFrameRate(),
                baseProfile.getWidth(),
                baseProfile.getHeight(),
                derivedProfile,
                targetBitDepth,
                baseProfile.getChromaSubsampling(),
                targetHdrFormat
        );
    }

    private static @VideoProfileProxy.VideoEncoder int deriveCodec(int hdrFormat) {
        switch (hdrFormat) {
            case HDR_NONE:
            case HDR_HLG:
            case HDR_HDR10:
            case HDR_HDR10PLUS:
            case HDR_DOLBY_VISION:
                return MediaRecorder.VideoEncoder.HEVC;
            default:
                throw new IllegalArgumentException("Unexpected HDR format: " + hdrFormat);
        }
    }

    private static int deriveProfile(int hdrFormat) {
        switch (hdrFormat) {
            case HDR_NONE:
                return MediaCodecInfo.CodecProfileLevel.HEVCProfileMain;
            case HDR_HLG:
                return MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10;
            case HDR_HDR10:
                return MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10;
            case HDR_HDR10PLUS:
                return MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus;
            case HDR_DOLBY_VISION:
                return EncoderProfilesProxy.CODEC_PROFILE_NONE;
            default:
                throw new IllegalArgumentException("Unexpected HDR format: " + hdrFormat);
        }
    }

    @NonNull
    private static String deriveMediaType(@VideoProfileProxy.VideoEncoder int codec) {
        return getVideoCodecMimeType(codec);
    }

    /**
     * Scale bit depth to match new bit depth.
     */
    private static int scaleBitrate(int baseBitrate, int actualBitDepth, int baseBitDepth) {
        if (actualBitDepth == baseBitDepth) {
            return baseBitrate;
        }

        Rational bitDepthRatio = new Rational(actualBitDepth, baseBitDepth);
        int resolvedBitrate = (int) (baseBitrate * bitDepthRatio.doubleValue());

        if (Logger.isDebugEnabled(TAG)) {
            String debugString = String.format("Base Bitrate(%dbps) * Bit Depth Ratio (%d / %d) "
                            + "= %d", baseBitrate, actualBitDepth, baseBitDepth, resolvedBitrate);
            Logger.d(TAG, debugString);
        }

        return resolvedBitrate;
    }

    /**
     * Check if any encoder supports the video profile and adapt the bitrate if possible. A null
     * will be returned if the video profile is not able to support.
     */
    @VisibleForTesting
    @Nullable
    static VideoProfileProxy validateOrAdapt(@Nullable VideoProfileProxy profile,
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder) {
        if (profile == null) {
            return null;
        }
        VideoEncoderConfig videoEncoderConfig = toVideoEncoderConfig(profile);
        VideoEncoderInfo videoEncoderInfo = videoEncoderInfoFinder.apply(videoEncoderConfig);
        if (videoEncoderInfo == null
                || !videoEncoderInfo.isSizeSupportedAllowSwapping(profile.getWidth(),
                profile.getHeight())) {
            return null;
        }
        int baseBitrate = videoEncoderConfig.getBitrate();
        int newBitrate = videoEncoderInfo.getSupportedBitrateRange().clamp(baseBitrate);
        return newBitrate == baseBitrate ? profile : modifyBitrate(profile, newBitrate);
    }

    @NonNull
    private static VideoProfileProxy modifyBitrate(@NonNull VideoProfileProxy baseProfile,
            int newBitrate) {
        return VideoProfileProxy.create(
                baseProfile.getCodec(),
                baseProfile.getMediaType(),
                newBitrate,
                baseProfile.getFrameRate(),
                baseProfile.getWidth(),
                baseProfile.getHeight(),
                baseProfile.getProfile(),
                baseProfile.getBitDepth(),
                baseProfile.getChromaSubsampling(),
                baseProfile.getHdrFormat()
        );
    }
}
