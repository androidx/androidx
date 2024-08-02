/*
 * Copyright 2024 The Android Open Source Project
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
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} to demonstrate tiny interstitials.
 * <p>
 * For Restricted Content blocking, WebView just shows a grey page with no signs, text or
 * links when its size is tiny.
 */
public class TinyInterstitialActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tiny_interstitial);
        setTitle(R.string.tiny_interstitial_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        WebView webview = findViewById(R.id.tiny_webview);
        webview.loadUrl(RestrictedContentHelpers.RESTRICTED_CONTENT_URL);
    }
}
