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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link Activity} to exercise Proxy Override functionality.
 */
public class ProxyOverrideActivity extends AppCompatActivity {
    private Proxy mProxy;
    private Button mSetProxyOverrideButton;
    private Button mLoadURLButton;
    private Button mLoadBypassURLButton;
    private WebView mWebView;
    private CheckBox mReverseBypassCheckBox;
    private TextView mRequestCountTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxy_override);
        setTitle(R.string.proxy_override_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        // Check for proxy override feature
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        // Initialize views
        mRequestCountTextView = findViewById(R.id.proxy_override_textview);
        mRequestCountTextView.setText(getResources().getString(
                R.string.proxy_override_requests_served, 0));
        mWebView = findViewById(R.id.proxy_override_webview);
        mWebView.setWebViewClient(new WebViewClientCompat());
        mReverseBypassCheckBox = findViewById(R.id.proxy_override_reverse_bypass_checkbox);
        mSetProxyOverrideButton = findViewById(R.id.proxy_override_button);
        mSetProxyOverrideButton.setOnClickListener(v -> {
            mReverseBypassCheckBox.setEnabled(false);
            mSetProxyOverrideButton.setEnabled(false);
            setProxyOverride();
        });
        mLoadURLButton = findViewById(R.id.proxy_override_load_url_button);
        mLoadURLButton.setOnClickListener(v -> mWebView.loadUrl("http://www.google.com/"));
        mLoadBypassURLButton = findViewById(R.id.proxy_override_load_bypass_button);
        mLoadBypassURLButton.setOnClickListener(v -> mWebView.loadUrl("http://example.com/"));

        // Check for reverse bypass feature
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS)) {
            mReverseBypassCheckBox.setVisibility(View.VISIBLE);
        }

        // Initialize proxy server
        // Skip this step if you already have a proxy url
        mProxy = new Proxy(/*port=*/0, () -> runOnUiThread(() -> mRequestCountTextView.setText(
                getResources().getString(R.string.proxy_override_requests_served,
                        mProxy.getRequestCount()))));
        mProxy.start();
    }

    private void setProxyOverride() {
        // Construct a ProxyConfig object using your proxy URL and your bypass list
        ProxyConfig proxyConfig = new ProxyConfig.Builder()
                // Use your proxy URL here
                .addProxyRule("localhost:" + mProxy.getPort())
                // Add as many URLs to the bypass list as you need
                .addBypassRule("example.com")
                .addBypassRule("www.anotherbypassurl.com")
                // Set reverse bypass if the checkbox was checked. With reverse bypass, only
                // the URLs in the bypass list will use the proxy settings.
                .setReverseBypassEnabled(mReverseBypassCheckBox.isChecked())
                .build();

        // Call setProxyOverride and specify a callback
        ProxyController.getInstance()
                .setProxyOverride(proxyConfig, Runnable::run, this::onProxyOverrideComplete);
    }

    private void onProxyOverrideComplete() {
        // Your code goes here, after the proxy override callback was executed
        mLoadURLButton.setEnabled(true);
        mLoadBypassURLButton.setEnabled(true);
    }

    @Override
    protected void onDestroy() {
        if (mProxy != null) {
            mProxy.shutdown();
        }
        super.onDestroy();
    }
}
