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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.CompoundButton
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/** An Activity that launches a payment app in WebView through PaymentRequest API. */
class PaymentRequestActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var paymentRequestToggle: CompoundButton
    private lateinit var hasEnrolledInstrumentToggle: CompoundButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_payment_request)
        setTitle(R.string.payment_request_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PAYMENT_REQUEST)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        WebView.setWebContentsDebuggingEnabled(true)
        webView =
            findViewById<WebView>(R.id.webview_supports_payment_request).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    @Suppress("DEPRECATION")
                    databaseEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true

                    // Default layout behavior for chrome on android:
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
                }
            }

        if (savedInstanceState == null) {
            webView.loadUrl(EXAMPLE_SITE_WITH_PAYMENT_REQUEST_API)
        } else {
            webView.restoreState(savedInstanceState)
        }

        paymentRequestToggle =
            findViewById<CompoundButton>(R.id.payment_request_toggle).apply {
                isChecked = WebSettingsCompat.getPaymentRequestEnabled(webView.settings)
                setOnCheckedChangeListener(this@PaymentRequestActivity::onToggleChanged)
            }

        hasEnrolledInstrumentToggle =
            findViewById<CompoundButton>(R.id.has_enrolled_instrument_toggle).apply {
                isChecked = WebSettingsCompat.getHasEnrolledInstrumentEnabled(webView.settings)
                setOnCheckedChangeListener(this@PaymentRequestActivity::onToggleChanged)
            }

        onBackPressedDispatcher.addCallback(
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (!webView.canGoBack()) {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    } else {
                        webView.goBack()
                    }
                }
            }
        )
    }

    private fun onToggleChanged(button: CompoundButton, isChecked: Boolean) {
        require(WebViewFeature.isFeatureSupported(WebViewFeature.PAYMENT_REQUEST)) {
            "WebView Feature PAYMENT_REQUEST is not supported"
        }
        WebSettingsCompat.setPaymentRequestEnabled(webView.settings, paymentRequestToggle.isChecked)
        WebSettingsCompat.setHasEnrolledInstrumentEnabled(
            webView.settings,
            hasEnrolledInstrumentToggle.isChecked,
        )
        webView.reload()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    companion object {
        private const val EXAMPLE_SITE_WITH_PAYMENT_REQUEST_API =
            "https://rsolomakhin.github.io/pr/bob/"
    }
}
