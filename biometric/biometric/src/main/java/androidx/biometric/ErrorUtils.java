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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Utilities related to biometric authentication errors.
 */
class ErrorUtils {
    // Prevent instantiation.
    private ErrorUtils() {}

    /**
     * Checks if the given error code matches any known (i.e. publicly defined) error.
     *
     * @param errorCode An integer ID associated with the error.
     * @return Whether the error code matches a known error.
     */
    static boolean isKnownError(int errorCode) {
        switch (errorCode) {
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
            case BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks if the given error code indicates that the user has been (temporarily or permanently)
     * locked out from using biometric authentication, likely due to too many attempts.
     *
     * @param errorCode An integer ID associated with the error.
     * @return Whether the error code indicates that the user has been locked out.
     */
    static boolean isLockoutError(int errorCode) {
        return errorCode == BiometricPrompt.ERROR_LOCKOUT
                || errorCode == BiometricPrompt.ERROR_LOCKOUT_PERMANENT;
    }

    /**
     * Only needs to provide a subset of the fingerprint error strings since the rest are translated
     * in FingerprintManager
     */
    @NonNull
    static String getFingerprintErrorString(@Nullable Context context, int errorCode) {
        if (context == null) {
            return "";
        }

        // Only needs to provide a subset of the fingerprint error strings. The rest are translated
        // in FingerprintManager.
        switch (errorCode) {
            case BiometricPrompt.ERROR_HW_NOT_PRESENT:
                return context.getString(R.string.fingerprint_error_hw_not_present);
            case BiometricPrompt.ERROR_HW_UNAVAILABLE:
                return context.getString(R.string.fingerprint_error_hw_not_available);
            case BiometricPrompt.ERROR_NO_BIOMETRICS:
                return context.getString(R.string.fingerprint_error_no_fingerprints);
            case BiometricPrompt.ERROR_USER_CANCELED:
                return context.getString(R.string.fingerprint_error_user_canceled);
            case BiometricPrompt.ERROR_LOCKOUT:
            case BiometricPrompt.ERROR_LOCKOUT_PERMANENT:
                return context.getString(R.string.fingerprint_error_lockout);
            default:
                Log.e("BiometricUtils", "Unknown error code: " + errorCode);
                return context.getString(R.string.default_error_msg);
        }
    }
}
