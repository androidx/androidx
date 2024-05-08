/*
 * Copyright 2022 The Android Open Source Project
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

import android.hardware.camera2.CaptureRequest;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.impl.Camera2ImplConfig;
import androidx.camera.camera2.internal.compat.quirk.DeviceQuirks;
import androidx.camera.camera2.internal.compat.quirk.TorchIsClosedAfterImageCapturingQuirk;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.impl.CaptureConfig;
import androidx.camera.core.impl.DeferrableSurface;

import java.util.List;

/**
 * This is a workaround for b/228272227 where the Torch is unexpectedly closed after a single
 * capturing.
 */
public class TorchStateReset {
    private final boolean mIsImageCaptureTorchIsClosedQuirkEnabled;

    public TorchStateReset() {
        mIsImageCaptureTorchIsClosedQuirkEnabled =
                DeviceQuirks.get(TorchIsClosedAfterImageCapturingQuirk.class) != null;
    }

    /**
     * The method indicates if the Torch would be unexpectedly closed after the capture. Return
     * true means the Torch will be unexpectedly closed, and it requires turning on the Torch
     * again after the capturing.
     */
    public boolean isTorchResetRequired(@NonNull List<CaptureRequest> captureRequestList,
            boolean isSingleCapture) {
        if (mIsImageCaptureTorchIsClosedQuirkEnabled && isSingleCapture) {
            for (CaptureRequest captureRequest: captureRequestList) {
                Integer flashMode = captureRequest.get(CaptureRequest.FLASH_MODE);
                if (flashMode != null && flashMode == CaptureRequest.FLASH_MODE_TORCH) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Create a single request from the {@link CaptureConfig}. And turns off the Torch by
     * overriding the FLASH_MODE to FLASH_MODE_OFF.
     *
     * @param repeatingConfig the CaptureConfig that is used for the repeating request of the
     *                        current session.
     * @return a CaptureConfig that can be used in a single request to reset the FLASH_MODE to
     * FLASH_MODE_OFF. The returned CaptureConfig will not set the CameraCaptureCallbacks and
     * Tags since it is only used for Torch reset.
     */
    @OptIn(markerClass = ExperimentalCamera2Interop.class)
    @NonNull
    public CaptureConfig createTorchResetRequest(@NonNull CaptureConfig repeatingConfig) {
        CaptureConfig.Builder captureConfigBuilder = new CaptureConfig.Builder();
        captureConfigBuilder.setTemplateType(repeatingConfig.getTemplateType());

        // The single request only applies to the repeating surfaces.
        for (DeferrableSurface deferrableSurface : repeatingConfig.getSurfaces()) {
            captureConfigBuilder.addSurface(deferrableSurface);
        }

        captureConfigBuilder.addImplementationOptions(repeatingConfig.getImplementationOptions());
        Camera2ImplConfig.Builder builder = new Camera2ImplConfig.Builder();
        builder.setCaptureRequestOption(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);

        captureConfigBuilder.addImplementationOptions(builder.build());

        return captureConfigBuilder.build();
    }
}
