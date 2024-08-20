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

import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.core.Logger
import androidx.camera.core.impl.Quirk
import androidx.camera.core.impl.QuirkSettingsHolder
import androidx.camera.core.impl.Quirks
import javax.inject.Inject

/** Provider of camera specific quirks. */
@CameraScope
public class CameraQuirks
@Inject
constructor(
    private val cameraMetadata: CameraMetadata?,
    private val streamConfigurationMapCompat: StreamConfigurationMapCompat
) {
    /**
     * Goes through all defined camera specific quirks, then filters them to retrieve quirks
     * required for the camera identified by the provided [CameraMetadata].
     */
    public val quirks: Quirks by lazy {
        val quirkSettings = QuirkSettingsHolder.instance().get()
        val quirks: MutableList<Quirk> = mutableListOf()
        if (cameraMetadata == null) {
            Log.error { "Failed to enable quirks: camera metadata injection failed" }
            return@lazy Quirks(quirks)
        }

        // Go through all defined camera quirks in lexicographical order,
        // and add them to `quirks` if they should be loaded
        if (
            quirkSettings.shouldEnableQuirk(
                AeFpsRangeLegacyQuirk::class.java,
                AeFpsRangeLegacyQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(AeFpsRangeLegacyQuirk(cameraMetadata))
        }
        if (
            quirkSettings.shouldEnableQuirk(
                AfRegionFlipHorizontallyQuirk::class.java,
                AfRegionFlipHorizontallyQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(AfRegionFlipHorizontallyQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                AspectRatioLegacyApi21Quirk::class.java,
                AspectRatioLegacyApi21Quirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(AspectRatioLegacyApi21Quirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                CamcorderProfileResolutionQuirk::class.java,
                CamcorderProfileResolutionQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(CamcorderProfileResolutionQuirk(streamConfigurationMapCompat))
        }
        if (
            quirkSettings.shouldEnableQuirk(
                CameraNoResponseWhenEnablingFlashQuirk::class.java,
                CameraNoResponseWhenEnablingFlashQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(CameraNoResponseWhenEnablingFlashQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                CaptureSessionStuckQuirk::class.java,
                CaptureSessionStuckQuirk.isEnabled()
            )
        ) {
            quirks.add(CaptureSessionStuckQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                CloseCaptureSessionOnVideoQuirk::class.java,
                CloseCaptureSessionOnVideoQuirk.isEnabled()
            )
        ) {
            quirks.add(CloseCaptureSessionOnVideoQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ConfigureSurfaceToSecondarySessionFailQuirk::class.java,
                ConfigureSurfaceToSecondarySessionFailQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(ConfigureSurfaceToSecondarySessionFailQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                FinalizeSessionOnCloseQuirk::class.java,
                FinalizeSessionOnCloseQuirk.isEnabled()
            )
        ) {
            quirks.add(FinalizeSessionOnCloseQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                FlashTooSlowQuirk::class.java,
                FlashTooSlowQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(FlashTooSlowQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ImageCaptureFailWithAutoFlashQuirk::class.java,
                ImageCaptureFailWithAutoFlashQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(ImageCaptureFailWithAutoFlashQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ImageCaptureFlashNotFireQuirk::class.java,
                ImageCaptureFlashNotFireQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(ImageCaptureFlashNotFireQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ImageCaptureWashedOutImageQuirk::class.java,
                ImageCaptureWashedOutImageQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(ImageCaptureWashedOutImageQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ImageCaptureWithFlashUnderexposureQuirk::class.java,
                ImageCaptureWithFlashUnderexposureQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(ImageCaptureWithFlashUnderexposureQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                JpegHalCorruptImageQuirk::class.java,
                JpegHalCorruptImageQuirk.isEnabled()
            )
        ) {
            quirks.add(JpegHalCorruptImageQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                PreviewOrientationIncorrectQuirk::class.java,
                PreviewOrientationIncorrectQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(PreviewOrientationIncorrectQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                TextureViewIsClosedQuirk::class.java,
                TextureViewIsClosedQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(TextureViewIsClosedQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                TorchFlashRequiredFor3aUpdateQuirk::class.java,
                TorchFlashRequiredFor3aUpdateQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(TorchFlashRequiredFor3aUpdateQuirk(cameraMetadata))
        }
        if (
            quirkSettings.shouldEnableQuirk(
                YuvImageOnePixelShiftQuirk::class.java,
                YuvImageOnePixelShiftQuirk.isEnabled()
            )
        ) {
            quirks.add(YuvImageOnePixelShiftQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                PreviewStretchWhenVideoCaptureIsBoundQuirk::class.java,
                PreviewStretchWhenVideoCaptureIsBoundQuirk.isEnabled()
            )
        ) {
            quirks.add(PreviewStretchWhenVideoCaptureIsBoundQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                PreviewDelayWhenVideoCaptureIsBoundQuirk::class.java,
                PreviewDelayWhenVideoCaptureIsBoundQuirk.isEnabled()
            )
        ) {
            quirks.add(PreviewDelayWhenVideoCaptureIsBoundQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ImageCaptureFailedWhenVideoCaptureIsBoundQuirk::class.java,
                ImageCaptureFailedWhenVideoCaptureIsBoundQuirk.isEnabled()
            )
        ) {
            quirks.add(ImageCaptureFailedWhenVideoCaptureIsBoundQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                TemporalNoiseQuirk::class.java,
                TemporalNoiseQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(TemporalNoiseQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                ImageCaptureFailedForVideoSnapshotQuirk::class.java,
                ImageCaptureFailedForVideoSnapshotQuirk.isEnabled()
            )
        ) {
            quirks.add(ImageCaptureFailedForVideoSnapshotQuirk())
        }
        if (
            quirkSettings.shouldEnableQuirk(
                LockAeAndCaptureImageBreakCameraQuirk::class.java,
                LockAeAndCaptureImageBreakCameraQuirk.isEnabled(cameraMetadata)
            )
        ) {
            quirks.add(LockAeAndCaptureImageBreakCameraQuirk())
        }

        Quirks(quirks).also {
            Logger.d(TAG, "camera2-pipe-integration CameraQuirks = " + Quirks.toString(it))
        }
    }

    public companion object {
        private const val TAG = "CameraQuirks"

        public fun isImmediateSurfaceReleaseAllowed(): Boolean {
            // TODO(b/285956022): Releasing a Surface too early turns out to cause memory leaks
            //  where an Image may not be eventually closed. When the issue is resolved on an
            //  architectural level, uncomment the following, allowing compliant devices to recycle
            //  Surfaces and shutdown sooner.
            //  Build.BRAND == "google" && Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1
            return false
        }
    }
}
