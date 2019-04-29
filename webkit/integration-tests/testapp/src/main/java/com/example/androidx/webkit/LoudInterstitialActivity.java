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
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate "Loud" interstitials. WebView displays a
 * red (or grey) error page with considerable description when it's "full" sized (takes up almost
 * the entire available device screen), and we believe it's likely the predominant part of the UI.
 * <p>
 * This {@link android.app.Activity} points to a safe page, but that page itself has links to
 * (fake) malicious resources of various threat types, to allow testing each threat type. Within the
 * interstitial, the user can click any of several links which provide more information about Safe
 * Browsing overall, including "proceed" and "back to safety" buttons for navigation.
 */
public class LoudInterstitialActivity extends AppCompatActivity {

    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loud_interstitial);
        setTitle(R.string.loud_interstitial_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        mWebView = findViewById(R.id.loud_webview);
        mWebView.getSettings().setJavaScriptEnabled(true); // in case site needs JS to render
        mWebView.setWebViewClient(new WebViewClient()); // allow mWebView to handle navigations
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(mWebView.getSettings(), true);
        }
        mWebView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
