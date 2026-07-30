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
import android.view.View
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewFeature

@OptIn(Profile.ExperimentalPreconnect::class)
class PreconnectActivity : AppCompatActivity() {

    private lateinit var preconnectUrlInput: EditText
    private lateinit var enqueuePreconnectButton: Button
    private lateinit var preconnectButton: Button
    private lateinit var startupWebViewButton: Button
    private lateinit var loadButton: Button
    private lateinit var statusText: TextView
    private lateinit var webView: WebView
    private lateinit var webViewContainer: FrameLayout
    private lateinit var profile: Profile

    private var featuresEnabled = true
    private var enqueuePreconnectEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preconnect)
        setTitle(R.string.preconnect_activity_title)
        if (!areAllFeaturesSupported(WebViewFeature.PRECONNECT, WebViewFeature.MULTI_PROFILE)) {
            showMessage(R.string.preconnect_not_supported)
            Log.e(TAG, "Preconnect not supported")
            featuresEnabled = false
        }

        enqueuePreconnectEnabled = areAllFeaturesSupported(WebViewFeature.ENQUEUE_PRECONNECT)

        preconnectUrlInput = findViewById(R.id.preconnect_url)
        enqueuePreconnectButton = findViewById(R.id.enqueue_preconnect_button)
        preconnectButton = findViewById(R.id.preconnect_button)
        startupWebViewButton = findViewById(R.id.startup_webview_button)
        loadButton = findViewById(R.id.load_button)
        statusText = findViewById(R.id.preconnect_status)
        webViewContainer = findViewById(R.id.webview_container)

        if (featuresEnabled) setupView()
    }

    /**
     * [ProfileStore.getInstance] and [ProfileStore.getOrCreateProfile] does not start up WebView.
     */
    private fun setupView() {
        profile = ProfileStore.getInstance().getOrCreateProfile(Profile.DEFAULT_PROFILE_NAME)

        enqueuePreconnectButton.apply {
            setOnClickListener(::handleEnqueuePreconnect)
            isEnabled = enqueuePreconnectEnabled
        }
        preconnectButton.apply {
            setOnClickListener(::handlePreconnect)
            isEnabled = true
        }
        startupWebViewButton.apply {
            setOnClickListener(::handleStartupWebView)
            isEnabled = true
        }
        loadButton.apply {
            setOnClickListener(::handleLoad)
            isEnabled = true
        }
    }

    private fun handleEnqueuePreconnect(view: View) {
        require(enqueuePreconnectEnabled) { "EnqueuePreconnect is not supported" }

        val url = preconnectUrlInput.text.toString()
        statusText.text = getString(R.string.preconnect_status_enqueue)
        profile.enqueuePreconnect(url)
    }

    private fun handlePreconnect(view: View) {
        val url = preconnectUrlInput.text.toString()
        statusText.text = getString(R.string.preconnect_status_preconnect)
        profile.preconnect(url)
    }

    /**
     * Currently the [ProfileStore.getProfile] starts up WebView , but
     * [ProfileStore.getOrCreateProfile] does not, when ENQUEUE_PRECONNECT feature flag is enabled.
     */
    private fun handleStartupWebView(view: View) {
        ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME)
    }

    private fun handleLoad(view: View) {
        val url = preconnectUrlInput.text.toString()
        statusText.text = getString(R.string.preconnect_status_loading, url)
        // Instantiate WebView lazily to avoid triggering Chromium initialization
        // before preconnect or startupWebView can be tested.
        if (!::webView.isInitialized) {
            webView = WebView(this).apply { webViewContainer.addView(this) }
        }
        webView.loadUrl(url)
    }

    companion object {
        private const val TAG = "PRECONNECT_ACTIVITY"
    }
}
