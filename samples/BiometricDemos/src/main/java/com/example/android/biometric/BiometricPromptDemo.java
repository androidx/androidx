/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Demo for BiometricPrompt, demonstrating: 1) Use of biometrics without a crypto object. 2) Use
 * of biometrics with a crypto object, where the keys become invalid after new biometric enrollment.
 * 3) Use of biometric prompt (compat) that persists through orientation changes. 4) Use of
 * biometric prompt (compat) that doesn't persist through orientation changes. 5) Cancellation of
 * the authentication attempt.
 */
public class BiometricPromptDemo extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biometric_prompt_demo);
        final Button hostInActivityButton = findViewById(R.id.host_in_activity_button);
        final Button hostInFragmentButton = findViewById(R.id.host_in_fragment_button);
        hostInActivityButton.setOnClickListener(view -> launchActivityHost());
        hostInFragmentButton.setOnClickListener(view -> launchFragmentHost());
    }

    private void launchActivityHost() {
        Intent intent = new Intent(this, BiometricPromptDemoActivityHost.class);
        startActivity(intent);
    }

    private void launchFragmentHost() {
        Intent intent = new Intent(this, BiometricPromptDemoFragmentHostActivity.class);
        startActivity(intent);
    }
}
