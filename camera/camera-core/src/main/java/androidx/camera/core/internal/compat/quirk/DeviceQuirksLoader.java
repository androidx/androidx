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

package androidx.camera.core.internal.compat.quirk;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.QuirkSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads all device specific quirks required for the current device
 */
public class DeviceQuirksLoader {

    private DeviceQuirksLoader() {
    }

    /**
     * Goes through all defined device-specific quirks, and returns those that should be loaded
     * on the current device.
     */
    @NonNull
    static List<Quirk> loadQuirks(@NonNull QuirkSettings quirkSettings) {
        final List<Quirk> quirks = new ArrayList<>();

        if (quirkSettings.shouldEnableQuirk(ImageCaptureRotationOptionQuirk.class,
                ImageCaptureRotationOptionQuirk.load())) {
            quirks.add(new ImageCaptureRotationOptionQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(SurfaceOrderQuirk.class,
                SurfaceOrderQuirk.load())) {
            quirks.add(new SurfaceOrderQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(CaptureFailedRetryQuirk.class,
                CaptureFailedRetryQuirk.load())) {
            quirks.add(new CaptureFailedRetryQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(LowMemoryQuirk.class,
                LowMemoryQuirk.load())) {
            quirks.add(new LowMemoryQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(LargeJpegImageQuirk.class,
                LargeJpegImageQuirk.load())) {
            quirks.add(new LargeJpegImageQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(IncorrectJpegMetadataQuirk.class,
                IncorrectJpegMetadataQuirk.load())) {
            quirks.add(new IncorrectJpegMetadataQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(ImageCaptureFailedForSpecificCombinationQuirk.class,
                ImageCaptureFailedForSpecificCombinationQuirk.load())) {
            quirks.add(new ImageCaptureFailedForSpecificCombinationQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(PreviewGreenTintQuirk.class,
                PreviewGreenTintQuirk.load())) {
            quirks.add(PreviewGreenTintQuirk.INSTANCE);
        }

        return quirks;
    }
}
