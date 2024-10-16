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

        // Load all device specific quirks
        if (quirkSettings.shouldEnableQuirk(
                ImageCapturePixelHDRPlusQuirk.class,
                ImageCapturePixelHDRPlusQuirk.load())) {
            quirks.add(new ImageCapturePixelHDRPlusQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ExtraCroppingQuirk.class,
                ExtraCroppingQuirk.load())) {
            quirks.add(new ExtraCroppingQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                Nexus4AndroidLTargetAspectRatioQuirk.class,
                Nexus4AndroidLTargetAspectRatioQuirk.load())) {
            quirks.add(new Nexus4AndroidLTargetAspectRatioQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ExcludedSupportedSizesQuirk.class,
                ExcludedSupportedSizesQuirk.load())) {
            quirks.add(new ExcludedSupportedSizesQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                CrashWhenTakingPhotoWithAutoFlashAEModeQuirk.class,
                CrashWhenTakingPhotoWithAutoFlashAEModeQuirk.load())) {
            quirks.add(new CrashWhenTakingPhotoWithAutoFlashAEModeQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                PreviewPixelHDRnetQuirk.class,
                PreviewPixelHDRnetQuirk.load())) {
            quirks.add(new PreviewPixelHDRnetQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                StillCaptureFlashStopRepeatingQuirk.class,
                StillCaptureFlashStopRepeatingQuirk.load())) {
            quirks.add(new StillCaptureFlashStopRepeatingQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ExtraSupportedSurfaceCombinationsQuirk.class,
                ExtraSupportedSurfaceCombinationsQuirk.load())) {
            quirks.add(new ExtraSupportedSurfaceCombinationsQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                FlashAvailabilityBufferUnderflowQuirk.class,
                FlashAvailabilityBufferUnderflowQuirk.load())) {
            quirks.add(new FlashAvailabilityBufferUnderflowQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                RepeatingStreamConstraintForVideoRecordingQuirk.class,
                RepeatingStreamConstraintForVideoRecordingQuirk.load())) {
            quirks.add(new RepeatingStreamConstraintForVideoRecordingQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                TextureViewIsClosedQuirk.class,
                TextureViewIsClosedQuirk.load())) {
            quirks.add(new TextureViewIsClosedQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                CaptureSessionOnClosedNotCalledQuirk.class,
                CaptureSessionOnClosedNotCalledQuirk.load())) {
            quirks.add(new CaptureSessionOnClosedNotCalledQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                TorchIsClosedAfterImageCapturingQuirk.class,
                TorchIsClosedAfterImageCapturingQuirk.load())) {
            quirks.add(new TorchIsClosedAfterImageCapturingQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ZslDisablerQuirk.class,
                ZslDisablerQuirk.load())) {
            quirks.add(new ZslDisablerQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ExtraSupportedOutputSizeQuirk.class,
                ExtraSupportedOutputSizeQuirk.load())) {
            quirks.add(new ExtraSupportedOutputSizeQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                InvalidVideoProfilesQuirk.class,
                InvalidVideoProfilesQuirk.load())) {
            quirks.add(new InvalidVideoProfilesQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                Preview3AThreadCrashQuirk.class,
                Preview3AThreadCrashQuirk.load())) {
            quirks.add(new Preview3AThreadCrashQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                SmallDisplaySizeQuirk.class,
                SmallDisplaySizeQuirk.load())) {
            quirks.add(new SmallDisplaySizeQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                PreviewUnderExposureQuirk.class,
                PreviewUnderExposureQuirk.load())) {
            quirks.add(PreviewUnderExposureQuirk.INSTANCE);
        }

        return quirks;
    }
}
