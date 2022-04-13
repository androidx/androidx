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

package com.example.androidx.webkit;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate IsMultiProcessEnabled query.
 */
public class MultiProcessEnabledActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_is_multi_process_enabled);
        setTitle(R.string.multi_process_enabled_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROCESS)) {
            if (WebViewCompat.isMultiProcessEnabled()) {
                WebkitHelpers.showMessageInActivity(MultiProcessEnabledActivity.this,
                        R.string.multi_process_enabled);
            } else {
                WebkitHelpers.showMessageInActivity(MultiProcessEnabledActivity.this,
                        R.string.multi_process_disabled);
            }
        } else {
            WebkitHelpers.showMessageInActivity(MultiProcessEnabledActivity.this,
                    R.string.multi_process_unavailable);
        }
    }
}
