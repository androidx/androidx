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

/**
 * Utility class for specifying custom behavior based on the vendor and model of the device.
 */
class DeviceUtils {
    // Prevent instantiation.
    private DeviceUtils() {}

    /**
     * Checks if the current device should explicitly fall back to using
     * {@link FingerprintDialogFragment} when
     * {@link BiometricPrompt#authenticate(BiometricPrompt.PromptInfo,
     * BiometricPrompt.CryptoObject)} is called.
     *
     * @param context The application or activity context.
     * @param vendor Name of the device vendor/manufacturer.
     * @param model Model name of the current device.
     * @return Whether the current device should fall back to fingerprint for crypto-based
     *  authentication.
     */
    static boolean shouldUseFingerprintForCrypto(
            @NonNull Context context, String vendor, String model) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            // This workaround is only needed for API 28.
            return false;
        }
        return isVendorInList(context, vendor, R.array.crypto_fingerprint_fallback_vendors)
            || isModelInPrefixList(context, model, R.array.crypto_fingerprint_fallback_prefixes);
    }

    /**
     * Checks if the current device should hide {@link FingerprintDialogFragment} and ensure that
     * {@link FingerprintDialogFragment} is always dismissed immediately upon receiving an error or
     * cancel signal (e.g. if the dialog is shown behind an overlay).
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @return Whether the {@link FingerprintDialogFragment} should be hidden.
     */
    static boolean shouldHideFingerprintDialog(@NonNull Context context, String model) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P) {
            // This workaround is only needed for API 28.
            return false;
        }
        return isModelInPrefixList(context, model, R.array.hide_fingerprint_instantly_prefixes);
    }

    /**
     * Checks if the current device should delay showing a new biometric prompt when the previous
     * prompt was recently dismissed.
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @return Whether showing the prompt should be delayed after dismissal.
     */
    static boolean shouldDelayShowingPrompt(@NonNull Context context, String model) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
            // This workaround is only needed for API 29.
            return false;
        }
        return isModelInList(context, model, R.array.delay_showing_prompt_models);
    }

    /**
     * Checks if all biometric sensors for the current device can be assumed to meet the
     * <strong>Class 3</strong> (formerly <strong>Strong</strong>) security threshold.
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @return Whether the device can be assumed to have only <strong>Class 3</strong> biometrics.
     */
    static boolean canAssumeStrongBiometrics(@NonNull Context context, String model) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 (API 30) and above may downgrade a sensor's security class at runtime.
            return false;
        }
        return isModelInList(context, model, R.array.assume_strong_biometrics_models);
    }

    /**
     * Checks if the current device should directly invoke
     * {@link android.app.KeyguardManager#createConfirmDeviceCredentialIntent(CharSequence,
     * CharSequence)} for authentication when both <strong>Class 2</strong> (formerly
     * <strong>Weak</strong>) biometrics and device credentials (i.e. PIN, pattern, or password) are
     * allowed.
     *
     * @param context The application or activity context.
     * @param vendor Name of the device vendor/manufacturer.
     * @return Whether the device should use {@link android.app.KeyguardManager} for authentication
     * if both <strong>Class 2</strong> biometrics and device credentials are allowed.
     */
    static boolean shouldUseKeyguardManagerForBiometricAndCredential(
            @NonNull Context context, @Nullable String vendor) {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
            // This workaround is only needed for API 29.
            return false;
        }
        return isVendorInList(context, vendor, R.array.keyguard_biometric_and_credential_vendors);
    }

    /**
     * Checks if the name of the current device vendor matches one in the given string array
     * resource.
     *
     * @param context The application or activity context.
     * @param vendor Case-insensitive name of the device vendor.
     * @param resId Resource ID for the string array of vendor names to check against.
     * @return Whether the vendor name matches one in the string array.
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
     * Checks if the current device model matches a prefix in the given string array resource.
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @param resId Resource ID for the string array of device model prefixes to check against.
     * @return Whether the model matches a prefix in the string array.
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

    /**
     * Checks if the current device model matches one in the given string array resource.
     *
     * @param context The application or activity context.
     * @param model Model name of the current device.
     * @param resId Resource ID for the string array of device model prefixes to check against.
     * @return Whether the model matches one in the string array.
     */
    private static boolean isModelInList(@NonNull Context context, String model, int resId) {
        if (model == null) {
            return false;
        }

        final String[] modelNames = context.getResources().getStringArray(resId);
        for (final String modelName : modelNames) {
            if (model.equals(modelName)) {
                return true;
            }
        }
        return false;
    }
}
