/*
 * Copyright 2023 The Android Open Source Project
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
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.Profile;
import androidx.webkit.ProfileStore;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate using Multi-Profile feature.
 *
 * <p>
 *
 * It creates two WebViews and assigns the default profile to one and a newly created profile to
 * the other one. There's a button above each WebView to print the cookie value as a confirmation
 * that each WebView get different cookie value.
 *
 */
public class MultiProfileTestActivity extends AppCompatActivity {

    private static final String INITIAL_URL = "https://www.google.com";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_profile);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            WebkitHelpers.showMessageInActivity(MultiProfileTestActivity.this,
                    R.string.multi_profile_not_supported);
            return;
        }

        initializeDefault();
        initializeCreatedProfile();
    }

    private void initializeDefault() {
        WebView defaultWebView = findViewById(R.id.default_webview);
        defaultWebView.setWebViewClient(new WebViewClient());
        defaultWebView.getSettings().setJavaScriptEnabled(true);
        defaultWebView.loadUrl(INITIAL_URL);
        findViewById(R.id.default_profile_cookie_text).setOnClickListener(
                v -> logCookieForProfile(defaultWebView));
    }

    private void initializeCreatedProfile() {
        WebView createdProfileWebView = findViewById(R.id.first_profile);
        ProfileStore profileStore = ProfileStore.getInstance();
        Profile createdProfile = profileStore.getOrCreateProfile("First");
        WebViewCompat.setProfile(createdProfileWebView, createdProfile.getName());
        createdProfileWebView.setWebViewClient(new WebViewClient());
        createdProfileWebView.getSettings().setJavaScriptEnabled(true);
        createdProfileWebView.loadUrl(INITIAL_URL);
        findViewById(R.id.created_profile_cookie_text).setOnClickListener(
                v -> logCookieForProfile(createdProfileWebView));
    }

    /**
     * Show the cookies of the loaded page to the user to let them confirm that the two WebViews
     * get different cookie values.
     */
    private void logCookieForProfile(WebView requestedWebView) {
        String cookieInfo = WebViewCompat.getProfile(requestedWebView).getCookieManager().getCookie(
                requestedWebView.getUrl());
        Toast.makeText(MultiProfileTestActivity.this, cookieInfo,
                Toast.LENGTH_SHORT).show();
        Log.i(MultiProfileTestActivity.this.getLocalClassName(), cookieInfo);
    }
}
