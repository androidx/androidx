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
import android.os.Build;

import androidx.annotation.NonNull;
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
     * Determines if the current device should explicitly fall back to using
     * {@link FingerprintDialogFragment} and {@link FingerprintHelperFragment} when
     * {@link BiometricPrompt#authenticate(BiometricPrompt.PromptInfo,
     * BiometricPrompt.CryptoObject)} is called.
     *
     * @param context The application or activity context.
     * @param vendor Name of the device vendor/manufacturer.
     * @param model Model name of the current device.
     * @return true if the current device should fall back to fingerprint for crypto-based
     * authentication, or false otherwise.
     */
    static boolean shouldUseFingerprintForCrypto(@NonNull Context context, String vendor,
            String model) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            // This workaround is only needed for API 28.
            return false;
        }
        return isVendorInList(context, vendor, R.array.crypto_fingerprint_fallback_vendors)
            || isModelInPrefixList(context, model, R.array.crypto_fingerprint_fallback_prefixes);
    }

    /**
     * Determines if the current device requires {@link FingerprintDialogFragment} to always be
     * dismissed immediately upon receiving an error or cancel signal (e.g., if the dialog is
     * shown behind an overlay that sends a cancel signal when it is dismissed).
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @return true if {@link FingerprintDialogFragment} should always be dismissed immediately, or
     * false otherwise.
     */
    static boolean shouldHideFingerprintDialog(@NonNull Context context, String model) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            // This workaround is only needed for API 28.
            return false;
        }
        return isModelInPrefixList(context, model, R.array.hide_fingerprint_instantly_prefixes);
    }

    /**
     * Determines if the name of the current device vendor matches one in the given string array
     * resource.
     *
     * @param context The application or activity context.
     * @param vendor Case-insensitive name of the device vendor.
     * @param resId Resource ID for the string array of vendor names to check against.
     * @return true if the vendor name matches one in the given string array, or false otherwise.
     */
    private static boolean isVendorInList(@NonNull Context context, String vendor, int resId) {
        if (vendor == null) {
            return false;
        }

        final String[] vendorNames = context.getResources().getStringArray(resId);
        for (final String vendorName : vendorNames) {
            if (vendor.equalsIgnoreCase(vendorName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the current device model matches a prefix in the given string array resource.
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @param resId Resource ID for the string array of device model prefixes to check against.
     * @return true if the model matches one in the given string array, or false otherwise.
     */
    private static boolean isModelInPrefixList(@NonNull Context context, String model, int resId) {
        if (model == null) {
            return false;
        }

        final String[] modelPrefixes = context.getResources().getStringArray(resId);
        for (final String modelPrefix : modelPrefixes) {
            if (model.startsWith(modelPrefix)) {
                return true;
            }
        }
        return false;
    }
}
