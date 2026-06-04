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
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.util.concurrent.Executors

class AssetLoaderInternalStorageActivity : AppCompatActivity() {

    private val DEMO_HTML_CONTENT =
        "<h3 id=\"data_success_msg\">Successfully loaded html from app files dir!</h3>"

    private lateinit var publicDir: File
    private lateinit var demoFile: File
    private lateinit var webView: WebView

    private class MyWebViewClient(assetLoader: WebViewAssetLoader) :
        AssetLoaderWebViewClient(assetLoader) {

        @Deprecated(
            "Intentional use of deprecated function"
        ) // use the old one for compatibility with all API levels
        override fun shouldOverrideUrlLoading(view: WebView, url: String) = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_asset_loader)
        setTitle(R.string.asset_loader_internal_storage_activity_title)
        setUpDemoAppActivity()

        webView = findViewById(R.id.webview_asset_loader_webview)

        publicDir = File(filesDir, "public")
        demoFile = File(publicDir, "some_text.html")

        // Host "files/public/" in app's data directory under:
        // http://appassets.androidplatform.net/public_data/...
        val assetLoader =
            WebViewAssetLoader.Builder()
                .addPathHandler(
                    "/public_data/",
                    WebViewAssetLoader.InternalStoragePathHandler(this, publicDir),
                )
                .build()

        webView.setWebViewClient(MyWebViewClient(assetLoader))

        // Write the demo file asynchronously and then load the file after it's written.
        Executors.newSingleThreadExecutor().execute {
            writeFileOnBackgroundThread()
            Handler(Looper.getMainLooper()).post(this::loadFileAssetInWebView)
        }
    }

    fun writeFileOnBackgroundThread() {
        demoFile.parentFile!!.mkdirs()
        demoFile.writeText(DEMO_HTML_CONTENT)
    }

    fun loadFileAssetInWebView() {
        webView.loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/public_data/some_text.html")
    }

    override fun onDestroy() {
        super.onDestroy()

        demoFile.delete()
        publicDir.delete()
    }
}
