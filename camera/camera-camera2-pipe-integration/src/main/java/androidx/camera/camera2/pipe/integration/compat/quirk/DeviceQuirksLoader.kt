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

package androidx.camera.camera2.pipe.integration.compat.quirk

import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.QuirkSettings

/** Loads all device specific quirks required for the current device. */
public object DeviceQuirksLoader {

    /**
     * Goes through all defined device-specific quirks, and returns those that should be loaded on
     * the current device.
     */
    public fun loadQuirks(quirkSettings: QuirkSettings): List<Quirk> {
        val quirks: MutableList<Quirk> = mutableListOf()

        // Load all device specific quirks, preferably in lexicographical order
        if (
            quirkSettings.shouldEnableQuirk(
                CloseCameraDeviceOnCameraGraphCloseQuirk::class.java,
                CloseCameraDeviceOnCameraGraphCloseQuirk.isEnabled()
            )
        ) {
            quirks.add(CloseCameraDeviceOnCameraGraphCloseQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                CloseCaptureSessionOnDisconnectQuirk::class.java,
                CloseCaptureSessionOnDisconnectQuirk.isEnabled()
            )
        ) {
            quirks.add(CloseCaptureSessionOnDisconnectQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                CrashWhenTakingPhotoWithAutoFlashAEModeQuirk::class.java,
                CrashWhenTakingPhotoWithAutoFlashAEModeQuirk.isEnabled()
            )
        ) {
            quirks.add(CrashWhenTakingPhotoWithAutoFlashAEModeQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ControlZoomRatioRangeAssertionErrorQuirk::class.java,
                ControlZoomRatioRangeAssertionErrorQuirk.isEnabled()
            )
        ) {
            quirks.add(ControlZoomRatioRangeAssertionErrorQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                DisableAbortCapturesOnStopWithSessionProcessorQuirk::class.java,
                DisableAbortCapturesOnStopWithSessionProcessorQuirk.isEnabled()
            )
        ) {
            quirks.add(DisableAbortCapturesOnStopWithSessionProcessorQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                FlashAvailabilityBufferUnderflowQuirk::class.java,
                FlashAvailabilityBufferUnderflowQuirk.isEnabled()
            )
        ) {
            quirks.add(FlashAvailabilityBufferUnderflowQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ImageCapturePixelHDRPlusQuirk::class.java,
                ImageCapturePixelHDRPlusQuirk.isEnabled()
            )
        ) {
            quirks.add(ImageCapturePixelHDRPlusQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                InvalidVideoProfilesQuirk::class.java,
                InvalidVideoProfilesQuirk.isEnabled()
            )
        ) {
            quirks.add(InvalidVideoProfilesQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ExcludedSupportedSizesQuirk::class.java,
                ExcludedSupportedSizesQuirk.isEnabled()
            )
        ) {
            quirks.add(ExcludedSupportedSizesQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ExtraCroppingQuirk::class.java,
                ExtraCroppingQuirk.isEnabled()
            )
        ) {
            quirks.add(ExtraCroppingQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ExtraSupportedOutputSizeQuirk::class.java,
                ExtraSupportedOutputSizeQuirk.isEnabled()
            )
        ) {
            quirks.add(ExtraSupportedOutputSizeQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ExtraSupportedSurfaceCombinationsQuirk::class.java,
                ExtraSupportedSurfaceCombinationsQuirk.isEnabled()
            )
        ) {
            quirks.add(ExtraSupportedSurfaceCombinationsQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                Nexus4AndroidLTargetAspectRatioQuirk::class.java,
                Nexus4AndroidLTargetAspectRatioQuirk.isEnabled()
            )
        ) {
            quirks.add(Nexus4AndroidLTargetAspectRatioQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                PreviewPixelHDRnetQuirk::class.java,
                PreviewPixelHDRnetQuirk.isEnabled()
            )
        ) {
            quirks.add(PreviewPixelHDRnetQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                RepeatingStreamConstraintForVideoRecordingQuirk::class.java,
                RepeatingStreamConstraintForVideoRecordingQuirk.isEnabled()
            )
        ) {
            quirks.add(RepeatingStreamConstraintForVideoRecordingQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                StillCaptureFlashStopRepeatingQuirk::class.java,
                StillCaptureFlashStopRepeatingQuirk.isEnabled()
            )
        ) {
            quirks.add(StillCaptureFlashStopRepeatingQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                TorchIsClosedAfterImageCapturingQuirk::class.java,
                TorchIsClosedAfterImageCapturingQuirk.isEnabled()
            )
        ) {
            quirks.add(TorchIsClosedAfterImageCapturingQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                SurfaceOrderQuirk::class.java,
                SurfaceOrderQuirk.isEnabled()
            )
        ) {
            quirks.add(SurfaceOrderQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                CaptureSessionOnClosedNotCalledQuirk::class.java,
                CaptureSessionOnClosedNotCalledQuirk.isEnabled()
            )
        ) {
            quirks.add(CaptureSessionOnClosedNotCalledQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(ZslDisablerQuirk::class.java, ZslDisablerQuirk.load())
        ) {
            quirks.add(ZslDisablerQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                SmallDisplaySizeQuirk::class.java,
                SmallDisplaySizeQuirk.load()
            )
        ) {
            quirks.add(SmallDisplaySizeQuirk())
        }
        return quirks
    }
}
