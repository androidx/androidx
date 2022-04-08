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
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate functionality to selectively enable/disable Safe
 * Browsing for a subset of the application's {@link WebView}s. This shows three WebViews, to show
 * Safe Browsing enabled, disabled, and inheriting the default setting (see inline comment for how
 * this works).
 */
public class PerWebViewEnableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_per_web_view_enable);
        setTitle(R.string.per_web_view_enable_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        WebView enabledWebView = findViewById(R.id.enabled_webview);
        WebView disabledWebView = findViewById(R.id.disabled_webview);
        WebView defaultWebView = findViewById(R.id.default_webview);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(enabledWebView.getSettings(), true);
            WebSettingsCompat.setSafeBrowsingEnabled(disabledWebView.getSettings(), false);
            // defaultWebView will defer to the EnableSafeBrowsing metadata manifest tag. If that's
            // not set, then Safe Browsing will be enabled by default for WebViews >= M66, per
            // https://blog.chromium.org/2018/04/protecting-webview-with-safe-browsing.html.
        } else {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
        }

        enabledWebView.loadUrl(SafeBrowsingHelpers.MALWARE_URL);
        disabledWebView.loadUrl(SafeBrowsingHelpers.MALWARE_URL);
        defaultWebView.loadUrl(SafeBrowsingHelpers.MALWARE_URL);
    }
}
