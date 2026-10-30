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
import android.system.ErrnoException
import android.view.Choreographer
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.SharedArrayBuffer
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import java.nio.IntBuffer

/**
 * Demonstrates sharing a [SharedArrayBuffer] between the application and the web content running in
 * a [WebView].
 *
 * The buffer is allocated by the application and handed to the page, which passes it on to a worker
 * that doubles the two values it holds. Because the memory is shared rather than copied, the
 * application sees those writes without the page sending anything back, and renders them in plain
 * [TextView]s outside of the WebView.
 */
class SharedArrayBufferActivity : AppCompatActivity() {

    /** View over the memory shared with the web content, or `null` if the demo is unavailable. */
    private var sharedValues: IntBuffer? = null

    private lateinit var nativeValueViews: List<TextView>

    /**
     * Shared memory comes with no change notification: nothing calls back into the application when
     * the web content writes to the buffer. Reading the buffer again is the only way to observe
     * those writes, so this callback does so once per frame for as long as the activity is visible.
     */
    private val frameCallback =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                renderSharedValues()
                Choreographer.getInstance().postFrameCallback(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_shared_array_buffer)
        setTitle(ACTIVITY_TITLE)
        setUpDemoAppActivity()

        if (
            !areAllFeaturesSupported(
                WebViewFeature.CROSS_ORIGIN_ISOLATED_ALLOWLIST,
                WebViewFeature.WEB_MESSAGE_LISTENER,
                WebViewFeature.WEB_MESSAGE_SHARED_ARRAY_BUFFER,
            )
        ) {
            showMessage(R.string.webkit_api_not_available)
            return
        }

        nativeValueViews =
            listOf(findViewById(R.id.native_value_0), findViewById(R.id.native_value_1))

        val sharedArrayBuffer = SharedArrayBuffer.allocate(BUFFER_NAME, BUFFER_SIZE)
        sharedValues =
            try {
                // The mapped buffer already uses the platform byte order, which is also the byte
                // order of the Int32Array the page uses, so the two sides agree on the values.
                sharedArrayBuffer.mapAndCreateByteBuffer().asIntBuffer()
            } catch (e: ErrnoException) {
                showMessage(R.string.webkit_api_not_available)
                return
            }
        renderSharedValues()

        val assetLoader =
            WebViewAssetLoader.Builder()
                .setDomain(BASE_DOMAIN)
                .addPathHandler(ASSETS_PATH, WebViewAssetLoader.AssetsPathHandler(this))
                .build()

        findViewById<WebView>(R.id.webview).apply {
            webViewClient = AssetLoaderWebViewClient(assetLoader)
            settings.javaScriptEnabled = true
            WebViewCompat.getProfile(this).crossOriginIsolatedAllowlist = setOf(BASE_ORIGIN)
            // The page asks for the buffer once it is ready to receive it, which avoids having to
            // guess when the message listener has been installed on the JavaScript side.
            WebViewCompat.addWebMessageListener(this, JS_OBJECT_NAME, setOf(BASE_ORIGIN)) {
                _,
                _,
                _,
                _,
                replyProxy ->
                replyProxy.postMessage(sharedArrayBuffer)
            }
            loadUrl(PAGE_URL)
        }
    }

    override fun onStart() {
        super.onStart()
        if (sharedValues != null) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }

    override fun onStop() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        super.onStop()
    }

    private fun renderSharedValues() {
        val values = sharedValues ?: return
        nativeValueViews.forEachIndexed { index, view ->
            // The two values are read independently, so a frame may show one of them updated
            // before the other. A real application that needs a consistent snapshot has to
            // coordinate with the web content, for example through a counter that JavaScript
            // bumps with Atomics.store() once it has finished writing.
            view.text = getString(R.string.shared_array_buffer_native_value, index, values[index])
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
        const val JS_OBJECT_NAME = "sharedArrayBufferBridge"
        const val BUFFER_NAME = "androidx-webkit-demo"

        /** Enough room for the two `Int32` values the demo shares. */
        const val BUFFER_SIZE = 8L
    }
}
