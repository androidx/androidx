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

package androidx.camera.video.internal.utils;

import android.util.Range;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.video.VideoSpec;
import androidx.camera.video.internal.config.VideoConfigUtil;

/** Utility class for encoder profiles related operations. */
@RequiresApi(21)
public class EncoderProfilesUtil {

    private EncoderProfilesUtil() {
    }

    /**
     * Derives a VideoProfile from a base VideoProfile and a new resolution.
     *
     * <p>Most fields are directly copied from the base VideoProfile except the bitrate, which will
     * be scaled and clamped according to the new resolution and the given bitrate range.
     *
     * @param baseVideoProfile    the VideoProfile to derive.
     * @param newResolution       the new resolution.
     * @param bitrateRangeToClamp the bitrate range to clamp. This is usually the supported
     *                            bitrate range of the target codec. Set
     *                            {@link VideoSpec#BITRATE_RANGE_AUTO} as no clamp required.
     * @return a derived VideoProfile.
     */
    @NonNull
    public static EncoderProfilesProxy.VideoProfileProxy deriveVideoProfile(
            @NonNull EncoderProfilesProxy.VideoProfileProxy baseVideoProfile,
            @NonNull Size newResolution,
            @NonNull Range<Integer> bitrateRangeToClamp) {

        // "Guess" bit rate.
        int derivedBitrate = VideoConfigUtil.scaleAndClampBitrate(
                baseVideoProfile.getBitrate(),
                baseVideoProfile.getBitDepth(), baseVideoProfile.getBitDepth(),
                baseVideoProfile.getFrameRate(), baseVideoProfile.getFrameRate(),
                newResolution.getWidth(), baseVideoProfile.getWidth(),
                newResolution.getHeight(), baseVideoProfile.getHeight(),
                bitrateRangeToClamp);

        return EncoderProfilesProxy.VideoProfileProxy.create(
                baseVideoProfile.getCodec(),
                baseVideoProfile.getMediaType(),
                derivedBitrate,
                baseVideoProfile.getFrameRate(),
                newResolution.getWidth(),
                newResolution.getHeight(),
                baseVideoProfile.getProfile(),
                baseVideoProfile.getBitDepth(),
                baseVideoProfile.getChromaSubsampling(),
                baseVideoProfile.getHdrFormat()
        );
    }

    /**
     * Gets the first VideoProfile from the given EncoderProfileProxy. Returns null if
     * encoderProfiles is null or there is no VideoProfile.
     */
    @Nullable
    public static EncoderProfilesProxy.VideoProfileProxy getFirstVideoProfile(
            @Nullable EncoderProfilesProxy encoderProfiles) {
        if (encoderProfiles != null && !encoderProfiles.getVideoProfiles().isEmpty()) {
            return encoderProfiles.getVideoProfiles().get(0);
        }
        return null;
    }
}
