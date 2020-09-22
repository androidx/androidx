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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager.Authenticators;

/**
 * Utilities related to {@link BiometricManager.Authenticators} constants.
 */
class AuthenticatorUtils {
    /**
     * A bitmask for the portion of an {@link BiometricManager.AuthenticatorTypes} value related to
     * biometric sensor class.
     */
    private static final int BIOMETRIC_CLASS_MASK = 0x7FFF;

    // Prevent instantiation.
    private AuthenticatorUtils() {}

    /**
     * Converts the given set of allowed authenticator types to a unique, developer-readable string.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return A string that uniquely identifies the set of authenticators and can be used in
     * developer-facing contexts (e.g. error messages).
     */
    static String convertToString(@BiometricManager.AuthenticatorTypes int authenticators) {
        switch (authenticators) {
            case Authenticators.BIOMETRIC_STRONG:
                return "BIOMETRIC_STRONG";
            case Authenticators.BIOMETRIC_WEAK:
                return "BIOMETRIC_WEAK";
            case Authenticators.DEVICE_CREDENTIAL:
                return "DEVICE_CREDENTIAL";
            case Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL:
                return "BIOMETRIC_STRONG | DEVICE_CREDENTIAL";
            case Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL:
                return "BIOMETRIC_WEAK | DEVICE_CREDENTIAL";
            default:
                return String.valueOf(authenticators);
        }
    }

    /**
     * Combines relevant information from the given {@link BiometricPrompt.PromptInfo} and
     * {@link BiometricPrompt.CryptoObject} to determine which type(s) of authenticators should be
     * allowed for a given authentication session.
     *
     * @param info   The {@link BiometricPrompt.PromptInfo} for a given authentication session.
     * @param crypto The {@link BiometricPrompt.CryptoObject} for a given crypto-based
     *               authentication session, or {@code null} for non-crypto authentication.
     * @return A bit field representing all valid authenticator types that may be invoked.
     */
    @SuppressWarnings("deprecation")
    @BiometricManager.AuthenticatorTypes
    static int getConsolidatedAuthenticators(
            @NonNull BiometricPrompt.PromptInfo info,
            @Nullable BiometricPrompt.CryptoObject crypto) {

        @BiometricManager.AuthenticatorTypes int authenticators;
        if (info.getAllowedAuthenticators() != 0) {
            // Use explicitly allowed authenticators if set.
            authenticators = info.getAllowedAuthenticators();
        } else {
            // Crypto auth requires a Class 3 (Strong) biometric.
            authenticators = crypto != null
                    ? Authenticators.BIOMETRIC_STRONG
                    : Authenticators.BIOMETRIC_WEAK;

            if (info.isDeviceCredentialAllowed()) {
                authenticators |= Authenticators.DEVICE_CREDENTIAL;
            }
        }

        return authenticators;
    }

    /**
     * Checks if the given set of allowed authenticator types is supported on this Android version.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether user authentication with the given set of allowed authenticator types is
     * supported on the current Android version.
     */
    static boolean isSupportedCombination(@BiometricManager.AuthenticatorTypes int authenticators) {
        switch (authenticators) {
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
                // 0 means "no authenticator types" and is supported. Other values are not.
                return authenticators == 0;
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
     * Checks if any biometric class is included in the given set of allowed authenticator types.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return Whether the allowed authenticator types include one or more biometric classes.
     */
    static boolean isSomeBiometricAllowed(@BiometricManager.AuthenticatorTypes int authenticators) {
        return (authenticators & BIOMETRIC_CLASS_MASK) != 0;
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
