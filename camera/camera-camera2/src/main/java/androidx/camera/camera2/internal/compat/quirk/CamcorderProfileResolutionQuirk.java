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

package androidx.camera.camera2.internal.compat.quirk;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.camera2.internal.compat.StreamConfigurationMapCompat;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.Quirk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>QuirkSummary
 *     Bug Id: 180819729
 *     Description: Quirk that should validate the video resolution of {@link CamcorderProfile}
 *                  on legacy camera. When using the Camera 2 API in {@code LEGACY} mode (i.e.
 *                  when {@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL} is set to
 *                  {@link CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}),
 *                  {@link CamcorderProfile#hasProfile} may return {@code true} for unsupported
 *                  resolutions. To ensure a given resolution is supported in LEGACY mode, the
 *                  configuration given in
 *                  {@link CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP} must contain
 *                  the resolution in the supported output sizes. The recommended way to check
 *                  this is with {@link StreamConfigurationMap#getOutputSizes(Class)} with the
 *                  class of the desired recording endpoint, and check that the desired
 *                  resolution is contained in the list returned.
 *     Device(s): All legacy devices
 *     @see CamcorderProfile#hasProfile
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CamcorderProfileResolutionQuirk implements Quirk {
    private static final String TAG = "CamcorderProfileResolutionQuirk";

    static boolean load(@NonNull CameraCharacteristicsCompat characteristicsCompat) {
        final Integer level = characteristicsCompat.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    private final StreamConfigurationMapCompat mStreamConfigurationMapCompat;
    private List<Size> mSupportedResolutions = null;

    public CamcorderProfileResolutionQuirk(
            @NonNull CameraCharacteristicsCompat characteristicsCompat) {
        mStreamConfigurationMapCompat =
                characteristicsCompat.getStreamConfigurationMapCompat();
    }

    /** Returns the supported video resolutions. */
    @NonNull
    public List<Size> getSupportedResolutions() {
        if (mSupportedResolutions == null) {
            Size[] sizes = mStreamConfigurationMapCompat.getOutputSizes(
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE);
            mSupportedResolutions = sizes != null ? Arrays.asList(sizes.clone())
                    : Collections.emptyList();

            Logger.d(TAG, "mSupportedResolutions = " + mSupportedResolutions);
        }

        return new ArrayList<>(mSupportedResolutions);
    }
}
