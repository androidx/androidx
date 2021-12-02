/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.quirk.CamcorderProfileResolutionQuirk;
import androidx.camera.camera2.internal.compat.quirk.CameraQuirks;
import androidx.camera.camera2.internal.compat.workaround.CamcorderProfileResolutionValidator;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.CamcorderProfileProvider;
import androidx.camera.core.impl.CamcorderProfileProxy;

/** An implementation that provides the {@link CamcorderProfileProxy}. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class Camera2CamcorderProfileProvider implements CamcorderProfileProvider {
    private static final String TAG = "Camera2CamcorderProfileProvider";

    private final boolean mHasValidCameraId;
    private final int mCameraId;
    private final CamcorderProfileResolutionValidator mCamcorderProfileResolutionValidator;

    public Camera2CamcorderProfileProvider(@NonNull String cameraId,
            @NonNull CameraCharacteristicsCompat cameraCharacteristics) {
        boolean hasValidCameraId = false;
        int intCameraId = -1;
        try {
            intCameraId = Integer.parseInt(cameraId);
            hasValidCameraId = true;
        } catch (NumberFormatException e) {
            Logger.w(TAG, "Camera id is not an integer: " + cameraId
                    + ", unable to create CamcorderProfileProvider");
        }
        mHasValidCameraId = hasValidCameraId;
        mCameraId = intCameraId;
        CamcorderProfileResolutionQuirk quirk = CameraQuirks.get(cameraId, cameraCharacteristics)
                .get(CamcorderProfileResolutionQuirk.class);
        mCamcorderProfileResolutionValidator = new CamcorderProfileResolutionValidator(quirk);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasProfile(int quality) {
        if (!mHasValidCameraId) {
            return false;
        }

        if (!CamcorderProfile.hasProfile(mCameraId, quality)) {
            return false;
        }

        if (mCamcorderProfileResolutionValidator.hasQuirk()) {
            // Only get profile when quirk exist for performance concern.
            CamcorderProfileProxy profile = getProfileInternal(quality);
            return mCamcorderProfileResolutionValidator.hasValidVideoResolution(profile);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public CamcorderProfileProxy get(int quality) {
        if (!mHasValidCameraId) {
            return null;
        }

        if (!CamcorderProfile.hasProfile(mCameraId, quality)) {
            return null;
        }

        CamcorderProfileProxy profile = getProfileInternal(quality);
        if (!mCamcorderProfileResolutionValidator.hasValidVideoResolution(profile)) {
            return null;
        }
        return profile;
    }

    @Nullable
    @SuppressWarnings("deprecation")
    private CamcorderProfileProxy getProfileInternal(int quality) {
        CamcorderProfile profile = null;
        try {
            profile = CamcorderProfile.get(mCameraId, quality);
        } catch (RuntimeException e) {
            // CamcorderProfile.get() will throw
            // - RuntimeException if not able to retrieve camcorder profile params.
            // - IllegalArgumentException if quality is not valid.
            Logger.w(TAG, "Unable to get CamcorderProfile by quality: " + quality, e);
        }
        return profile != null ? CamcorderProfileProxy.fromCamcorderProfile(profile) : null;
    }
}
