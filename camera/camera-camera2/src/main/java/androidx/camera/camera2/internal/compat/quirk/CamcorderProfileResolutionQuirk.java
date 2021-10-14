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

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.os.Build;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.ImageFormatConstants;
import androidx.camera.core.impl.Quirk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Quirk that should validate the video resolution of {@link CamcorderProfile} on legacy camera.
 *
 * <p>
 * When using the Camera 2 API in {@code LEGACY} mode (i.e. when
 * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL} is set
 * to
 * {@link android.hardware.camera2.CameraCharacteristics#INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY}),
 * {@link CamcorderProfile#hasProfile} may return {@code true} for unsupported resolutions. To
 * ensure a given resolution is supported in LEGACY mode, the configuration given in
 * {@link android.hardware.camera2.CameraCharacteristics#SCALER_STREAM_CONFIGURATION_MAP}
 * must contain the resolution in the supported output sizes. The recommended way to check this
 * is with {@link android.hardware.camera2.params.StreamConfigurationMap#getOutputSizes(Class)}
 * with the class of the desired recording endpoint, and check that the desired resolution is
 * contained in the list returned.
 * </p>
 *
 * @see CamcorderProfile#hasProfile
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CamcorderProfileResolutionQuirk implements Quirk {
    private static final String TAG = "CamcorderProfileResolutionQuirk";

    static boolean load(@NonNull CameraCharacteristicsCompat characteristicsCompat) {
        final Integer level = characteristicsCompat.get(
                CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        return level != null && level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY;
    }

    private final List<Size> mSupportedResolutions;

    public CamcorderProfileResolutionQuirk(
            @NonNull CameraCharacteristicsCompat characteristicsCompat) {
        StreamConfigurationMap map =
                characteristicsCompat.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if (map == null) {
            Logger.e(TAG, "StreamConfigurationMap is null");
        }
        Size[] sizes;
        // Before Android 23, use {@link SurfaceTexture} will finally mapped to 0x22 in
        // StreamConfigurationMap to retrieve the output sizes information.
        if (Build.VERSION.SDK_INT < 23) {
            sizes = map != null ? map.getOutputSizes(SurfaceTexture.class) : null;
        } else {
            sizes = map != null ? map.getOutputSizes(
                    ImageFormatConstants.INTERNAL_DEFINED_IMAGE_FORMAT_PRIVATE) : null;
        }

        mSupportedResolutions = sizes != null ? Arrays.asList(sizes.clone())
                : Collections.emptyList();

        Logger.d(TAG, "mSupportedResolutions = " + mSupportedResolutions);
    }

    /** Returns the supported video resolutions. */
    @NonNull
    public List<Size> getSupportedResolutions() {
        return new ArrayList<>(mSupportedResolutions);
    }
}
