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

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.FragmentActivity;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
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
     * Launches the confirm device credential (CDC) Settings activity to allow the user to
     * authenticate with their device credential (PIN/pattern/password) on Android P and below.
     *
     * @param loggingTag The tag to be used for logging events.
     * @param activity Activity that will launch the CDC activity and handle its result. Should be
     *                 {@link DeviceCredentialHandlerActivity}; all other activities will fail to
     *                 launch the CDC activity and instead log an error.
     * @param bundle Bundle of extras forwarded from {@link BiometricPrompt}.
     * @param onLaunch Optional callback to be run before launching the new activity.
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    static void launchDeviceCredentialConfirmation(
            @NonNull String loggingTag, @Nullable FragmentActivity activity,
            @Nullable Bundle bundle, @Nullable Runnable onLaunch) {
        if (!(activity instanceof DeviceCredentialHandlerActivity)) {
            Log.e(loggingTag, "Failed to check device credential. Parent handler not found.");
            return;
        }
        final DeviceCredentialHandlerActivity handlerActivity =
                (DeviceCredentialHandlerActivity) activity;

        // Get the KeyguardManager service in whichever way the platform supports.
        final KeyguardManager keyguardManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager = handlerActivity.getSystemService(KeyguardManager.class);
        } else {
            final Object service = handlerActivity.getSystemService(Context.KEYGUARD_SERVICE);
            if (!(service instanceof KeyguardManager)) {
                Log.e(loggingTag, "Failed to check device credential. KeyguardManager not found.");
                handlerActivity.handleDeviceCredentialResult(Activity.RESULT_CANCELED);
                return;
            }
            keyguardManager = (KeyguardManager) service;
        }

        if (keyguardManager == null) {
            Log.e(loggingTag, "Failed to check device credential. KeyguardManager was null.");
            handlerActivity.handleDeviceCredentialResult(Activity.RESULT_CANCELED);
            return;
        }

        // Pass along the title and subtitle from the biometric prompt.
        final CharSequence title;
        final CharSequence subtitle;
        if (bundle != null) {
            title = bundle.getCharSequence(BiometricPrompt.KEY_TITLE);
            subtitle = bundle.getCharSequence(BiometricPrompt.KEY_SUBTITLE);
        } else {
            title = null;
            subtitle = null;
        }

        @SuppressWarnings("deprecation")
        final Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(title, subtitle);
        if (intent == null) {
            Log.e(loggingTag, "Failed to check device credential. Got null intent from Keyguard.");
            handlerActivity.handleDeviceCredentialResult(Activity.RESULT_CANCELED);
            return;
        }

        // Prevent the bridge from resetting until the confirmation activity finishes.
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();
        bridge.setConfirmingDeviceCredential(true);
        bridge.startIgnoringReset();

        // Run callback after the CDC flag is set but before launching the activity.
        if (onLaunch != null) {
            onLaunch.run();
        }

        // Launch a new instance of the confirm device credential Settings activity.
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        handlerActivity.startActivityForResult(intent, 0 /* requestCode */);
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
     * @param model Model name of the current device.
     * @return true if the current device should fall back to fingerprint for crypto-based
     * authentication, or false otherwise.
     */
    static boolean shouldUseFingerprintForCrypto(@NonNull Context context, String model) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            // This workaround is only needed for Android P and Q.
            return false;
        }
        return isModelInPrefixList(context, model, R.array.crypto_fingerprint_fallback_prefixes);
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
    static boolean shouldAlwaysHideFingerprintDialogInstantly(@NonNull Context context,
            String model) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                || Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            // This workaround is only needed for Android P and Q.
            return false;
        }
        return isModelInPrefixList(context, model, R.array.hide_fingerprint_instantly_prefixes);
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
