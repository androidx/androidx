/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.Quirks;

import java.util.ArrayList;
import java.util.List;

/** Provider of camera specific quirks. */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraQuirks {

    private CameraQuirks() {
    }

    /**
     * Goes through all defined camera specific quirks, then filters them to retrieve quirks
     * required for the camera identified by the provided camera id and
     * {@link CameraCharacteristics}.
     *
     * @param cameraId                    Camera id of the camera device  used to filter quirks
     * @param cameraCharacteristicsCompat Characteristics of the camera device user to filter quirks
     * @return List of quirks associated with the camera identified by its id and
     * {@link CameraCharacteristics}.
     */
    @NonNull
    public static Quirks get(@NonNull final String cameraId,
            @NonNull final CameraCharacteristicsCompat cameraCharacteristicsCompat) {
        final List<Quirk> quirks = new ArrayList<>();
        // Go through all defined camera quirks, and add them to `quirks` if they should be loaded
        if (AeFpsRangeLegacyQuirk.load(cameraCharacteristicsCompat)) {
            quirks.add(new AeFpsRangeLegacyQuirk(cameraCharacteristicsCompat));
        }
        if (AspectRatioLegacyApi21Quirk.load(cameraCharacteristicsCompat)) {
            quirks.add(new AspectRatioLegacyApi21Quirk());
        }
        if (JpegHalCorruptImageQuirk.load(cameraCharacteristicsCompat)) {
            quirks.add(new JpegHalCorruptImageQuirk());
        }
        if (CamcorderProfileResolutionQuirk.load(cameraCharacteristicsCompat)) {
            quirks.add(new CamcorderProfileResolutionQuirk(cameraCharacteristicsCompat));
        }
        if (ImageCaptureWashedOutImageQuirk.load(cameraCharacteristicsCompat)) {
            quirks.add(new ImageCaptureWashedOutImageQuirk());
        }
        if (CameraNoResponseWhenEnablingFlashQuirk.load(cameraCharacteristicsCompat)) {
            quirks.add(new CameraNoResponseWhenEnablingFlashQuirk());
        }
        if (YuvImageOnePixelShiftQuirk.load(cameraCharacteristicsCompat)) {
            quirks.add(new YuvImageOnePixelShiftQuirk());
        }

        return new Quirks(quirks);
    }
}
