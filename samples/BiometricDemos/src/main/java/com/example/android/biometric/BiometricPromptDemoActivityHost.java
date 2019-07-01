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

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Hosting activity for the BiometricPrompt demo. Shows how a biometric prompt can be launched
 * directly from an activity by hooking into appropriate lifecycle methods for the activity.
 */
public class BiometricPromptDemoActivityHost extends FragmentActivity {

    private BiometricPromptDemoController mController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.biometric_prompt_demo_content);
        final TextView hostTypeTextView = findViewById(R.id.host_type_text_view);
        hostTypeTextView.setText(R.string.label_host_type_activity);

        final Button createKeysButton = findViewById(R.id.button_enable_biometric_with_crypto);
        final Button authenticateButton = findViewById(R.id.button_authenticate);
        final Button canAuthenticateButton = findViewById(R.id.can_authenticate);
        final CheckBox useCryptoCheckbox = findViewById(R.id.checkbox_use_crypto);
        final CheckBox confirmationRequiredCheckbox = findViewById(
                R.id.checkbox_require_confirmation);
        final CheckBox deviceCredentialAllowedCheckbox = findViewById(
                R.id.checkbox_enable_fallback);
        final RadioGroup radioGroup = findViewById(R.id.radio_group);

        mController = new BiometricPromptDemoActivityController(
                this,
                createKeysButton,
                authenticateButton,
                canAuthenticateButton,
                useCryptoCheckbox,
                confirmationRequiredCheckbox,
                deviceCredentialAllowedCheckbox,
                radioGroup);
        mController.init(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mController.onResume();
    }

    @Override
    protected void onPause() {
        mController.onPause();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mController.onSaveInstanceState(outState);
    }
}
