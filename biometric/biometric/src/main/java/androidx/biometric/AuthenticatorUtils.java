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

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.biometric.BiometricManager.Authenticators;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    private AuthenticatorUtils() {
    }

    /**
     * Converts the given set of allowed authenticator types to a unique, developer-readable string.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return A string that uniquely identifies the set of authenticators and can be used in
     * developer-facing contexts (e.g. error messages).
     */
    @SuppressLint("WrongConstant")
    static String convertToString(@BiometricManager.AuthenticatorTypes int authenticators) {
        String result;
        switch (authenticators & ~Authenticators.IDENTITY_CHECK) {
            case Authenticators.BIOMETRIC_STRONG:
                result = "BIOMETRIC_STRONG";
                break;
            case Authenticators.BIOMETRIC_WEAK:
                result = "BIOMETRIC_WEAK";
                break;
            case Authenticators.DEVICE_CREDENTIAL:
                result = "DEVICE_CREDENTIAL";
                break;
            case Authenticators.BIOMETRIC_STRONG | Authenticators.DEVICE_CREDENTIAL:
                result = "BIOMETRIC_STRONG | DEVICE_CREDENTIAL";
                break;
            case Authenticators.BIOMETRIC_WEAK | Authenticators.DEVICE_CREDENTIAL:
                result = "BIOMETRIC_WEAK | DEVICE_CREDENTIAL";
                break;
            default:
                result = String.valueOf(authenticators);
        }
        if ((authenticators & Authenticators.IDENTITY_CHECK) == Authenticators.IDENTITY_CHECK) {
            if (authenticators != Authenticators.IDENTITY_CHECK) {
                result += " | ";
            }
            result += "IDENTITY_CHECK";
        }
        return result;
    }

    /**
     * Removes non-biometric authenticator types from the given set of allowed authenticators.
     *
     * @param authenticators A bit field representing a set of allowed authenticator types.
     * @return A bit field representing the allowed biometric authenticator types.
     */
    @BiometricManager.AuthenticatorTypes
    static int getBiometricAuthenticators(@BiometricManager.AuthenticatorTypes int authenticators) {
        return authenticators & BIOMETRIC_CLASS_MASK;
    }

    /**
     * Combines relevant information from the given {@link BiometricPrompt.PromptInfo} and
     * {@link BiometricPrompt.CryptoObject} to determine which type(s) of authenticators should be
     * allowed for a given authentication session.
     *
     * @param info                     The {@link BiometricPrompt.PromptInfo} for a given
     *                                 authentication session.
     * @param crypto                   The {@link BiometricPrompt.CryptoObject} for a given
     *                                 crypto-based
     *                                 authentication session, or {@code null} for non-crypto
     *                                 authentication.
     * @param isIdentityCheckAvailable Whether Identity check is available in the current api
     *                                 level. If not, ignore identity check from authenticators.
     * @return A bit field representing all valid authenticator types that may be invoked.
     */
    @SuppressLint("WrongConstant")
    @SuppressWarnings("deprecation")
    @BiometricManager.AuthenticatorTypes
    static int getConsolidatedAuthenticators(
            BiometricPrompt.@NonNull PromptInfo info,
            BiometricPrompt.@Nullable CryptoObject crypto,
            boolean isIdentityCheckAvailable) {
        if (info == null) {
            return 0;
        }

        // Use explicitly allowed authenticators if set.
        @BiometricManager.AuthenticatorTypes int authenticators = info.getAllowedAuthenticators();

        //  We don't want identity check to block the authentication, so ignore identity check if
        //  it's not available: a. if there are other authenticators set by the app, identity
        //  check will be ignored and use the others only; b. if not, the default authenticator
        //  will be used from above.
        // TODO(b/375693808): Add this information to setAllowedAuthenticators() doc.
        if ((authenticators & Authenticators.IDENTITY_CHECK) == Authenticators.IDENTITY_CHECK
                && !isIdentityCheckAvailable) {
            authenticators &= ~Authenticators.IDENTITY_CHECK;
        }

        if (authenticators == 0) {
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
    @SuppressLint("WrongConstant")
    static boolean isSupportedCombination(@BiometricManager.AuthenticatorTypes int authenticators) {
        // Ignore identity check. We don't want identity check to block the authentication. See
        // getConsolidatedAuthenticators() for more information.
        // TODO(b/375693808): Add this information to setAllowedAuthenticators() doc.
        authenticators &= ~Authenticators.IDENTITY_CHECK;

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
        return getBiometricAuthenticators(authenticators) != 0;
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
