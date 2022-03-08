/*
 * Copyright 2022 The Android Open Source Project
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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import com.google.common.collect.ImmutableMap;

/** An {@link android.app.Activity} to demonstrate the getVariationsHeader() API. */
public class GetVariationsHeaderActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_variations_header);
        setTitle(R.string.variations_header_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.GET_VARIATIONS_HEADER)) {
            TextView textView = findViewById(R.id.textview);
            WebView webView = findViewById(R.id.webview);
            String headerValue = WebViewCompat.getVariationsHeader();
            textView.setText(getString(R.string.variations_header_message, headerValue));
            webView.loadUrl(
                    "https://www.google.com", ImmutableMap.of("X-Client-Data", headerValue));
        } else {
            WebkitHelpers.showMessageInActivity(
                    GetVariationsHeaderActivity.this, R.string.variations_header_unavailable);
        }
    }
}
