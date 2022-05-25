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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate Safe Browsing behavior with a {@link WebView}
 * instance which is detached from the view hierarchy. This behaves identically to {@link
 * InvisibleActivity}: the WebView emits a network error with error code {@link
 * android.webkit.WebViewClient#ERROR_UNSAFE_RESOURCE}.
 *
 * @see InvisibleActivity
 */
public class UnattachedActivity extends AppCompatActivity {

    private WebView mUnattachedWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unattached);
        setTitle(R.string.unattached_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        mUnattachedWebView = new WebView(this);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(mUnattachedWebView.getSettings(), true);
        }
        TextView netErrorTextView = findViewById(R.id.net_errors);
        mUnattachedWebView.setWebViewClient(new ErrorLoggingWebViewClient(netErrorTextView));
        mUnattachedWebView.loadUrl(SafeBrowsingHelpers.MALWARE_URL);
    }
}
