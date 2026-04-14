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
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.SafeBrowsingResponseCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature

/**
 * An {@link Activity} which shows a custom interstitial if {@link WebView} encounters malicious
 * resources. This class contains the logic for responding to user interaction with custom
 * interstitials. The UI for these interstitials is implemented by {@link
 * PopupInterstitialActivity}.
 */
class CustomInterstitialActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var customWebViewClient: CustomInterstitialWebViewClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_custom_interstitial)
        setTitle(R.string.custom_interstitial_activity_title)
        setUpDemoAppActivity()
        if (
            !(WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_HIT) &&
                WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_RESPONSE_PROCEED) &&
                WebViewFeature.isFeatureSupported(
                    WebViewFeature.SAFE_BROWSING_RESPONSE_BACK_TO_SAFETY
                ))
        ) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        customWebViewClient = CustomInterstitialWebViewClient(this)
        webView =
            findViewById<WebView>(R.id.custom_interstitial_webview).apply {
                webViewClient = customWebViewClient
                loadUrl(SafeBrowsingHelpers.TEST_SAFE_BROWSING_SITE)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        customWebViewClient.handleInterstitialResponse(requestCode, resultCode, data)
    }

    private inner class CustomInterstitialWebViewClient(private val activity: Activity) :
        WebViewClientCompat() {

        private val safeBrowsingResponseMap = SparseArray<SafeBrowsingResponseCompat>()
        private var activityRequestCounter = 0

        override fun onSafeBrowsingHit(
            view: WebView,
            request: WebResourceRequest,
            threatType: Int,
            callback: SafeBrowsingResponseCompat,
        ) {
            safeBrowsingResponseMap[activityRequestCounter] = callback
            createInterstitial(threatType, request)
            activityRequestCounter++
        }

        private fun createInterstitial(threatType: Int, request: WebResourceRequest) {
            val intent =
                Intent(activity, PopupInterstitialActivity::class.java).apply {
                    putExtra(PopupInterstitialActivity.THREAT_TYPE, threatType)
                    putExtra(PopupInterstitialActivity.THREAT_URL, request.url.toString())
                }
            activity.startActivityForResult(intent, activityRequestCounter)
        }

        fun handleInterstitialResponse(requestCode: Int, resultCode: Int, data: Intent?) {
            if (data == null) {
                throw IllegalStateException("data is not expected to be null")
            }
            // Get the correct SafeBrowsingResponse for the given interstitial Intent (there can be
            // multiple Intents at the same time if multiple resources are malicious).
            val response = safeBrowsingResponseMap.get(requestCode)
            safeBrowsingResponseMap.delete(requestCode)

            // Make sure the request was successful
            when (resultCode) {
                RESULT_OK -> onResultCodeOk(response, data)
                RESULT_CANCELED -> {
                    // User pressed the system's back button, treat this like backToSafety().
                    response.backToSafety(false)
                }
                else -> {
                    throw IllegalStateException(
                        "PopupInterstitialActivity shouldn't return any nonstandard resultCodes"
                    )
                }
            }
        }

        private fun onResultCodeOk(response: SafeBrowsingResponseCompat, data: Intent) {
            // Figure out what navigation action we should take.
            val result = data.getStringExtra(PopupInterstitialActivity.ACTION_RESPONSE)
            // User pressed the system's back button, treat this like backToSafety().
            val shouldReport =
                data.getBooleanExtra(PopupInterstitialActivity.SHOULD_SEND_REPORT, false)

            when (result) {
                PopupInterstitialActivity.ACTION_RESPONSE_BACK_TO_SAFETY ->
                    response.backToSafety(shouldReport)

                PopupInterstitialActivity.ACTION_RESPONSE_PROCEED -> response.proceed(shouldReport)
            }
        }
    }
}
