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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** An {@link Activity} to show how WebMessageListener deals with malicious websites. */
class WebMessageListenerMaliciousWebsiteActivity : AppCompatActivity() {

    private class AssetLoaderWebViewClient(val assetLoaders: List<WebViewAssetLoader>) :
        WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            return assetLoaders.firstNotNullOfOrNull { it.shouldInterceptRequest(request.url) }
        }

        @Deprecated(
            "Intentional use of deprecated function"
        ) // use the old one for compatibility with all API levels
        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
            return assetLoaders.firstNotNullOfOrNull { it.shouldInterceptRequest(Uri.parse(url)) }
        }
    }

    private inner class AvailableInAllFramesMessageListener(val badAuthorities: List<String>) :
        WebViewCompat.WebMessageListener {

        override fun onPostMessage(
            view: WebView,
            message: WebMessageCompat,
            sourceOrigin: Uri,
            isMainFrame: Boolean,
            replyProxy: JavaScriptReplyProxy,
        ) {
            badAuthorities.forEach {
                if (sourceOrigin.authority.equals(it)) {
                    Toast.makeText(
                            this@WebMessageListenerMaliciousWebsiteActivity,
                            "Message from known bad website, no response.",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                    return
                }
            }
            replyProxy.postMessage("Reply from app for ${message.data}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_web_message_listener_malicious_website)
        setTitle(R.string.web_message_listener_malicious_website_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            showMessage(R.string.web_message_listener_malicious_website_activity_title)
            return
        }

        // Use WebViewAssetLoader to load html page from app's assets.
        val assetLoaderMalicious =
            WebViewAssetLoader.Builder()
                .setDomain(MALICIOUS_URL)
                .addPathHandler(ASSET_PATH, WebViewAssetLoader.AssetsPathHandler(this))
                .build()

        val assetLoaderGenuine =
            WebViewAssetLoader.Builder()
                .setDomain(EXAMPLE_URL)
                .addPathHandler(ASSET_PATH, WebViewAssetLoader.AssetsPathHandler(this))
                .build()

        val webView =
            findViewById<WebView>(R.id.webview).apply {
                webViewClient =
                    AssetLoaderWebViewClient(listOf(assetLoaderGenuine, assetLoaderMalicious))
                settings.javaScriptEnabled = true
            }

        // If you only intend to communicate with a limited number of origins, prefer only injecting
        // the listener in those frames.
        WebViewCompat.addWebMessageListener(
            webView,
            "restrictedObject",
            setOf("https://$EXAMPLE_URL"),
        ) { _: WebView, _: WebMessageCompat, _: Uri, _: Boolean, replyProxy: JavaScriptReplyProxy ->
            replyProxy.postMessage("Hello")
        }

        // If you need to communicate with a wider set of origins but are aware of some origins
        // matching your filter that you need to block communication with, you can check the sending
        // frame's origin on the Java side in onPostMessage().
        WebViewCompat.addWebMessageListener(
            webView,
            "allFramesObject",
            setOf("*"),
            AvailableInAllFramesMessageListener(listOf(MALICIOUS_URL)),
        )

        webView.loadUrl(
            "https://malicious.com/androidx_webkit/example/assets/www/web_message_listener_malicious.html"
        )
    }

    companion object {
        private const val MALICIOUS_URL = "malicious.com"
        private const val EXAMPLE_URL = "example.com"
        private const val ASSET_PATH = "/androidx_webkit/example/assets/"
    }
}
