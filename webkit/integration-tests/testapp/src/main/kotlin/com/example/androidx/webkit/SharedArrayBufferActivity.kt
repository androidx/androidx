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
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

class SharedArrayBufferActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_shared_array_buffer)
        setTitle(ACTIVITY_TITLE)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.CROSS_ORIGIN_ISOLATED_ALLOWLIST)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }
        val assetLoader =
            WebViewAssetLoader.Builder()
                .setDomain(BASE_DOMAIN)
                .addPathHandler(ASSETS_PATH, WebViewAssetLoader.AssetsPathHandler(this))
                .build()

        findViewById<WebView>(R.id.webview).apply {
            webViewClient = AssetLoaderWebViewClient(assetLoader)
            settings.javaScriptEnabled = true
            WebViewCompat.getProfile(this).crossOriginIsolatedAllowlist = setOf(BASE_ORIGIN)
            loadUrl(PAGE_URL)
        }
    }

    private class AssetLoaderWebViewClient(private val assetLoader: WebViewAssetLoader) :
        WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? =
            assetLoader.shouldInterceptRequest(request.url)?.apply {
                responseHeaders = (responseHeaders ?: emptyMap()) + (HEADER_KEY to HEADER_VALUE)
            }
    }

    companion object {
        const val ACTIVITY_TITLE = "SharedArrayBuffer"
        const val BASE_DOMAIN = "example.com"
        const val BASE_ORIGIN = "https://$BASE_DOMAIN"
        const val ASSETS_PATH = "/androidx_webkit/example/assets/"
        const val PAGE_URL = BASE_ORIGIN + ASSETS_PATH + "www/shared_array_buffer.html"
        const val HEADER_KEY = "Document-Isolation-Policy"
        const val HEADER_VALUE = "isolate-and-credentialless"
    }
}
