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
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader

/**
 * An {@link Activity} to showcase a very simple use case of using {@link
 * androidx.webkit.WebViewAssetLoader}.
 */
class AssetLoaderSimpleActivity : AppCompatActivity() {

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
        setTitle(R.string.asset_loader_simple_activity_title)
        setUpDemoAppActivity()

        // Host application assets under http://appassets.androidplatform.net/assets/...
        val assetLoader =
            WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
                .build()

        with(findViewById<WebView>(R.id.webview_asset_loader_webview)) {
            webViewClient = MyWebViewClient(assetLoader)
            loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/www/some_text.html")
        }
    }
}
