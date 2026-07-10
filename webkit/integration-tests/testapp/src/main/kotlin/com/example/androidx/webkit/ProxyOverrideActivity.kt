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
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewFeature

/** An {@link Activity} to exercise Proxy Override functionality. */
class ProxyOverrideActivity : AppCompatActivity() {

    private var proxy: HttpServer? = null
    private lateinit var reverseBypassCheckBox: CheckBox
    private lateinit var loadURLButton: Button
    private lateinit var loadBypassURLButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_proxy_override)
        setTitle(R.string.proxy_override_activity_title)
        setUpDemoAppActivity()

        // Check for proxy override feature
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val requestCountTextView =
            findViewById<TextView>(R.id.proxy_override_textview).apply {
                text = resources.getString(R.string.proxy_override_requests_served, 0)
            }
        val webView =
            findViewById<WebView>(R.id.proxy_override_webview).apply {
                webViewClient = WebViewClientCompat()
            }
        reverseBypassCheckBox = findViewById(R.id.proxy_override_reverse_bypass_checkbox)
        findViewById<Button>(R.id.proxy_override_button).apply {
            setOnClickListener {
                reverseBypassCheckBox.isEnabled = false
                this.isEnabled = false
                setProxyOverride()
            }
        }
        loadURLButton =
            findViewById<Button>(R.id.proxy_override_load_url_button).apply {
                setOnClickListener { webView.loadUrl(PROXY_OVERRIDE_URL) }
            }
        loadBypassURLButton =
            findViewById<Button>(R.id.proxy_override_load_bypass_button).apply {
                setOnClickListener { webView.loadUrl(PROXY_BYPASS_URL) }
            }

        // Check for reverse bypass feature
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE_REVERSE_BYPASS)) {
            reverseBypassCheckBox.visibility = View.VISIBLE
        }

        // Initialize proxy server
        // Skip this step if you already have a proxy url
        proxy =
            HttpServer(port = 0, { HttpServer.ProxyRequestHandler(it, this) }) {
                // on request callback, called when url loaded
                runOnUiThread {
                    proxy?.let {
                        requestCountTextView.text =
                            resources.getString(
                                R.string.proxy_override_requests_served,
                                it.getRequestCount(),
                            )
                    }
                }
            }
        proxy?.start()
    }

    private fun setProxyOverride() {
        if (proxy == null) {
            return
        }
        // Call setProxyOverride and specify a callback
        ProxyController.getInstance()
            .setProxyOverride(
                ProxyConfig.Builder()
                    // Use your proxy URL here
                    .addProxyRule("localhost:${proxy!!.getPort()}")
                    // Add as many URLs to the bypass list as you need
                    .addBypassRule(PROXY_BYPASS_URL)
                    .addBypassRule(ANOTHER_PROXY_BYPASS_URL)
                    // Set reverse bypass if the checkbox was checked. With reverse bypass, only
                    // the URLs in the bypass list will use the proxy settings.
                    .setReverseBypassEnabled(reverseBypassCheckBox.isChecked)
                    .build(),
                Runnable::run,
                this::onProxyOverrideComplete,
            )
    }

    private fun onProxyOverrideComplete() {
        // Your code goes here, after the proxy override callback was executed
        loadURLButton.isEnabled = true
        loadBypassURLButton.isEnabled = true
    }

    override fun onDestroy() {
        proxy?.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val PROXY_OVERRIDE_URL = "http://www.google.com/"
        private const val PROXY_BYPASS_URL = "www.example.com"
        private const val ANOTHER_PROXY_BYPASS_URL = "www.anotherbypassurl.com"
    }
}
