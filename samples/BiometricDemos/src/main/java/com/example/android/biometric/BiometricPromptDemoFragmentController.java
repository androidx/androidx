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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

class BiometricPromptDemoFragmentController extends BiometricPromptDemoController {

    private final Fragment mFragment;

    BiometricPromptDemoFragmentController(
            @NonNull Fragment fragment,
            @NonNull Button createKeysButton,
            @NonNull Button authenticateButton,
            @NonNull Button canAuthenticateButton,
            @NonNull CheckBox useCryptoCheckbox,
            @NonNull CheckBox confirmationRequiredCheckbox,
            @NonNull CheckBox deviceCredentialAllowedCheckbox,
            @NonNull RadioGroup radioGroup) {
        mFragment = fragment;
        mCreateKeysButton = createKeysButton;
        mAuthenticateButton = authenticateButton;
        mCanAuthenticateButton = canAuthenticateButton;
        mUseCryptoCheckbox = useCryptoCheckbox;
        mConfirmationRequiredCheckbox = confirmationRequiredCheckbox;
        mDeviceCredentialAllowedCheckbox = deviceCredentialAllowedCheckbox;
        mRadioGroup = radioGroup;
    }

    @Override
    Context getApplicationContext() {
        return mFragment.getContext().getApplicationContext();
    }

    @Override
    void onResume() {
        // Developers should (re)create the BiometricPrompt every time the fragment is resumed.
        // This is necessary because it is possible for the executor and callback to be GC'd.
        // Instantiating the prompt here allows the library to handle things such as configuration
        // changes.
        mBiometricPrompt = new BiometricPrompt(mFragment.getActivity(), mExecutor,
                    mAuthenticationCallback);
    }
}
