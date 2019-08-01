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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
@RequiresApi(Build.VERSION_CODES.P)
@SuppressLint("SyntheticAccessor")
public class DeviceCredentialHandlerActivity extends AppCompatActivity {
    private static final String TAG = "DeviceCredentialHandler";

    static final String EXTRA_PROMPT_INFO_BUNDLE = "prompt_info_bundle";

    @Nullable
    private DeviceCredentialHandlerBridge mBridge;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(null);
        setContentView(R.layout.device_credential_handler_activity);

        mBridge = DeviceCredentialHandlerBridge.getInstance();
        if (mBridge.getExecutor() == null || mBridge.getAuthenticationCallback() == null) {
            Log.e(TAG, "onCreate: Executor and/or callback was null!");
        } else {
            final BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                    mBridge.getExecutor(), mBridge.getAuthenticationCallback());
            final Bundle infoBundle = getIntent().getBundleExtra(EXTRA_PROMPT_INFO_BUNDLE);
            final BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo(infoBundle);
            biometricPrompt.authenticate(info);
        }
    }

    // Handles the result of startActivity invoked by the attached BiometricPrompt.
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle result from ConfirmDeviceCredentialActivity.
        if (mBridge == null || mBridge.getAuthenticationCallback() == null) {
            Log.e(TAG, "onActivityResult: Bridge or callback was null!");
        } else if (resultCode == RESULT_OK) {
            mBridge.getAuthenticationCallback().onAuthenticationSucceeded(
                    new BiometricPrompt.AuthenticationResult(null /* crypto */));
        } else {
            // Treat any non-OK result as a user cancellation.
            mBridge.getAuthenticationCallback().onAuthenticationError(
                    BiometricConstants.ERROR_USER_CANCELED,
                    getString(R.string.generic_error_user_canceled));
        }

        finish();
    }
}
