/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.biometric;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

class Utils {
    // Private constructor to prevent instantiation.
    private Utils() {
    }

    /**
     * Determines if the given ID fails to match any known error message.
     *
     * @param errMsgId Integer ID representing an error.
     * @return true if the error is not publicly defined, or false otherwise.
     */
    static boolean isUnknownError(int errMsgId) {
        switch (errMsgId) {
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
            case BiometricPrompt.ERROR_UNABLE_TO_PROCESS:
            case BiometricPrompt.ERROR_TIMEOUT:
            case BiometricPrompt.ERROR_NO_SPACE:
            case BiometricPrompt.ERROR_CANCELED:
            case BiometricPrompt.ERROR_LOCKOUT:
            case BiometricPrompt.ERROR_VENDOR:
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
            case BiometricPrompt.ERROR_USER_CANCELED:
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
            case BiometricPrompt.ERROR_NEGATIVE_BUTTON:
            case BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL:
                return false;
            default:
                return true;
        }
    }

    /**
     * Finishes a given activity if and only if it's a {@link DeviceCredentialHandlerActivity}.
     *
     * @param activity The activity to finish.
     */
    static void maybeFinishHandler(@Nullable FragmentActivity activity) {
        if (activity instanceof DeviceCredentialHandlerActivity && !activity.isFinishing()) {
            activity.finish();
        }
    }

    /**
     * Determines if we are in the process of having the user confirm their PIN/pattern/password.
     *
     * @return true if the user is confirming their device credential, or false otherwise.
     */
    static boolean isConfirmingDeviceCredential() {
        DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        return bridge != null && bridge.isConfirmingDeviceCredential();
    }
}
