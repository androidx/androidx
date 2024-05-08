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

package androidx.camera.core.impl;

import androidx.annotation.NonNull;
import androidx.camera.core.Logger;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Utility class for checking the convergence of 3A states.
 */
public class ConvergenceUtils {

    private static final String TAG = "ConvergenceUtils";

    private static final Set<CameraCaptureMetaData.AfState> AF_CONVERGED_STATE_SET =
            Collections.unmodifiableSet(EnumSet.of(
                    CameraCaptureMetaData.AfState.PASSIVE_FOCUSED,
                    CameraCaptureMetaData.AfState.PASSIVE_NOT_FOCUSED,
                    CameraCaptureMetaData.AfState.LOCKED_FOCUSED,
                    CameraCaptureMetaData.AfState.LOCKED_NOT_FOCUSED
            ));

    private static final Set<CameraCaptureMetaData.AwbState> AWB_CONVERGED_STATE_SET =
            Collections.unmodifiableSet(EnumSet.of(
                    CameraCaptureMetaData.AwbState.CONVERGED,
                    // Unknown means cannot get valid state from CaptureResult
                    CameraCaptureMetaData.AwbState.UNKNOWN
            ));

    private static final Set<CameraCaptureMetaData.AeState> AE_CONVERGED_STATE_SET =
            Collections.unmodifiableSet(EnumSet.of(
                    CameraCaptureMetaData.AeState.CONVERGED,
                    CameraCaptureMetaData.AeState.FLASH_REQUIRED,
                    // Unknown means cannot get valid state from CaptureResult
                    CameraCaptureMetaData.AeState.UNKNOWN
            ));

    private static final Set<CameraCaptureMetaData.AeState> AE_TORCH_AS_FLASH_CONVERGED_STATE_SET;

    static {
        EnumSet<CameraCaptureMetaData.AeState> aeStateSet = EnumSet.copyOf(AE_CONVERGED_STATE_SET);

        // Some devices always show FLASH_REQUIRED when the torch is opened, so it cannot be
        // treated as the AE converge signal.
        aeStateSet.remove(CameraCaptureMetaData.AeState.FLASH_REQUIRED);

        // AeState.UNKNOWN means it doesn't have valid AE info. For this kind of device, we tend
        // to wait for a few more seconds for the auto exposure update. So the UNKNOWN state
        // should not be treated as the AE converge signal.
        aeStateSet.remove(CameraCaptureMetaData.AeState.UNKNOWN);

        AE_TORCH_AS_FLASH_CONVERGED_STATE_SET = Collections.unmodifiableSet(aeStateSet);
    }

    private ConvergenceUtils() {
    }

    /**
     * Check if the capture result is converged.
     */
    public static boolean is3AConverged(@NonNull CameraCaptureResult captureResult,
            boolean isTorchAsFlash) {

        // If afMode is OFF or UNKNOWN , no need for waiting.
        // otherwise wait until af is locked or focused.
        boolean isAfReady = captureResult.getAfMode() == CameraCaptureMetaData.AfMode.OFF
                || captureResult.getAfMode() == CameraCaptureMetaData.AfMode.UNKNOWN
                || AF_CONVERGED_STATE_SET.contains(captureResult.getAfState());

        boolean isAeReady;
        boolean isAeModeOff = captureResult.getAeMode() == CameraCaptureMetaData.AeMode.OFF;
        if (isTorchAsFlash) {
            isAeReady = isAeModeOff
                    || AE_TORCH_AS_FLASH_CONVERGED_STATE_SET.contains(captureResult.getAeState());
        } else {
            isAeReady = isAeModeOff || AE_CONVERGED_STATE_SET.contains(captureResult.getAeState());
        }

        boolean isAwbModeOff = captureResult.getAwbMode() == CameraCaptureMetaData.AwbMode.OFF;
        boolean isAwbReady = isAwbModeOff
                || AWB_CONVERGED_STATE_SET.contains(captureResult.getAwbState());

        Logger.d(TAG, "checkCaptureResult, AE=" + captureResult.getAeState()
                + " AF =" + captureResult.getAfState()
                + " AWB=" + captureResult.getAwbState());
        return isAfReady && isAeReady && isAwbReady;
    }
}
