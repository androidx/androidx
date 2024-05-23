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

package androidx.camera.camera2.internal.compat.workaround;

import androidx.annotation.NonNull;
import androidx.camera.camera2.internal.compat.quirk.AutoFlashUnderExposedQuirk;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.impl.Quirks;

/**
 * The workaround sets AE_MODE to AE_MODE_ON_ALWAYS_FLASH in still capture requests. On some
 * devices like Pixel 3A, using AE_MODE_ON_AUTO_FLASH failed to get correctly exposed photos.
 *
 * <p>This class is not thread-safe and must be accessed from the same thread.
 * @see AutoFlashUnderExposedQuirk
 */
public class OverrideAeModeForStillCapture {
    private final boolean mHasAutoFlashUnderExposedQuirk;
    private boolean mAePrecaptureStarted = false;
    public OverrideAeModeForStillCapture(@NonNull Quirks cameraQuirks) {
        mHasAutoFlashUnderExposedQuirk = cameraQuirks.get(AutoFlashUnderExposedQuirk.class) != null;
    }

    /**
     * Notify that Ae precapture has started.
     */
    public void onAePrecaptureStarted() {
        mAePrecaptureStarted = true;
    }

    /**
     * Notify that the Ae precapture sequence has ended.
     */
    public void onAePrecaptureFinished() {
        mAePrecaptureStarted = false;
    }

    /**
     * Returns true if it requires to set CONTROL_AE_MODE to CONTROL_AE_MODE_ON_ALWAYS_FLASH for
     * still capture requests in given flash mode.
     */
    public boolean shouldSetAeModeAlwaysFlash(@ImageCapture.FlashMode int flashMode) {
        return mAePrecaptureStarted && flashMode == ImageCapture.FLASH_MODE_AUTO
                && mHasAutoFlashUnderExposedQuirk;
    }
}
