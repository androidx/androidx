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

package androidx.camera.camera2.internal;

import android.media.CamcorderProfile;
import android.media.EncoderProfiles;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.InvalidVideoProfilesQuirk;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;
import androidx.camera.core.impl.compat.EncoderProfilesProxyCompat;

import java.util.HashMap;
import java.util.Map;

/** An implementation that provides the {@link EncoderProfilesProxy}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Camera2EncoderProfilesProvider implements EncoderProfilesProvider {

    private static final String TAG = "Camera2EncoderProfilesProvider";

    private final boolean mHasValidCameraId;
    private final String mCameraId;
    private final int mIntCameraId;
    private final Map<Integer, EncoderProfilesProxy> mEncoderProfilesCache = new HashMap<>();

    public Camera2EncoderProfilesProvider(@NonNull String cameraId) {
        mCameraId = cameraId;
        boolean hasValidCameraId = false;
        int intCameraId = -1;
        try {
            intCameraId = Integer.parseInt(cameraId);
            hasValidCameraId = true;
        } catch (NumberFormatException e) {
            Logger.w(TAG, "Camera id is not an integer: " + cameraId
                    + ", unable to create Camera2EncoderProfilesProvider");
        }
        mHasValidCameraId = hasValidCameraId;
        mIntCameraId = intCameraId;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasProfile(int quality) {
        if (!mHasValidCameraId) {
            return false;
        }

        return CamcorderProfile.hasProfile(mIntCameraId, quality);
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public EncoderProfilesProxy getAll(int quality) {
        if (!mHasValidCameraId) {
            return null;
        }

        if (!CamcorderProfile.hasProfile(mIntCameraId, quality)) {
            return null;
        }

        // Cache the value on first query, and reuse the result in subsequent queries.
        if (mEncoderProfilesCache.containsKey(quality)) {
            return mEncoderProfilesCache.get(quality);
        } else {
            EncoderProfilesProxy profiles = getProfilesInternal(quality);
            mEncoderProfilesCache.put(quality, profiles);
            return profiles;
        }
    }

    @Nullable
    private EncoderProfilesProxy getProfilesInternal(int quality) {
        if (Build.VERSION.SDK_INT >= 31) {
            EncoderProfiles profiles = Api31Impl.getAll(mCameraId, quality);
            if (profiles == null) {
                return null;
            }

            boolean isVideoProfilesInvalid = DeviceQuirks.get(InvalidVideoProfilesQuirk.class)
                    != null;
            if (isVideoProfilesInvalid) {
                Logger.d(TAG, "EncoderProfiles contains invalid video profiles, use "
                        + "CamcorderProfile to create EncoderProfilesProxy.");
            } else {
                try {
                    return EncoderProfilesProxyCompat.from(profiles);
                } catch (NullPointerException e) {
                    Logger.w(TAG, "Failed to create EncoderProfilesProxy, EncoderProfiles might "
                            + " contain invalid video profiles. Use CamcorderProfile instead.", e);
                }
            }
        }

        return createProfilesFromCamcorderProfile(quality);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private EncoderProfilesProxy createProfilesFromCamcorderProfile(int quality) {
        CamcorderProfile profile = null;
        try {
            profile = CamcorderProfile.get(mIntCameraId, quality);
        } catch (RuntimeException e) {
            // CamcorderProfile.get() will throw
            // - RuntimeException if not able to retrieve camcorder profile params.
            // - IllegalArgumentException if quality is not valid.
            Logger.w(TAG, "Unable to get CamcorderProfile by quality: " + quality, e);
        }
        return profile != null ? EncoderProfilesProxyCompat.from(profile) : null;
    }

    @RequiresApi(31)
    static class Api31Impl {
        @DoNotInline
        static EncoderProfiles getAll(String cameraId, int quality) {
            return CamcorderProfile.getAll(cameraId, quality);
        }

        // This class is not instantiable.
        private Api31Impl() {
        }
    }
}
