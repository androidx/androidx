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
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setMargins
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * An {@link android.app.Activity} to demonstrate medium ("Quiet") interstitials. WebView displays a
 * grey error page with a small bit of description when it's "medium" sized (large enough to show
 * text, but small enough that it's likely not the predominant part of the UI), when loading
 * malicious resources.
 *
 * <p>
 * Medium interstitials are triggered when the WebView is either taller or wider than an otherwise
 * "small" WebView. This {@link android.app.Activity} can show either case ("tall" and "wide",
 * respectively), based on the boolean extra {@link #LAYOUT_HORIZONTAL}.
 */
class MediumInterstitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Decide whether to show the WebViews side-by-side ("tall") or stacked on top of each
        // other ("wide").
        val isHorizontal = intent.getBooleanExtra(LAYOUT_HORIZONTAL, true)

        setContentView(R.layout.activity_medium_interstitial)
        setTitle(
            if (isHorizontal) R.string.medium_tall_interstitial_activity_title
            else R.string.medium_wide_interstitial_activity_title
        )
        findViewById<LinearLayout>(R.id.activity_medium_interstitial).orientation =
            if (isHorizontal) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        setUpDemoAppActivity()

        val safeBrowsingPagesMap =
            mapOf(
                R.id.malware_webview to SafeBrowsingHelpers.MALWARE_URL,
                R.id.phishing_webview to SafeBrowsingHelpers.PHISHING_URL,
                R.id.unwanted_software_webview to SafeBrowsingHelpers.UNWANTED_SOFTWARE_URL,
                R.id.billing_webview to SafeBrowsingHelpers.BILLING_URL,
            )
        // Add more threat types (here and in the layout), if we support more in the future.

        val width =
            if (isHorizontal) STRETCH_THIS_DIMENSION else LinearLayout.LayoutParams.MATCH_PARENT
        val height =
            if (isHorizontal) LinearLayout.LayoutParams.MATCH_PARENT else STRETCH_THIS_DIMENSION
        val params = LinearLayout.LayoutParams(width, height, 1F).apply { setMargins(MARGIN_DP) }

        safeBrowsingPagesMap.forEach { (id, url) ->
            findViewById<WebView>(id).apply {
                layoutParams = params
                // A medium interstitial may have links on it in the future; allow this WebView to
                // handle opening those by setting a WebViewClient.
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
                    WebSettingsCompat.setSafeBrowsingEnabled(this.settings, true)
                }
                loadUrl(url)
            }
        }
    }

    companion object {
        const val LAYOUT_HORIZONTAL = "layoutHorizontal"
        private const val STRETCH_THIS_DIMENSION = 0
        private const val MARGIN_DP = 2
    }
}
