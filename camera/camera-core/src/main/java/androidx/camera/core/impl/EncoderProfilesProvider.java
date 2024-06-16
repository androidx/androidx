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

import static android.media.CamcorderProfile.QUALITY_1080P;
import static android.media.CamcorderProfile.QUALITY_2160P;
import static android.media.CamcorderProfile.QUALITY_480P;
import static android.media.CamcorderProfile.QUALITY_4KDCI;
import static android.media.CamcorderProfile.QUALITY_720P;
import static android.media.CamcorderProfile.QUALITY_8KUHD;
import static android.media.CamcorderProfile.QUALITY_CIF;
import static android.media.CamcorderProfile.QUALITY_QCIF;
import static android.media.CamcorderProfile.QUALITY_QHD;
import static android.media.CamcorderProfile.QUALITY_QVGA;
import static android.media.CamcorderProfile.QUALITY_VGA;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import android.media.CamcorderProfile;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * EncoderProfilesProvider is used to obtain the {@link EncoderProfilesProxy}.
 */
public interface EncoderProfilesProvider {

    /**
     * Checks if the quality is supported on this device.
     *
     * <p>The quality should be one of quality constants defined in {@link CamcorderProfile}.
     */
    boolean hasProfile(int quality);

    /**
     * Gets the {@link EncoderProfilesProxy} if the quality is supported on the device.
     *
     * <p>The quality should be one of quality constants defined in {@link CamcorderProfile}.
     *
     * @see #hasProfile(int)
     */
    @Nullable
    EncoderProfilesProxy getAll(int quality);

    /** An implementation that contains no data. */
    EncoderProfilesProvider EMPTY = new EncoderProfilesProvider() {
        @Override
        public boolean hasProfile(int quality) {
            return false;
        }

        @Nullable
        @Override
        public EncoderProfilesProxy getAll(int quality) {
            return null;
        }
    };

    List<Integer> QUALITY_HIGH_TO_LOW = unmodifiableList(asList(
            QUALITY_8KUHD, // 7680x4320
            QUALITY_4KDCI, // 4096x2160
            QUALITY_2160P, // 3840x2160
            QUALITY_QHD, // 2560x1440
            QUALITY_1080P, // 1920x1080
            QUALITY_720P, // 1280x720
            QUALITY_480P, // 720x480
            QUALITY_VGA, // 640x480
            QUALITY_CIF, // 352x288
            QUALITY_QVGA, // 320x240
            QUALITY_QCIF // 176x144
    ));
}
