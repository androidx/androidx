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
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader

/** A WebViewClient which is used for asset loading activities within the webkit demo app */
open class AssetLoaderWebViewClient(private val assetLoader: WebViewAssetLoader) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
        assetLoader.shouldInterceptRequest(request.url)

    @Deprecated("Intentional") // use the old one for compatibility with all API levels
    override fun shouldInterceptRequest(view: WebView?, url: String?) =
        assetLoader.shouldInterceptRequest(Uri.parse(url))
}
