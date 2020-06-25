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

package androidx.biometric;

import android.os.Build;

import androidx.biometric.BiometricManager.Authenticators;

/**
 * Utilities related to {@link BiometricManager.Authenticators} constants.
 */
class AuthenticatorUtils {
    // Prevent instantiation.
    private AuthenticatorUtils() {}

    /**
     * Checks if the given set of allowed authenticator types is supported on this Android version.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether user authentication with the given set of allowed authenticator types is
     * supported on the current Android version.
     */
    static boolean isSupportedCombination(@BiometricManager.AuthenticatorTypes int authenticators) {
        switch (authenticators) {
            case 0:
            case Authenticators.BIOMETRIC_STRONG:
            case Authenticators.BIOMETRIC_WEAK:
            case Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL:
                return true;

            // A biometric can be used instead of device credential prior to API 30.
            case Authenticators.DEVICE_CREDENTIAL:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;

            // A Class 2 (Weak) biometric can be used instead of device credential on API 28-29.
            case Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL:
                return Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                        || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q;

            default:
                return false;
        }
    }

    /**
     * Checks if a device credential is included in the given set of allowed authenticator types.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether {@link Authenticators#DEVICE_CREDENTIAL} is an allowed authenticator type.
     */
    static boolean isDeviceCredentialAllowed(
            @BiometricManager.AuthenticatorTypes int authenticators) {
        return (authenticators & Authenticators.DEVICE_CREDENTIAL) != 0;
    }

    /**
     * Checks if a <strong>Class 2</strong> (formerly <strong>Weak</strong>) biometric is included
     * in the given set of allowed authenticator types.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether {@link Authenticators#BIOMETRIC_WEAK} is an allowed authenticator type.
     */
    static boolean isWeakBiometricAllowed(@BiometricManager.AuthenticatorTypes int authenticators) {
        return (authenticators & Authenticators.BIOMETRIC_WEAK) == Authenticators.BIOMETRIC_WEAK;
    }
}
