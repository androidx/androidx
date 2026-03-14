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
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

class FullPageInterstitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_full_page_interstitial)
        setTitle(R.string.full_page_interstitial_activity_title)
        setUpDemoAppActivity()

        val webView =
            findViewById<WebView>(R.id.full_page_webview).apply {
                settings.javaScriptEnabled = true
                webViewClient = WebViewClient()
            }

        val contentType = intent.getIntExtra(INTENT_EXTRA_CONTENT_TYPE, ContentType.SAFE_CONTENT)

        when (contentType) {
            ContentType.MALICIOUS_CONTENT -> {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                    WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, true)
                }
                webView.loadUrl(SafeBrowsingHelpers.MALWARE_URL)
            }
            ContentType.RESTRICTED_CONTENT -> webView.loadUrl(RESTRICTED_CONTENT_SITE)
            else -> webView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE)
        }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }
}
