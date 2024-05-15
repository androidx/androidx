/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.video.internal.compat.quirk;

import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_HIGH;

import static androidx.camera.core.internal.utils.SizeUtil.RESOLUTION_480P;
import static androidx.camera.core.internal.utils.SizeUtil.getArea;
import static androidx.camera.video.internal.config.VideoConfigUtil.toVideoEncoderConfig;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.deriveVideoProfile;
import static androidx.camera.video.internal.utils.EncoderProfilesUtil.getFirstVideoProfile;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import android.os.Build;
import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.arch.core.util.Function;
import androidx.camera.core.impl.CameraInfoInternal;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.Quirk;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.encoder.VideoEncoderConfig;
import androidx.camera.video.internal.encoder.VideoEncoderInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>QuirkSummary
 *     Bug Id: b/311311853
 *     Description: MotoC doesn't have any supported Quality for front camera. The reason is
 *                  that the highest supported CamcorderProfile is QUALITY_CIF(352x288).
 *                  By experimental result, QUALITY_480P(720x480) can be used to record video so
 *                  we can add at least one supported quality.
 *                  In addition, MotoC only has two camera id, "0" for rear and "1" for front, so
 *                  it is feasible to simply check camera id "1" to create EncoderProfilesProxy.
 *     Device(s): moto c
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ExtraSupportedQualityQuirk implements Quirk {
    private static final String MOTO_C_FRONT_CAM_ID = "1";

    static boolean load() {
        return isMotoC();
    }

    private static boolean isMotoC() {
        return "motorola".equalsIgnoreCase(Build.BRAND) && "moto c".equalsIgnoreCase(Build.MODEL);
    }

    /** Gets the EncoderProfilesProxy for the extra supported quality. */
    @Nullable
    public Map<Integer, EncoderProfilesProxy> getExtraEncoderProfiles(
            @NonNull CameraInfoInternal cameraInfo,
            @NonNull EncoderProfilesProvider encoderProfilesProvider,
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder) {
        if (isMotoC()) {
            return getExtraEncoderProfilesForMotoC(cameraInfo, encoderProfilesProvider,
                    videoEncoderInfoFinder);
        }
        return emptyMap();
    }

    // Create 480P EncoderProfiles for front Camera if not exist. Derive profile from QUALITY_HIGH
    // which should be QUALITY_CIF. Even if QUALITY_HIGH is not QUALITY_CIF due to ROM
    // update, the code should still work.
    @Nullable
    private Map<Integer, EncoderProfilesProxy> getExtraEncoderProfilesForMotoC(
            @NonNull CameraInfoInternal cameraInfo,
            @NonNull EncoderProfilesProvider encoderProfilesProvider,
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder) {
        if (!MOTO_C_FRONT_CAM_ID.equals(cameraInfo.getCameraId())
                || encoderProfilesProvider.hasProfile(QUALITY_480P)) {
            return null;
        }

        // Derive from QUALITY_HIGH
        EncoderProfilesProxy profilesHigh = encoderProfilesProvider.getAll(QUALITY_HIGH);
        EncoderProfilesProxy.VideoProfileProxy videoProfileHigh =
                getFirstVideoProfile(profilesHigh);
        if (videoProfileHigh == null) {
            return null;
        }
        Range<Integer> supportedBitrateRange =
                getSupportedBitrateRange(videoProfileHigh, videoEncoderInfoFinder);
        EncoderProfilesProxy.VideoProfileProxy derivedVideoProfile =
                deriveVideoProfile(videoProfileHigh, RESOLUTION_480P, supportedBitrateRange);
        EncoderProfilesProxy profiles480p =
                EncoderProfilesProxy.ImmutableEncoderProfilesProxy.create(
                        profilesHigh.getDefaultDurationSeconds(),
                        profilesHigh.getRecommendedFileFormat(),
                        profilesHigh.getAudioProfiles(),
                        singletonList(derivedVideoProfile));

        // Return mapping
        Map<Integer, EncoderProfilesProxy> extraEncoderProfilesMap = new HashMap<>();
        extraEncoderProfilesMap.put(QUALITY_480P, profiles480p);
        // Update QUALITY_HIGH if necessary.
        Size sizeHigh = new Size(videoProfileHigh.getWidth(), videoProfileHigh.getHeight());
        if (getArea(RESOLUTION_480P) > getArea(sizeHigh)) {
            extraEncoderProfilesMap.put(QUALITY_HIGH, profiles480p);
        }
        return extraEncoderProfilesMap;
    }

    @NonNull
    private static Range<Integer> getSupportedBitrateRange(
            @NonNull EncoderProfilesProxy.VideoProfileProxy videoProfile,
            @NonNull Function<VideoEncoderConfig, VideoEncoderInfo> videoEncoderInfoFinder) {
        VideoEncoderInfo encoderInfo =
                videoEncoderInfoFinder.apply(toVideoEncoderConfig(videoProfile));
        return encoderInfo != null ? encoderInfo.getSupportedBitrateRange()
                : VideoSpec.BITRATE_RANGE_AUTO;
    }
}
