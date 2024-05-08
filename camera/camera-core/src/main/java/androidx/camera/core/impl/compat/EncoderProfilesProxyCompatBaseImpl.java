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

package androidx.camera.core.impl.compat;

import static androidx.camera.core.impl.EncoderProfilesProxy.getAudioCodecMimeType;
import static androidx.camera.core.impl.EncoderProfilesProxy.getRequiredAudioProfile;
import static androidx.camera.core.impl.EncoderProfilesProxy.getVideoCodecMimeType;

import android.media.CamcorderProfile;
import android.media.EncoderProfiles;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.AudioProfileProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.ImmutableEncoderProfilesProxy;
import androidx.camera.core.impl.EncoderProfilesProxy.VideoProfileProxy;

import java.util.ArrayList;
import java.util.List;

class EncoderProfilesProxyCompatBaseImpl {

    /** Creates an EncoderProfilesProxy instance from {@link CamcorderProfile}. */
    @NonNull
    public static EncoderProfilesProxy from(
            @NonNull CamcorderProfile camcorderProfile) {
        return ImmutableEncoderProfilesProxy.create(
                camcorderProfile.duration,
                camcorderProfile.fileFormat,
                toAudioProfiles(camcorderProfile),
                toVideoProfiles(camcorderProfile)
        );
    }

    /** Creates VideoProfileProxy instances from {@link CamcorderProfile}. */
    @NonNull
    private static List<VideoProfileProxy> toVideoProfiles(
            @NonNull CamcorderProfile camcorderProfile) {
        List<VideoProfileProxy> proxies = new ArrayList<>();
        proxies.add(VideoProfileProxy.create(
                camcorderProfile.videoCodec,
                getVideoCodecMimeType(camcorderProfile.videoCodec),
                camcorderProfile.videoBitRate,
                camcorderProfile.videoFrameRate,
                camcorderProfile.videoFrameWidth,
                camcorderProfile.videoFrameHeight,
                EncoderProfilesProxy.CODEC_PROFILE_NONE,
                VideoProfileProxy.BIT_DEPTH_8,
                EncoderProfiles.VideoProfile.YUV_420,
                EncoderProfiles.VideoProfile.HDR_NONE
        ));
        return proxies;
    }

    /** Creates AudioProfileProxy instances from {@link CamcorderProfile}. */
    @NonNull
    private static List<AudioProfileProxy> toAudioProfiles(
            @NonNull CamcorderProfile camcorderProfile) {
        List<AudioProfileProxy> proxies = new ArrayList<>();
        proxies.add(AudioProfileProxy.create(
                camcorderProfile.audioCodec,
                getAudioCodecMimeType(camcorderProfile.audioCodec),
                camcorderProfile.audioBitRate,
                camcorderProfile.audioSampleRate,
                camcorderProfile.audioChannels,
                getRequiredAudioProfile(camcorderProfile.audioCodec)
        ));
        return proxies;
    }

    // Class should not be instantiated.
    private EncoderProfilesProxyCompatBaseImpl() {
    }
}
