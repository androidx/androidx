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

package com.example.android.biometric;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executor;

/**
 * Controller for the biometric prompt demo app. Coordinates the logic of initializing buttons,
 * generating the info, and launching the prompt itself.
 */
abstract class BiometricPromptDemoController {

    private static final String TAG = "bio_prompt_demo_control";

    private static final String KEY_COUNTER = "saved_counter";
    private static final String KEY_LOG = "saved_log";

    private static final String BIOMETRIC_SUCCESS_MESSAGE = "BIOMETRIC_SUCCESS_MESSAGE";
    private static final String BIOMETRIC_ERROR_HW_UNAVAILABLE_MESSAGE =
            "BIOMETRIC_ERROR_HW_UNAVAILABLE";
    private static final String BIOMETRIC_ERROR_NONE_ENROLLED_MESSAGE =
            "BIOMETRIC_ERROR_NONE_ENROLLED";
    private static final String BIOMETRIC_ERROR_NO_HARDWARE =
            "BIOMETRIC_ERROR_NO_HARDWARE";
    private static final String BIOMETRIC_ERROR_UNKNOWN = "Error unknown return result";

    private static final int MODE_NONE = 0;
    private static final int MODE_PERSIST_ACROSS_CONFIGURATION_CHANGES = 1;
    private static final int MODE_CANCEL_ON_CONFIGURATION_CHANGE = 2;
    private static final int MODE_CANCEL_AFTER_THREE_FAILURES = 3;

    BiometricPrompt mBiometricPrompt;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    final Executor mExecutor = mHandler::post;
    private int mCounter;

    private final Button mCanAuthenticateButton;
    private final Button mAuthenticateButton;
    private final CheckBox mRequireConfirmationCheckbox;
    private final CheckBox mAllowDeviceCredentialCheckbox;
    private final RadioGroup mBiometricPromptConfigurationRadioGroup;

    private final TextView mLogView;

    final BiometricPrompt.AuthenticationCallback mAuthenticationCallback =
            new BiometricPrompt.AuthenticationCallback() {
                private int mNumberFailedAttempts = 0;

                @Override
                public void onAuthenticationError(int err, @NonNull CharSequence message) {
                    log("onAuthenticationError " + err + ": " + message);
                    mNumberFailedAttempts = 0;
                }

                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    log("onAuthenticationSucceeded");
                    mNumberFailedAttempts = 0;
                }

                @Override
                public void onAuthenticationFailed() {
                    log("onAuthenticationFailed");
                    mNumberFailedAttempts++;

                    // Cancel authentication after 3 failed attempts to test the cancel() method.
                    if (getMode() == MODE_CANCEL_AFTER_THREE_FAILURES
                            && mNumberFailedAttempts == 3) {
                        mBiometricPrompt.cancelAuthentication();
                    }
                }
            };

    abstract Context getApplicationContext();

    /**
     * (Re)establish a connection between the host fragment/activity and the BiometricPrompt
     * library.
     *
     * Developers should (re)create the BiometricPrompt every time the activity/fragment is
     * created. This allows authentication to work across configuration changes. The internal
     * implementation of the library uses fragments, which can be saved/restored. Instantiating
     * the library with a new callback and executor early in the fragment/activity lifecycle (e.g.
     * onCreate or onCreateView) allows the new instance to receive callbacks properly.
     */
    abstract void reconnect();

    BiometricPromptDemoController(@NonNull View inflatedRootView) {
        mCanAuthenticateButton = inflatedRootView.findViewById(R.id.can_authenticate);
        mAuthenticateButton = inflatedRootView.findViewById(R.id.button_authenticate);
        mRequireConfirmationCheckbox = inflatedRootView.findViewById(
                R.id.checkbox_require_confirmation);
        mAllowDeviceCredentialCheckbox = inflatedRootView.findViewById(
                R.id.checkbox_allow_device_credential);
        mBiometricPromptConfigurationRadioGroup = inflatedRootView.findViewById(
                R.id.radio_group_biometric_prompt_configuration);

        mLogView = inflatedRootView.findViewById(R.id.log_text);

        Button clearLogButton = inflatedRootView.findViewById(R.id.button_clear_log);
        clearLogButton.setOnClickListener((view1) -> mLogView.setText(""));
    }

    /** Sets up button callbacks and other state for the biometric prompt demo controller. */
    void init(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mCounter = savedInstanceState.getInt(KEY_COUNTER);
            mLogView.setText(savedInstanceState.getCharSequence(KEY_LOG));
        }
        mAuthenticateButton.setOnClickListener(v -> startAuthentication());

        mCanAuthenticateButton.setOnClickListener(v -> {
            BiometricManager biometricManager = BiometricManager.from(getApplicationContext());
            String message;
            switch (biometricManager.canAuthenticate()) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    message = BIOMETRIC_SUCCESS_MESSAGE;
                    break;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    message = BIOMETRIC_ERROR_HW_UNAVAILABLE_MESSAGE;
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                    message = BIOMETRIC_ERROR_NONE_ENROLLED_MESSAGE;
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    message = BIOMETRIC_ERROR_NO_HARDWARE;
                    break;
                default:
                    message = BIOMETRIC_ERROR_UNKNOWN;
            }
            log("canAuthenticate: " + message);
        });

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            mAllowDeviceCredentialCheckbox.setEnabled(false);
            mAllowDeviceCredentialCheckbox.setChecked(false);
        }
    }

    void onPause() {
        if (getMode() == MODE_CANCEL_ON_CONFIGURATION_CHANGE) {
            mBiometricPrompt.cancelAuthentication();
        }
    }

    void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(KEY_COUNTER, mCounter);
        outState.putCharSequence(KEY_LOG, mLogView.getText());
    }

    private void startAuthentication() {
        if (getMode() == MODE_NONE) {
            log("Select a test first");
            return;
        }

        // Build the biometric prompt info
        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Title " + mCounter)
                .setSubtitle("Subtitle " + mCounter)
                .setDescription(
                        "Lorem ipsum dolor sit amet, consecte etur adipisicing elit. "
                                + mCounter)
                .setConfirmationRequired(mRequireConfirmationCheckbox.isChecked());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && mAllowDeviceCredentialCheckbox.isChecked()) {
            builder.setDeviceCredentialAllowed(true);
        } else {
            builder.setNegativeButtonText("Negative Button " + mCounter);
        }
        BiometricPrompt.PromptInfo info = builder.build();
        mCounter++;

        // Show the biometric prompt.
        mBiometricPrompt.authenticate(info);
    }

    /**
     * @return The currently selected configuration.
     */
    private int getMode() {
        int id = mBiometricPromptConfigurationRadioGroup.getCheckedRadioButtonId();
        switch (id) {
            case R.id.radio_persist_across_configuration_changes:
                return MODE_PERSIST_ACROSS_CONFIGURATION_CHANGES;
            case R.id.radio_cancel_on_configuration_change:
                return MODE_CANCEL_ON_CONFIGURATION_CHANGE;
            case R.id.radio_cancel_after_three_failures:
                return MODE_CANCEL_AFTER_THREE_FAILURES;
            default:
                return MODE_NONE;
        }
    }

    private void log(String s) {
        Log.d(TAG, s);
        mLogView.append(s + '\n');
    }
}
