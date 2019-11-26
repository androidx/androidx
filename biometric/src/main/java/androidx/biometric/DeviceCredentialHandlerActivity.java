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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Transparent activity that is responsible for re-launching the {@link BiometricPrompt} and
 * handling results from {@link android.app.KeyguardManager#createConfirmDeviceCredentialIntent(
 * CharSequence, CharSequence)} in order to allow device credential authentication prior to Q.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("SyntheticAccessor")
public class DeviceCredentialHandlerActivity extends AppCompatActivity {
    private static final String TAG = "DeviceCredentialHandler";

    private static final String KEY_DID_CHANGE_CONFIGURATION = "did_change_configuration";

    static final String EXTRA_PROMPT_INFO_BUNDLE = "prompt_info_bundle";

    private boolean mDidChangeConfiguration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        final DeviceCredentialHandlerBridge bridge = DeviceCredentialHandlerBridge.getInstance();

        // Apply the client activity's theme to ensure proper dialog styling.
        if (bridge.getClientThemeResId() != 0) {
            setTheme(bridge.getClientThemeResId());
            getTheme().applyStyle(R.style.TransparentStyle, true /* force */);
        }

        // Must be called after setting the theme.
        super.onCreate(savedInstanceState);

        // Don't reset the bridge when recreating from a configuration change.
        mDidChangeConfiguration = savedInstanceState != null
                    && savedInstanceState.getBoolean(KEY_DID_CHANGE_CONFIGURATION, false);
        if (!mDidChangeConfiguration) {
            bridge.stopIgnoringReset();
        } else {
            mDidChangeConfiguration = false;
        }

        setTitle(null);
        setContentView(R.layout.device_credential_handler_activity);

        if (bridge.getExecutor() == null || bridge.getAuthenticationCallback() == null) {
            Log.e(TAG, "onCreate: Executor and/or callback was null!");
            finish();
        } else {
            // (Re)connect to and launch a biometric prompt within this activity.
            final BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                    bridge.getExecutor(), bridge.getAuthenticationCallback());
            final Bundle infoBundle = getIntent().getBundleExtra(EXTRA_PROMPT_INFO_BUNDLE);
            final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo(infoBundle);
            biometricPrompt.authenticate(info);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Prevent the client from resetting the bridge in onPause if just changing configuration.
        final DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (isChangingConfigurations() && bridge != null) {
            bridge.ignoreNextReset();
            mDidChangeConfiguration = true;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_DID_CHANGE_CONFIGURATION, mDidChangeConfiguration);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        handleDeviceCredentialResult(resultCode);
    }

    /**
     * Handles a result from the confirm device credential Settings activity.
     *
     * @param resultCode The (actual or simulated) result code from the device credential
     *                   Settings activity. Typically, either {@link android.app.Activity#RESULT_OK}
     *                   or {@link android.app.Activity#RESULT_CANCELED}.
     */
    void handleDeviceCredentialResult(int resultCode) {
        final DeviceCredentialHandlerBridge bridge =
                DeviceCredentialHandlerBridge.getInstanceIfNotNull();
        if (bridge == null) {
            Log.e(TAG, "onActivityResult: Bridge or callback was null!");
        } else if (resultCode == RESULT_OK) {
            bridge.setDeviceCredentialResult(DeviceCredentialHandlerBridge.RESULT_SUCCESS);
            bridge.setConfirmingDeviceCredential(false);
        } else {
            // Treat any non-OK result as a user cancellation.
            bridge.setDeviceCredentialResult(DeviceCredentialHandlerBridge.RESULT_ERROR);
            bridge.setConfirmingDeviceCredential(false);
        }

        finish();
    }
}
