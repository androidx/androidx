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

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.GoogleAccountsEvent
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** Demonstration of how to use the [WebViewCompat.setGoogleAccountsCallback] API. */
@OptIn(WebViewCompat.ExperimentalGoogleAccountsCallback::class)
class GoogleAccountsCallbackActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_accounts_callback)
        setTitle(R.string.google_accounts_callback_activity_title)
        setUpDemoAppActivity()

        if (
            !WebViewFeature.isFeatureSupported(
                WebViewFeature.GOOGLE_ACCOUNTS_CALLBACK_EXPERIMENTAL_V1
            )
        ) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val callbackTextView = findViewById<TextView>(R.id.google_accounts_callback_textview)
        val webView = findViewById<WebView>(R.id.google_accounts_callback_webview)

        webView.apply {
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
        }

        WebViewCompat.setGoogleAccountsCallback(webView, null) { event ->
            val actionTypeString =
                when (event.actionType) {
                    GoogleAccountsEvent.ACTION_TYPE_SIGN_OUT -> "SIGN_OUT"
                    GoogleAccountsEvent.ACTION_TYPE_CREATE_ACCOUNT -> "CREATE_ACCOUNT"
                    GoogleAccountsEvent.ACTION_TYPE_OTHER -> "OTHER"
                    else -> "UNKNOWN (${event.actionType})"
                }
            callbackTextView.text =
                getString(
                    R.string.google_accounts_callback_event_text,
                    actionTypeString,
                    event.continueUrl,
                    event.isOutermostMainFrame,
                    event.isNavigationRequest,
                )
        }

        webView.loadUrl("https://google.com")
    }
}
