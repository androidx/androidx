/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.androidx.webkit;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

/**
 * An {@link Activity} to show case a very simple use case of using
 * {@link androidx.webkit.WebViewAssetLoader}.
 */
public class AssetLoaderSimpleActivity extends AppCompatActivity {

    private static class MyWebViewClient extends WebViewClient {
        private final WebViewAssetLoader mAssetLoader;

        MyWebViewClient(@NonNull WebViewAssetLoader assetLoader) {
            mAssetLoader = assetLoader;
        }

        @Override
        @SuppressWarnings("deprecation") // use the old one for compatibility with all API levels.
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        @RequiresApi(21)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request.getUrl());
        }

        @Override
        @SuppressWarnings("deprecation") // use the old one for compatibility with all API levels.
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return mAssetLoader.shouldInterceptRequest(Uri.parse(url));
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_loader);
        setTitle(R.string.asset_loader_simple_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        WebView webView = findViewById(R.id.webview_asset_loader_webview);

        // Host application assets under http://appassets.androidplatform.net/assets/...
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new MyWebViewClient(assetLoader));

        Uri path = new Uri.Builder()
                .scheme("https")
                .authority(WebViewAssetLoader.DEFAULT_DOMAIN)
                .appendPath("assets")
                .appendPath("www")
                .appendPath("some_text.html")
                .build();

        webView.loadUrl(path.toString());
    }
}
