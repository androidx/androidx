/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.androidx.webkit

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * An {@link android.app.Activity} to demonstrate small ("Quiet") interstitials.
 *
 * <p>
 * For Safe Browsing, WebView displays a grey error page with very little text when it is
 * sufficiently small (and loads a malicious resource).
 *
 * <p>
 * For Restricted Content blocking, WebView displays a grey error page with a blocked sign on it
 * (when a restricted resource is loaded). No text or "learn more" link is shown.
 */
class SmallInterstitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_small_interstitial)
        setTitle(R.string.small_interstitial_activity_title)
        setUpDemoAppActivity()

        val webView = findViewById<WebView>(R.id.small_webview)
        val contentType = intent.getIntExtra(INTENT_EXTRA_CONTENT_TYPE, ContentType.SAFE_CONTENT)

        when (contentType) {
            ContentType.MALICIOUS_CONTENT -> {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                    WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, true)
                }
                webView.loadUrl(SafeBrowsingHelpers.MALWARE_URL)
            }
            ContentType.RESTRICTED_CONTENT -> webView.loadUrl(RESTRICTED_CONTENT_URL)
            else -> webView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE)
        }
    }
}
