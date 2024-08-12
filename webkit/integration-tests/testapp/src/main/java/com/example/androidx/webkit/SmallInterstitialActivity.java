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

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

/**
 * An {@link android.app.Activity} to demonstrate small ("Quiet") interstitials.
 * <p>
 * For Safe Browsing, WebView displays a grey error page with very little text when it is
 * sufficiently small (and loads a malicious resource).
 * <p>
 * For Restricted Content blocking, WebView displays a grey error page with a blocked sign
 * on it (when a restricted resource is loaded). No text or "learn more" link is shown.
 */
public class SmallInterstitialActivity extends AppCompatActivity {
    public static final String CONTENT_TYPE = "contentType";
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_small_interstitial);
        setTitle(R.string.small_interstitial_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        WebView webview = findViewById(R.id.small_webview);

        Intent intent = getIntent();
        @ContentType int contentType = intent.getIntExtra(CONTENT_TYPE, ContentType.SAFE_CONTENT);

        switch (contentType) {
            case ContentType.MALICIOUS_CONTENT:
                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                    WebSettingsCompat.setSafeBrowsingEnabled(webview.getSettings(), true);
                }
                webview.loadUrl(SafeBrowsingHelpers.MALWARE_URL);
                break;
            case ContentType.RESTRICTED_CONTENT:
                webview.loadUrl(RestrictedContentHelpers.RESTRICTED_CONTENT_URL);
                break;
            case ContentType.SAFE_CONTENT:
            default:
                webview.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE);
        }
    }
}
