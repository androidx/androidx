/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.support.design.widget;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.android.support.design.R;
import com.google.android.material.textfield.TextInputLayout;

/**
 * This demonstrates idiomatic usage of {@code TextInputLayout}
 */
public class TextInputLayoutUsage extends AppCompatActivity {

    private TextInputLayout mUsernameInputLayout;
    private TextInputLayout mPasswordInputLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_text_input);

        mUsernameInputLayout = findViewById(R.id.input_username);
        mPasswordInputLayout = findViewById(R.id.input_password);
    }

    public void showError(View view) {
        mUsernameInputLayout.setError("Some unknown error has occurred");
    }

    public void clearError(View view) {
        mUsernameInputLayout.setError(null);
    }

    public void setPasswordEnabled(View view) {
        mPasswordInputLayout.setPasswordVisibilityToggleEnabled(true);
    }

    public void setPasswordDisabled(View view) {
        mPasswordInputLayout.setPasswordVisibilityToggleEnabled(false);
    }

    public void toggleEnabled(View view) {
        mUsernameInputLayout.setEnabled(!mUsernameInputLayout.isEnabled());
    }

}
