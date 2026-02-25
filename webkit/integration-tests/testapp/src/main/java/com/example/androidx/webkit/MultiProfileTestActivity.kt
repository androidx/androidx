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
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * An {@link android.app.Activity} to demonstrate using Multi-Profile feature.
 *
 * <p>
 *
 * It creates two WebViews and assigns the default profile to one and a newly created profile to the
 * other one. There's a button above each WebView to print the cookie value as a confirmation that
 * each WebView get different cookie value.
 */
class MultiProfileTestActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_multi_profile)
        setTitle(R.string.multi_profile_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            showMessage(R.string.multi_profile_not_supported)
            return
        }

        initializeDefault()
        initializeCreatedProfile()
    }

    private fun initializeDefault() {
        val webView =
            findViewById<WebView>(R.id.default_webview).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(INITIAL_URL)
            }
        findViewById<TextView>(R.id.default_profile_cookie_text).setOnClickListener {
            logCookieForProfile(webView)
        }
    }

    private fun initializeCreatedProfile() {
        val profile = ProfileStore.getInstance().getOrCreateProfile("First")
        val webView =
            findViewById<WebView>(R.id.first_profile).apply {
                WebViewCompat.setProfile(this, profile.name)
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                loadUrl(INITIAL_URL)
            }
        findViewById<TextView>(R.id.created_profile_cookie_text).setOnClickListener {
            logCookieForProfile(webView)
        }
    }

    /**
     * Show the cookies of the loaded page to the user to let them confirm that the two WebViews get
     * different cookie values.
     */
    private fun logCookieForProfile(requestedWebView: WebView) {
        val cookieInfo =
            WebViewCompat.getProfile(requestedWebView).cookieManager.getCookie(requestedWebView.url)
        Toast.makeText(this, cookieInfo, Toast.LENGTH_SHORT).show()
        Log.i(this.localClassName, cookieInfo)
    }

    companion object {
        private const val INITIAL_URL = "https://www.google.com"
    }
}
