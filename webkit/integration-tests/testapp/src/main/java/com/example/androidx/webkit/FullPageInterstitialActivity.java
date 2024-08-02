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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate full page interstitials. WebView displays a
 * red or grey error page with considerable description when it's "full" sized (takes up almost
 * the entire available device screen), and we believe it's likely the predominant part of the UI.
 * <p>
 * This {@link android.app.Activity} points to a safe page, but that page itself has links to
 * (fake) malicious resources of various threat types and (fake) restricted resources, to allow
 * testing each threat type.
 * <p>
 * For Safe Browsing, within the interstitial, the user can click any of several links which
 * provide more information about Safe Browsing overall, including "proceed" and "back to safety"
 * buttons for navigation.
 * <p>
 * For Restricted Content blocking, user can click on "learn more", but there is no option to
 * proceed.
 */
public class FullPageInterstitialActivity extends AppCompatActivity {
    public static final String CONTENT_TYPE = "contentType";
    private WebView mWebView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_page_interstitial);
        setTitle(R.string.full_page_interstitial_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        mWebView = findViewById(R.id.full_page_webview);
        mWebView.getSettings().setJavaScriptEnabled(true); // in case site needs JS to render
        mWebView.setWebViewClient(new WebViewClient()); // allow mWebView to handle navigations
        Intent intent = getIntent();
        @ContentType int contentType = intent.getIntExtra(CONTENT_TYPE, ContentType.SAFE_CONTENT);

        switch (contentType) {
            case ContentType.MALICIOUS_CONTENT:
                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                    WebSettingsCompat.setSafeBrowsingEnabled(mWebView.getSettings(), true);
                }
                mWebView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE);
                break;
            case ContentType.RESTRICTED_CONTENT:
                mWebView.loadUrl(RestrictedContentHelpers.RESTRICTED_CONTENT_SITE);
                break;
            case ContentType.SAFE_CONTENT:
            default:
                mWebView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
