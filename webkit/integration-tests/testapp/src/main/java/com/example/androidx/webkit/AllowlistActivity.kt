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

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * An [Activity] to demonstrate how to allowlist a set of domains from Safe Browsing checks. This
 * includes buttons to toggle whether the allowlist is on or off.
 */
class AllowlistActivity : AppCompatActivity() {

    private lateinit var allowListWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_allowlist)
        setTitle(R.string.allowlist_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ALLOWLIST)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        findViewById<SwitchCompat>(R.id.allowlist_switch).apply {
            isChecked = true
            setOnCheckedChangeListener { _, _ ->
                if (isChecked) {
                    allowlistSafeBrowsingTestSite()
                } else {
                    clearAllowlist()
                }
            }
        }

        allowListWebView =
            findViewById<WebView>(R.id.allowlist_webview).apply {
                // Allow allowListWebView to handle navigations.
                webViewClient = WebViewClient()
            }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(allowListWebView.settings, true)
        }

        // Set the allowlist and load the test site.
        allowlistSafeBrowsingTestSite {
            allowListWebView.loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE)
        }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (allowListWebView.canGoBack()) {
                        allowListWebView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        clearAllowlist()
    }

    // To clear the allowlist (and check all domains with Safe Browsing), pass an empty list.
    private fun clearAllowlist() =
        WebViewCompat.setSafeBrowsingAllowlist(emptySet()) { success ->
            if (!success) {
                showMessage(R.string.invalid_allowlist_input_message)
            }
            // Nothing interesting to do if this succeeds, let user continue to use the app.
        }

    private fun allowlistSafeBrowsingTestSite(onSuccess: Runnable? = null) {
        // Configure an allowlist of domains. Pages/resources loaded from these domains will never
        // be checked by Safe Browsing (until a new allowlist is applied).
        val allowList = setOf(SafeBrowsingHelpers.TEST_SAFE_BROWSING_DOMAIN)
        WebViewCompat.setSafeBrowsingAllowlist(allowList) { success ->
            if (success) {
                onSuccess?.run()
            } else {
                showMessage(R.string.invalid_allowlist_input_message)
            }
        }
    }
}
