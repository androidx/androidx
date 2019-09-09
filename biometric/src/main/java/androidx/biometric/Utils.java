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
     *                 launch the CDC activity and will instead log an error.
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

        // Get the KeyguardManager service in whichever way the platform supports.
        final KeyguardManager keyguardManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            keyguardManager = activity.getSystemService(KeyguardManager.class);
        } else {
            final Object service = activity.getSystemService(Context.KEYGUARD_SERVICE);
            if (!(service instanceof KeyguardManager)) {
                Log.e(loggingTag, "Failed to check device credential. KeyguardManager not found.");
                return;
            }
            keyguardManager = (KeyguardManager) service;
        }

        if (keyguardManager == null) {
            Log.e(loggingTag, "Failed to check device credential. KeyguardManager was null.");
            return;
        }

        // There's no longer a chance of returning early, so run the onLaunch callback.
        if (onLaunch != null) {
            onLaunch.run();
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

        // Prevent the bridge from resetting until the confirmation activity finishes.
        DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (bridge != null) {
            bridge.startIgnoringReset();
        }

        // Launch a new instance of the confirm device credential Settings activity.
        @SuppressWarnings("deprecation")
        final Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(title, subtitle);
        intent.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        activity.startActivityForResult(intent, 0 /* requestCode */);
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
}
