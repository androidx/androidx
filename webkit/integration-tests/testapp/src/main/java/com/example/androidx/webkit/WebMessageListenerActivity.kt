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
import android.text.SpannableString
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** An {@link Activity} to exercise WebMessageListener related functionality. */
class WebMessageListenerActivity : AppCompatActivity() {

    private lateinit var textView: TextView

    private class AssetLoaderWebViewClient(val assetLoader: WebViewAssetLoader) : WebViewClient() {

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest,
        ): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(request.url)
        }

        @Deprecated(
            "Intentional use of deprecated function"
        ) // use the old one for compatibility with all API levels
        override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
            return assetLoader.shouldInterceptRequest(Uri.parse(url))
        }
    }

    private inner class ReplyMessageListener : WebViewCompat.WebMessageListener {

        override fun onPostMessage(
            view: WebView,
            message: WebMessageCompat,
            sourceOrigin: Uri,
            isMainFrame: Boolean,
            replyProxy: JavaScriptReplyProxy,
        ) {
            if (message.data == "initialization") {
                this@WebMessageListenerActivity.findViewById<Button>(R.id.button_reply_proxy)
                    .setOnClickListener { replyProxy.postMessage("ReplyProxy button clicked.") }
            }
        }
    }

    private inner class MessagePortListener : WebViewCompat.WebMessageListener {

        override fun onPostMessage(
            view: WebView,
            message: WebMessageCompat,
            sourceOrigin: Uri,
            isMainFrame: Boolean,
            replyProxy: JavaScriptReplyProxy,
        ) {
            if (message.data == "send port") {
                this@WebMessageListenerActivity.findViewById<Button>(R.id.button_port)
                    .setOnClickListener {
                        message.ports!![0].postMessage(WebMessageCompat("Port button clicked."))
                    }
            }
        }
    }

    private inner class ToastMessageListener : WebViewCompat.WebMessageListener {

        override fun onPostMessage(
            view: WebView,
            message: WebMessageCompat,
            sourceOrigin: Uri,
            isMainFrame: Boolean,
            replyProxy: JavaScriptReplyProxy,
        ) {
            Toast.makeText(
                    this@WebMessageListenerActivity,
                    "Toast: ${message.data}",
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    private inner class MultipleMessagesListener : WebViewCompat.WebMessageListener {

        private var counter = 0

        override fun onPostMessage(
            view: WebView,
            message: WebMessageCompat,
            sourceOrigin: Uri,
            isMainFrame: Boolean,
            replyProxy: JavaScriptReplyProxy,
        ) {
            when (message.type) {
                WebMessageCompat.TYPE_STRING -> replyProxy.postMessage(message.data!!)
                WebMessageCompat.TYPE_ARRAY_BUFFER -> replyProxy.postMessage(message.arrayBuffer)
                else -> throw IllegalArgumentException("Invalid WebMessage type")
            }
            counter++
            if (counter % 100 == 0) {
                this@WebMessageListenerActivity.findViewById<TextView>(R.id.textview).text =
                    "${createNativeTitle()}\n$counter messages received."
            }
        }
    }

    private class NativeFeatureInterface {
        @Suppress("UNUSED") // used from JavaScript
        @JavascriptInterface
        fun isArrayBufferSupported(): Boolean =
            WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_ARRAY_BUFFER)
    }

    private fun createNativeTitle(): CharSequence {
        val title = "Native View\n"
        val ss = SpannableString(title)
        ss.setSpan(
            AbsoluteSizeSpan(55, true),
            0,
            title.length - 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        return ss
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_web_message_listener)
        setTitle(R.string.web_message_listener_activity_title)
        setUpDemoAppActivity()

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        val assetLoader =
            WebViewAssetLoader.Builder()
                .setDomain("example.com")
                .addPathHandler(
                    "/androidx_webkit/example/assets/",
                    WebViewAssetLoader.AssetsPathHandler(this),
                )
                .build()

        textView = findViewById<TextView>(R.id.textview).apply { text = createNativeTitle() }

        val webView =
            findViewById<WebView>(R.id.webview).apply {
                webViewClient = AssetLoaderWebViewClient(assetLoader)
                settings.javaScriptEnabled = true
                addJavascriptInterface(NativeFeatureInterface(), "nativeFeatures")
            }

        val allowedOriginRules = setOf("https://example.com")
        WebViewCompat.addWebMessageListener(
            webView,
            "replyObject",
            allowedOriginRules,
            ReplyMessageListener(),
        )
        WebViewCompat.addWebMessageListener(
            webView,
            "replyWithMessagePortObject",
            allowedOriginRules,
            MessagePortListener(),
        )
        WebViewCompat.addWebMessageListener(
            webView,
            "toastObject",
            allowedOriginRules,
            ToastMessageListener(),
        )
        WebViewCompat.addWebMessageListener(
            webView,
            "multipleMessagesObject",
            allowedOriginRules,
            MultipleMessagesListener(),
        )
        webView.loadUrl(
            "https://example.com/androidx_webkit/example/assets/www/web_message_listener.html"
        )
    }
}
