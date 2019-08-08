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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * Hosting fragment for the BiometricPrompt demo. Shows how a biometric prompt can be launched
 * from an activity-hosted fragment by hooking into appropriate lifecycle methods for the fragment.
 */
public class BiometricPromptDemoFragmentHost extends DialogFragment {

    private Context mContext;
    private BiometricPromptDemoController mController;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.biometric_prompt_demo_content, container, false);
        final TextView hostTypeTextView = view.findViewById(R.id.host_type_text_view);
        hostTypeTextView.setText(R.string.label_host_type_fragment);

        mController = new BiometricPromptDemoFragmentController(mContext, this, view);
        mController.init(savedInstanceState);
        mController.reconnect();
        return view;
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
