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
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * An [android.app.Activity] demonstrating [Profile.configureIsolatedSites].
 *
 * It configures a dedicated [Profile] with `https://google.com` as an isolated site, and creates
 * two [WebView] instances assigned to that profile. Both initially load `https://www.google.com/`
 * and allow the user to navigate to other sites by following links.
 */
@OptIn(Profile.ExperimentalSelectedSiteIsolation::class)
class SelectedSiteIsolationActivity : AppCompatActivity() {

    private lateinit var firstWebView: WebView
    private lateinit var secondWebView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_selected_site_isolation)
        setTitle(R.string.selected_site_isolation_activity_title)
        setUpDemoAppActivity()

        if (
            !areAllFeaturesSupported(
                WebViewFeature.MULTI_PROFILE,
                WebViewFeature.PROFILE_SELECTED_SITE_ISOLATION_EXPERIMENTAL_V1,
            )
        ) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val profile = getOrConfigureProfile()

        val firstLabel = findViewById<TextView>(R.id.first_webview_label)
        val secondLabel = findViewById<TextView>(R.id.second_webview_label)
        val firstBackButton = findViewById<Button>(R.id.first_webview_back_button)
        val secondBackButton = findViewById<Button>(R.id.second_webview_back_button)
        firstWebView = findViewById(R.id.first_webview)
        secondWebView = findViewById(R.id.second_webview)

        initializeWebView(
            firstWebView,
            profile,
            firstLabel,
            firstBackButton,
        )
        initializeWebView(
            secondWebView,
            profile,
            secondLabel,
            secondBackButton,
        )
    }

    override fun onDestroy() {
        if (::firstWebView.isInitialized) {
            firstWebView.destroy()
        }
        if (::secondWebView.isInitialized) {
            secondWebView.destroy()
        }
        super.onDestroy()
    }

    private fun getOrConfigureProfile(): Profile {
        val profile = ProfileStore.getInstance().getOrCreateProfile(PROFILE_NAME)
        if (!isProfileConfigured) {
            profile.configureIsolatedSites(setOf(ISOLATED_SITE))
            isProfileConfigured = true
        }
        return profile
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebView(
        webView: WebView,
        profile: Profile,
        label: TextView,
        backButton: Button,
    ) {
        backButton.setOnClickListener { webView.goBack() }
        WebViewCompat.setProfile(webView, profile.name)
        updateRendererLabel(webView, label, "")
        webView.settings.javaScriptEnabled = true
        // Note: this isn't a good example for how to do URL bars! It's just enough for this demo.
        webView.webViewClient =
            object : WebViewClient() {
                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    updateRendererLabel(view, label, url ?: "")
                }

                override fun onPageCommitVisible(view: WebView, url: String?) {
                    updateRendererLabel(view, label, url ?: "")
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    updateRendererLabel(view, label, url ?: "")
                }
            }
        webView.loadUrl(INITIAL_URL)
    }

    private fun updateRendererLabel(
        webView: WebView,
        label: TextView,
        url: String,
    ) {
        val renderer =
            if (WebViewFeature.isFeatureSupported(WebViewFeature.GET_WEB_VIEW_RENDERER)) {
                WebViewCompat.getWebViewRenderProcess(webView)
            } else {
                null
            }
        val rendererText =
            if (renderer != null) {
                Integer.toHexString(System.identityHashCode(renderer))
            } else {
                getString(R.string.selected_site_isolation_renderer_unknown)
            }
        label.text =
            getString(
                R.string.selected_site_isolation_webview,
                rendererText,
                url,
            )
    }

    companion object {
        private const val PROFILE_NAME = "SelectedSiteIsolationDemo"
        private const val ISOLATED_SITE = "https://google.com"
        private const val INITIAL_URL = "https://www.google.com/"

        private var isProfileConfigured = false
    }
}
