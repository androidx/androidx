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
import android.webkit.WebView
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * An {@link Activity} to exercise {@link WebViewCompat#addDocumentStartJavaScript(WebView, String,
 * java.util.Set)} related functionality.
 */
class DocumentStartJavaScriptActivity : AppCompatActivity() {

    private inner class ReplyMessageListener : WebViewCompat.WebMessageListener {
        override fun onPostMessage(
            view: WebView,
            message: WebMessageCompat,
            sourceOrigin: Uri,
            isMainFrame: Boolean,
            replyProxy: JavaScriptReplyProxy,
        ) {
            if (message.data.equals("initialization")) {
                this@DocumentStartJavaScriptActivity.findViewById<Button>(R.id.button_reply_proxy)
                    .setOnClickListener { replyProxy.postMessage("ReplyProxy button clicked.") }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_start_javascript)
        setTitle(R.string.document_start_javascript_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val assetLoader =
            WebViewAssetLoader.Builder()
                .setDomain(EXAMPLE_DOMAIN)
                .addPathHandler("$EXAMPLE_PATH/", WebViewAssetLoader.AssetsPathHandler(this))
                .build()

        val webView =
            findViewById<WebView>(R.id.webview).apply {
                webViewClient = AssetLoaderWebViewClient(assetLoader)
                settings.javaScriptEnabled = true
            }

        val allowedOriginRules = setOf("https://$EXAMPLE_DOMAIN")

        // Add WebMessageListeners.
        WebViewCompat.addWebMessageListener(
            webView,
            "replyObject",
            allowedOriginRules,
            ReplyMessageListener(),
        )
        WebViewCompat.addDocumentStartJavaScript(
            webView,
            """
            replyObject.onmessage = function(event) {
                document.getElementById('result').innerHTML = event.data;
            };
            replyObject.postMessage('initialization');
            """
                .trimIndent(),
            allowedOriginRules,
        )

        webView.loadUrl("https://$EXAMPLE_DOMAIN$EXAMPLE_PATH/www/document_start_javascript.html")
    }

    companion object {
        private const val EXAMPLE_DOMAIN = "example.com"
        private const val EXAMPLE_PATH = "/androidx_webkit/example/assets"
    }
}
