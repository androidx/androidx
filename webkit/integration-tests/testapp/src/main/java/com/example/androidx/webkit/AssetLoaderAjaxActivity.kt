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

import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.test.espresso.idling.net.UriIdlingResource
import androidx.webkit.WebViewAssetLoader

/**
 * An {@link Activity} to show a more useful use case: performing ajax calls to load files from
 * local app assets and resources in a safer way using WebViewAssetLoader.
 */
class AssetLoaderAjaxActivity : AppCompatActivity() {

    val MAX_IDLE_TIME_MS = 5000L

    private class MyWebViewClient(
        private val assetLoader: WebViewAssetLoader,
        val uriIdlingResource: UriIdlingResource,
    ) : WebViewClient() {

        @Deprecated(
            "Intentional use of deprecation"
        ) // use the old one for compatibility with all API levels
        override fun shouldOverrideUrlLoading(view: WebView, url: String) = false

        @Deprecated(
            "Intentional use of deprecation"
        ) // use the old one for compatibility with all API levels
        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
            uriIdlingResource.beginLoad(url)
            val response = assetLoader.shouldInterceptRequest(Uri.parse(url))
            uriIdlingResource.endLoad(url)
            return response
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            val url = request.url
            uriIdlingResource.beginLoad(url.toString())
            val response = assetLoader.shouldInterceptRequest(url)
            uriIdlingResource.endLoad(url.toString())
            return response
        }
    }

    // IdlingResource that indicates that WebView has finished loading all WebResourceRequests
    // by waiting until there are no requests made for 5000ms.
    val uriIdlingResource =
        UriIdlingResource("AssetLoaderWebViewUriIdlingResource", MAX_IDLE_TIME_MS)
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_asset_loader)
        setTitle(R.string.asset_loader_ajax_activity_title)
        setUpDemoAppActivity()

        // The "https://example.com" domain with the virtual path "/androidx_webkit/example/
        // is used to host resources/assets is used for demonstration purpose only.
        // The developer should ALWAYS use a domain which they are in control of or use
        // the default androidplatform.net reserved by Google for this purpose.
        // use "example.com" instead of the default domain
        // Host app resources ... under https://example.com/androidx_webkit/example/res/...
        // Host app assets under https://example.com/androidx_webkit/example/assets/...
        val assetLoader =
            WebViewAssetLoader.Builder()
                .setDomain("example.com") // use "example.com" instead of the default domain
                // Host app resources ... under https://example.com/androidx_webkit/example/res/...
                .addPathHandler(
                    "/androidx_webkit/example/res/",
                    WebViewAssetLoader.ResourcesPathHandler(this),
                )
                // Host app assets under https://example.com/androidx_webkit/example/assets/...
                .addPathHandler(
                    "/androidx_webkit/example/assets/",
                    WebViewAssetLoader.AssetsPathHandler(this),
                )
                .build()

        webView = findViewById(R.id.webview_asset_loader_webview)
        webView.webViewClient = MyWebViewClient(assetLoader, uriIdlingResource)

        with(webView.settings) {
            javaScriptEnabled = true
            setDeprecatedAllowFileAccess(this)
            // Keeping these off is less critical but still a good idea, especially
            // if your app is not using file:// or content:// URLs.
            allowFileAccess = true
            allowContentAccess = true
        }

        val loadButton: Button = findViewById(R.id.button_load_ajax_html)
        loadButton.setOnClickListener { loadUrl() }
    }

    /** @noinspection RedundantSuppression */
    @Suppress("DEPRECATION") /* b/180503860 */
    private fun setDeprecatedAllowFileAccess(webViewSettings: WebSettings) {
        // Setting this off for security. Off by default for SDK versions >= 16.
        webViewSettings.allowFileAccessFromFileURLs = false
        webViewSettings.allowUniversalAccessFromFileURLs = false
    }

    /** Load the url https://example.com/androidx_webkit/example/assets/www/ajax_requests.html. */
    fun loadUrl() {
        webView.loadUrl("https://example.com/androidx_webkit/example/assets/www/ajax_requests.html")
    }
}
