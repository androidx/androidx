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
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.NavigationParameters
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** Demonstration of how to use the [WebViewCompat.navigate] API. */
@OptIn(WebViewCompat.ExperimentalNavigate::class)
class NavigateActivity : AppCompatActivity() {
    private lateinit var urlInput: EditText
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigate)
        setUpDemoAppActivity()
        setTitle(R.string.navigate_activity_title)
        if (!areAllFeaturesSupported(WebViewFeature.WEBVIEW_NAVIGATE_EXPERIMENTAL_V1)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        urlInput = findViewById<EditText>(R.id.navigate_url)
        webView = findViewById<WebView>(R.id.navigate_webview)

        webView.apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
        }

        findViewById<Button>(R.id.navigate_load_button).apply {
            setOnClickListener(::handleNavigate)
            isEnabled = true
        }
    }

    private fun handleNavigate(view: View) {
        val url = urlInput.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, R.string.navigate_url_input_empty_message, Toast.LENGTH_SHORT)
                .show()
            return
        }

        val params =
            NavigationParameters.Builder()
                .setShouldReplaceCurrentEntry(true)
                .addAdditionalHeaders(mapOf("X-Test-Navigate-Header" to "TestValue"))
                .build()

        WebViewCompat.navigate(webView, url, params)
    }
}
