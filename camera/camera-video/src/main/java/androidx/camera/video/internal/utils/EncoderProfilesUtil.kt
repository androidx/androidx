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
package androidx.camera.video.internal.utils

import android.util.Range
import android.util.Size
import androidx.camera.core.impl.EncoderProfilesProxy
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy
import androidx.camera.video.internal.config.VideoConfigUtil

/** Utility class for encoder profiles related operations. */
public object EncoderProfilesUtil {
    /**
     * Derives a VideoProfile from a base VideoProfile and a new resolution.
     *
     * Most fields are directly copied from the base VideoProfile except the bitrate, which will be
     * scaled and clamped according to the new resolution and the given bitrate range.
     *
     * @param baseVideoProfile the VideoProfile to derive.
     * @param newResolution the new resolution.
     * @param bitrateRangeToClamp the bitrate range to clamp. This is usually the supported bitrate
     *   range of the target codec.
     * @return a derived VideoProfile.
     */
    @JvmStatic
    public fun deriveVideoProfile(
        baseVideoProfile: VideoProfileProxy,
        newResolution: Size,
        bitrateRangeToClamp: Range<Int>,
    ): VideoProfileProxy {
        // "Guess" bit rate.
        var derivedBitrate =
            VideoConfigUtil.scaleBitrate(
                baseVideoProfile.bitrate,
                baseVideoProfile.bitDepth,
                baseVideoProfile.bitDepth,
                baseVideoProfile.frameRate,
                baseVideoProfile.frameRate,
                newResolution.width,
                baseVideoProfile.width,
                newResolution.height,
                baseVideoProfile.height,
            )
        derivedBitrate = bitrateRangeToClamp.clamp(derivedBitrate)

        return VideoProfileProxy.create(
            baseVideoProfile.codec,
            baseVideoProfile.mediaType,
            derivedBitrate,
            baseVideoProfile.frameRate,
            newResolution.width,
            newResolution.height,
            baseVideoProfile.profile,
            baseVideoProfile.bitDepth,
            baseVideoProfile.chromaSubsampling,
            baseVideoProfile.hdrFormat,
        )
    }

    /**
     * Gets the first VideoProfile from the given EncoderProfileProxy. Returns null if
     * encoderProfiles is null or there is no VideoProfile.
     */
    @JvmStatic
    public fun getFirstVideoProfile(encoderProfiles: EncoderProfilesProxy?): VideoProfileProxy? {
        return encoderProfiles?.getVideoProfiles()?.firstOrNull()
    }
}
