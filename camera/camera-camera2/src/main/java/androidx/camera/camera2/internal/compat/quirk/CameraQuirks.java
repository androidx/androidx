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
import androidx.camera.camera2.internal.compat.CameraCharacteristicsCompat;
import androidx.camera.core.Logger;
import androidx.camera.core.impl.Quirk;
import androidx.camera.core.impl.QuirkSettings;
import androidx.camera.core.impl.QuirkSettingsHolder;
import androidx.camera.core.impl.Quirks;

import java.util.ArrayList;
import java.util.List;

/** Provider of camera specific quirks. */
public class CameraQuirks {
    private static final String TAG = "CameraQuirks";

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
        QuirkSettings quirkSettings = QuirkSettingsHolder.instance().get();
        final List<Quirk> quirks = new ArrayList<>();
        // Go through all defined camera quirks, and add them to `quirks` if they should be loaded
        if (quirkSettings.shouldEnableQuirk(AeFpsRangeLegacyQuirk.class,
                AeFpsRangeLegacyQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new AeFpsRangeLegacyQuirk(cameraCharacteristicsCompat));
        }
        if (quirkSettings.shouldEnableQuirk(AspectRatioLegacyApi21Quirk.class,
                AspectRatioLegacyApi21Quirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new AspectRatioLegacyApi21Quirk());
        }
        if (quirkSettings.shouldEnableQuirk(JpegHalCorruptImageQuirk.class,
                JpegHalCorruptImageQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new JpegHalCorruptImageQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(JpegCaptureDownsizingQuirk.class,
                JpegCaptureDownsizingQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new JpegCaptureDownsizingQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(CamcorderProfileResolutionQuirk.class,
                CamcorderProfileResolutionQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new CamcorderProfileResolutionQuirk(cameraCharacteristicsCompat));
        }
        if (quirkSettings.shouldEnableQuirk(CaptureNoResponseQuirk.class,
                CaptureNoResponseQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new CaptureNoResponseQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                LegacyCameraOutputConfigNullPointerQuirk.class,
                LegacyCameraOutputConfigNullPointerQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new LegacyCameraOutputConfigNullPointerQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(LegacyCameraSurfaceCleanupQuirk.class,
                LegacyCameraSurfaceCleanupQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new LegacyCameraSurfaceCleanupQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(ImageCaptureWashedOutImageQuirk.class,
                ImageCaptureWashedOutImageQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new ImageCaptureWashedOutImageQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(CameraNoResponseWhenEnablingFlashQuirk.class,
                CameraNoResponseWhenEnablingFlashQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new CameraNoResponseWhenEnablingFlashQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(YuvImageOnePixelShiftQuirk.class,
                YuvImageOnePixelShiftQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new YuvImageOnePixelShiftQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(FlashTooSlowQuirk.class,
                FlashTooSlowQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new FlashTooSlowQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(AfRegionFlipHorizontallyQuirk.class,
                AfRegionFlipHorizontallyQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new AfRegionFlipHorizontallyQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ConfigureSurfaceToSecondarySessionFailQuirk.class,
                ConfigureSurfaceToSecondarySessionFailQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new ConfigureSurfaceToSecondarySessionFailQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(PreviewOrientationIncorrectQuirk.class,
                PreviewOrientationIncorrectQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new PreviewOrientationIncorrectQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(CaptureSessionStuckQuirk.class,
                CaptureSessionStuckQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new CaptureSessionStuckQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(ImageCaptureFlashNotFireQuirk.class,
                ImageCaptureFlashNotFireQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new ImageCaptureFlashNotFireQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(ImageCaptureWithFlashUnderexposureQuirk.class,
                ImageCaptureWithFlashUnderexposureQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new ImageCaptureWithFlashUnderexposureQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(ImageCaptureFailWithAutoFlashQuirk.class,
                ImageCaptureFailWithAutoFlashQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new ImageCaptureFailWithAutoFlashQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(IncorrectCaptureStateQuirk.class,
                IncorrectCaptureStateQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new IncorrectCaptureStateQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(TorchFlashRequiredFor3aUpdateQuirk.class,
                TorchFlashRequiredFor3aUpdateQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new TorchFlashRequiredFor3aUpdateQuirk(cameraCharacteristicsCompat));
        }
        if (quirkSettings.shouldEnableQuirk(
                PreviewStretchWhenVideoCaptureIsBoundQuirk.class,
                PreviewStretchWhenVideoCaptureIsBoundQuirk.load())) {
            quirks.add(new PreviewStretchWhenVideoCaptureIsBoundQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                PreviewDelayWhenVideoCaptureIsBoundQuirk.class,
                PreviewDelayWhenVideoCaptureIsBoundQuirk.load())) {
            quirks.add(new PreviewDelayWhenVideoCaptureIsBoundQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                ImageCaptureFailedWhenVideoCaptureIsBoundQuirk.class,
                ImageCaptureFailedWhenVideoCaptureIsBoundQuirk.load())) {
            quirks.add(new ImageCaptureFailedWhenVideoCaptureIsBoundQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(TemporalNoiseQuirk.class,
                TemporalNoiseQuirk.load(cameraCharacteristicsCompat))) {
            quirks.add(new TemporalNoiseQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(ImageCaptureFailedForVideoSnapshotQuirk.class,
                ImageCaptureFailedForVideoSnapshotQuirk.load())) {
            quirks.add(new ImageCaptureFailedForVideoSnapshotQuirk());
        }
        if (quirkSettings.shouldEnableQuirk(
                CaptureSessionStuckWhenCreatingBeforeClosingCameraQuirk.class,
                CaptureSessionStuckWhenCreatingBeforeClosingCameraQuirk.load(
                        cameraCharacteristicsCompat))) {
            quirks.add(new CaptureSessionStuckWhenCreatingBeforeClosingCameraQuirk());
        }

        Quirks cameraQuirks = new Quirks(quirks);
        Logger.d(TAG, "camera2 CameraQuirks = " + Quirks.toString(cameraQuirks));
        return cameraQuirks;
    }
}
