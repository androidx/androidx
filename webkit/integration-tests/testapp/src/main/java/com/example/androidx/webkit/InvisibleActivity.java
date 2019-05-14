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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate Safe Browsing behavior with a {@link WebView}
 * instance which is attached to the view hierarchy, but marked as {@link
 * android.view.View#INVISIBLE}. In this case, the WebView will
 * immediately emit a network error via {@link android.webkit.WebViewClient#onReceivedError(WebView,
 * WebResourceRequest, WebResourceError)} (or the other {@code #onReceivedError} overload). The
 * network error code should be {@link android.webkit.WebViewClient#ERROR_UNSAFE_RESOURCE}.
 *
 * @see UnattachedActivity
 */
public class InvisibleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invisible);
        setTitle(R.string.invisible_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        WebView invisibleWebView = findViewById(R.id.invisible_webview);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(invisibleWebView.getSettings(), true);
        }
        TextView netErrorTextView = findViewById(R.id.net_errors);
        invisibleWebView.setWebViewClient(new ErrorLoggingWebViewClient(netErrorTextView));
        invisibleWebView.loadUrl(SafeBrowsingHelpers.MALWARE_URL);
    }
}
