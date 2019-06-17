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
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

/**
 * An {@link Activity} to exercise Proxy Override functionality.
 */
public class ProxyOverrideActivity extends AppCompatActivity {
    private Proxy mProxy;
    private WebView mWebView;
    private TextView mRequestCountTextView;
    private EditText mNavigationBar;
    private InputMethodManager mInputMethodManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_proxy_override);
        setTitle(R.string.proxy_override_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        // Initialize proxy server
        // Skip this step if you already have a proxy url
        mProxy = new Proxy(0, () -> runOnUiThread(() -> mRequestCountTextView.setText(
                getResources().getString(R.string.proxy_override_requests_served,
                        mProxy.getRequestCount()))));
        mProxy.start();

        // Initialize views
        mRequestCountTextView = findViewById(R.id.proxy_override_textview);
        mRequestCountTextView.setText(getResources().getString(
                R.string.proxy_override_requests_served, 0));
        mWebView = findViewById(R.id.proxy_override_webview);
        mWebView.setWebViewClient(new WebViewClient());
        mInputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        mNavigationBar = findViewById(R.id.proxy_override_edittext);
        mNavigationBar.setOnEditorActionListener((TextView v, int actionId, KeyEvent event) -> {
            // Listen to actions in the navigation bar (i.e. tapping enter on the soft keyboard)
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                // If action equals to IME_ACTION_NEXT, get the typed url, clear the navigation bar
                // and return true
                String url = mNavigationBar.getText().toString();
                if (!url.isEmpty()) {
                    // If the retrieved url is not empty, call WebView.loadUrl passing it
                    if (!url.startsWith("http")) url = "http://" + url;
                    mWebView.loadUrl(url);
                    mNavigationBar.setText("");
                }
                mInputMethodManager.hideSoftInputFromWindow(mNavigationBar.getWindowToken(), 0);
                mWebView.requestFocus();
                return true;
            }
            return false;
        });

        // Check for proxy override feature
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            // If feature is not supported, just show a warning in the webview
            mRequestCountTextView.setVisibility(View.GONE);
            mNavigationBar.setVisibility(View.GONE);
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        // Set proxy override
        // Use your proxy url here
        setProxyOverride("localhost:" + mProxy.getPort());
    }

    private void setProxyOverride(String proxyUrl) {
        ProxyController proxyController = ProxyController.getInstance();
        ProxyConfig proxyConfig = new ProxyConfig.Builder().addProxyRule(proxyUrl).build();
        proxyController.setProxyOverride(proxyConfig, (Runnable r) -> r.run(),
                () -> onProxyOverrideComplete());
    }

    private void onProxyOverrideComplete() {
        // Your code goes here, after the proxy override callback was executed
        mWebView.loadUrl("http://www.google.com");
    }

    @Override
    protected void onDestroy() {
        mProxy.shutdown();
        super.onDestroy();
    }
}
