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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * Hosting fragment for the BiometricPrompt demo. Shows how a biometric prompt can be launched
 * from an activity-hosted fragment by hooking into appropriate lifecycle methods for the fragment.
 */
public class BiometricPromptDemoFragmentHost extends DialogFragment {

    private BiometricPromptDemoController mController;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.biometric_prompt_demo_content, container, false);
        final TextView hostTypeTextView = view.findViewById(R.id.host_type_text_view);
        hostTypeTextView.setText(R.string.label_host_type_fragment);

        final Button createKeysButton = view.findViewById(R.id.button_enable_biometric_with_crypto);
        final Button authenticateButton = view.findViewById(R.id.button_authenticate);
        final Button canAuthenticateButton = view.findViewById(R.id.can_authenticate);
        final CheckBox useCryptoCheckbox = view.findViewById(R.id.checkbox_use_crypto);
        final CheckBox confirmationRequiredCheckbox = view.findViewById(
                R.id.checkbox_require_confirmation);
        final CheckBox deviceCredentialAllowedCheckbox = view.findViewById(
                R.id.checkbox_enable_fallback);
        final RadioGroup radioGroup = view.findViewById(R.id.radio_group);

        mController = new BiometricPromptDemoFragmentController(
                this,
                createKeysButton,
                authenticateButton,
                canAuthenticateButton,
                useCryptoCheckbox,
                confirmationRequiredCheckbox,
                deviceCredentialAllowedCheckbox,
                radioGroup);
        mController.init(savedInstanceState);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mController.onResume();
    }

    @Override
    public void onPause() {
        mController.onPause();
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mController.onSaveInstanceState(outState);
    }
}
