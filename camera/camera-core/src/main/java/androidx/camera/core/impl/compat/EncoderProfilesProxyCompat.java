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

import android.media.CamcorderProfile;
import android.media.EncoderProfiles;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProxy;

/**
 * Helper for accessing features of {@link EncoderProfiles} and {@link CamcorderProfile} in a
 * backwards compatible fashion.
 */
public final class EncoderProfilesProxyCompat {

    private static final String TAG = "EncoderProfilesProxyCompat";

    /** Creates an EncoderProfilesProxy instance from {@link EncoderProfiles}. */
    @RequiresApi(31)
    @NonNull
    public static EncoderProfilesProxy from(@NonNull EncoderProfiles encoderProfiles) {
        if (Build.VERSION.SDK_INT >= 33) {
            return EncoderProfilesProxyCompatApi33Impl.from(encoderProfiles);
        } else if (Build.VERSION.SDK_INT >= 31) {
            return EncoderProfilesProxyCompatApi31Impl.from(encoderProfiles);
        } else {
            throw new RuntimeException(
                    "Unable to call from(EncoderProfiles) on API " + Build.VERSION.SDK_INT
                            + ". Version 31 or higher required.");
        }
    }

    /** Creates an EncoderProfilesProxy instance from {@link CamcorderProfile}. */
    @NonNull
    public static EncoderProfilesProxy from(@NonNull CamcorderProfile camcorderProfile) {
        if (Build.VERSION.SDK_INT >= 31) {
            Logger.w(TAG, "Should use from(EncoderProfiles) on API " + Build.VERSION.SDK_INT
                    + "instead. CamcorderProfile is deprecated on API 31.");
        }

        return EncoderProfilesProxyCompatBaseImpl.from(camcorderProfile);
    }

    // Class should not be instantiated.
    private EncoderProfilesProxyCompat() {
    }
}
