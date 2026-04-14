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
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.PrefetchException
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import androidx.webkit.WebViewOutcomeReceiver
import org.json.JSONObject

@OptIn(Profile.ExperimentalUrlPrefetch::class)
class PrefetchActivity : AppCompatActivity() {

    private lateinit var prefetchUrlInput: EditText
    private lateinit var maxPrefetches: EditText
    private lateinit var prefetchTtlSeconds: EditText
    private lateinit var prefetchButton: Button
    private lateinit var loadButton: Button
    private lateinit var statusText: TextView
    private lateinit var webView: WebView
    private lateinit var profile: Profile

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prefetch)
        setTitle(R.string.prefetch_activity_title)
        if (
            !areAllFeaturesSupported(
                WebViewFeature.PROFILE_URL_PREFETCH,
                WebViewFeature.PREFETCH_CACHE_V1,
                WebViewFeature.MULTI_PROFILE,
            )
        ) {
            showMessage(R.string.profile_url_prefetch_not_supported)
            finish()
            return
        }

        prefetchUrlInput = findViewById<EditText>(R.id.prefetch_url)
        maxPrefetches = findViewById<EditText>(R.id.max_prefetches)
        prefetchTtlSeconds = findViewById<EditText>(R.id.prefetch_ttl_seconds)
        prefetchButton = findViewById<Button>(R.id.prefetch_button)
        loadButton = findViewById<Button>(R.id.load_button)
        statusText = findViewById<TextView>(R.id.prefetch_status)
        webView = findViewById<WebView>(R.id.prefetch_webview)

        profile = ProfileStore.getInstance().getOrCreateProfile(Profile.DEFAULT_PROFILE_NAME)

        webView.apply {
            WebViewCompat.setProfile(this, profile.name)
            webViewClient = WebViewClient()
            settings.javaScriptEnabled = true
            injectPerformanceObserverScript(webView = this)
            setupWebMessageListener(webView = this)
            loadUrl(INITIAL_URL)
        }

        prefetchButton.apply {
            setOnClickListener(::handlePrefetchButtonClick)
            isEnabled = true
        }

        loadButton.setOnClickListener {
            val url = prefetchUrlInput.text.toString()
            statusText.text = "Status: Loading $url..."
            webView.loadUrl(url)
        }
    }

    private fun injectPerformanceObserverScript(webView: WebView) {
        if (!areAllFeaturesSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            Log.w(TAG, "DOCUMENT_START_SCRIPT feature not supported.")
            statusText.text =
                "${statusText.text}\nWarning: Cannot inject script to detect prefetch."
            return
        }

        WebViewCompat.addDocumentStartJavaScript(webView, OBSERVER_SCRIPT, ALLOWED_ORIGIN_RULES)
    }

    private fun setupWebMessageListener(webView: WebView) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            Log.w(TAG, "WEB_MESSAGE_LISTENER feature not supported.")
            statusText.text = "${statusText.text}\nWarning: Cannot receive messages from WebView."
            return
        }

        WebViewCompat.addWebMessageListener(
            webView,
            JS_OBJECT_NAME, // Matches the name used in JS
            ALLOWED_ORIGIN_RULES,
        ) { _, message, _, _, _ ->
            runCatching { message.data?.let(::onMessage) }
                .onFailure { e -> Log.e(TAG, "Error parsing message from JS: $message", e) }
        }
    }

    private fun handlePrefetchButtonClick(view: View) {
        val url = prefetchUrlInput.text.toString()
        statusText.text = "Status: Prefetching $url..."

        maxPrefetches.text.toString().toIntOrNull()?.let {
            profile.prefetchCache.setMaxPrefetches(it)
        }
        prefetchTtlSeconds.text.toString().toIntOrNull()?.let {
            profile.prefetchCache.setPrefetchTtlSeconds(it)
        }

        profile.prefetchUrlAsync(
            /* url = */ url,
            /* cancellationSignal = */ null,
            /* callbackExecutor = */ null,
            /* outcomeReceiver = */ object : WebViewOutcomeReceiver<Void?, PrefetchException> {
                override fun onResult(result: Void?) {
                    statusText.text = "Status: Success!"
                }

                override fun onError(error: PrefetchException) {
                    statusText.text = "Status: Error - ${error.message}"
                }
            },
        )
    }

    private fun onMessage(message: String) {
        val json = JSONObject(message)
        val type = json.getString("type")
        val url = json.getString("url")
        val sizeBytes = json.getLong("size")
        val deliveryType = json.optString("deliveryType")

        Log.d(TAG, "Resource: $url, Type: $type, Size: $sizeBytes, Delivery: $deliveryType")

        if (type == "prefetched") {
            Log.i(TAG, "PREFETCHED resource loaded: $url")
            // Update UI or internal state to reflect that a prefetched resource was used
            runOnUiThread {
                statusText.text = "Status: Loaded prefetched resource: ${url.takeLast(50)}"
            }
        }
        // Add other handling for cache_valid, network, etc. if needed
    }

    companion object {
        private val ALLOWED_ORIGIN_RULES = setOf("*") // Use more specific origins if possible
        private const val TAG = "PREFETCH_ACTIVITY"
        private const val INITIAL_URL = "https://www.example.com"
        private const val JS_OBJECT_NAME = "AndroidListener"
        private const val OBSERVER_SCRIPT =
            """
            const observer = new PerformanceObserver((list) => {
                for (const entry of list.getEntries()) {
                    let type;
                    const delivery = entry.deliveryType || ''; // Fallback for older browsers

                    // Check for prefetch
                    if (delivery === 'prefetch') {
                        type = 'prefetched';
                    } else if (entry.transferSize === 0 && delivery === 'cache') {
                        // Served from cache, no revalidation
                        type = 'cache_valid';
                    } else if (entry.transferSize > 0 && delivery === 'cache') {
                        // Served from cache, but required revalidation (e.g., 304 Not Modified)
                        type = 'cache_revalidated';
                    } else if (entry.decodedBodySize === 0) {
                        // Cross-origin restrictions or other issues prevent size detection
                        type = 'undetermined';
                    } else {
                        // Likely served from the network
                        type = 'network';
                    }

                    // Send data back to the Android app
                    if (typeof AndroidListener !== 'undefined') {
                        AndroidListener.postMessage(JSON.stringify({
                            type: type,
                            size: entry.decodedBodySize || 0,
                            url: entry.name,
                            transferSize: entry.transferSize,
                            deliveryType: delivery
                        }));
                    } else {
                        console.log("AndroidListener not found. Message not sent.");
                    }
                }
            });

            // Watch for 'resource' and 'navigation' entries.
            // buffered: true ensures we get entries that were loaded before this script ran.
            observer.observe({ entryTypes: ["resource", "navigation"], buffered: true });
            """
    }
}
