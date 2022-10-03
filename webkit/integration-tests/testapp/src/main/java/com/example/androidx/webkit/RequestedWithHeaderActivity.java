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

import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.util.Collections;
import java.util.Set;

/**
 * Demo activity to show how to set an allow-list to preserve the legacy behavior for the {@code
 * X-Requested-With} header.
 */
public class RequestedWithHeaderActivity  extends AppCompatActivity {

    private HttpServer mServer;
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_requested_with_header);

        setTitle(R.string.requested_with_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        // Check if allow-list feature is enabled
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        // Initialize http server to show us what we requested
        mServer = new HttpServer(/*port=*/0, HttpServer.EchoRequestHandler::new, null);
        mServer.start();

        // Set up our WebView
        mWebView = findViewById(R.id.requested_with_header_webview);
        // Disable caching to always get fresh requests to the test server
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        // Get the current allow-list to initialize the UI
        Set<String> allowList = WebSettingsCompat.getRequestedWithHeaderOriginAllowList(
                mWebView.getSettings());

        // Wire up the UI
        RadioGroup radioGroup = findViewById(R.id.requested_with_header_radio_group);
        // Check the radio button corresponding to the current setting
        radioGroup.check(allowList.isEmpty()
                        ? R.id.requested_with_header_empty_mode
                        : R.id.requested_with_header_allowlist_mode);
        // Register a change handler
        radioGroup.setOnCheckedChangeListener(this::onRadioGroupChanged);

        // Send a request to our echo server to show the headers
        refreshView(!allowList.isEmpty());
    }

    private void refreshView(boolean useAllowList) {
        if (useAllowList) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(mWebView.getSettings(),
                    Collections.singleton("http://localhost:" + mServer.getPort()));
        } else {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(mWebView.getSettings(),
                    Collections.emptySet());
        }
        String requestUrl = new Uri.Builder()
                .scheme("http")
                .authority("localhost:" + mServer.getPort())
                .build()
                .toString();
        mWebView.loadUrl(requestUrl);
    }

    /**
     * Handler for selecting a new header mode through the radio group.
     * @param unused Triggering radio group
     * @param checkedId ID of checked radio button
     */
    public void onRadioGroupChanged(@NonNull RadioGroup unused, int checkedId) {
        refreshView(checkedId == R.id.requested_with_header_allowlist_mode);
    }

    @Override
    protected void onDestroy() {
        if (mServer != null) {
            mServer.shutdown();
        }
        super.onDestroy();
    }
}
