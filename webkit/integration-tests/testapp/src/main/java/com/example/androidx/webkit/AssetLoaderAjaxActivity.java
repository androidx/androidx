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

import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

/**
 * An {@link Activity} to show a more useful usecase: performing ajax calls to load files from
 * local app assets and resources in a safer way using WebViewAssetLoader.
 */
public class AssetLoaderAjaxActivity extends AppCompatActivity {

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @RequiresApi(21)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String request) {
            return mAssetLoader.shouldInterceptRequest(request);
        }
    }

    private WebViewAssetLoader mAssetLoader;
    private WebView mWebView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_asset_loader);
        setTitle(R.string.asset_loader_ajax_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        // The "https://example.com" domain with the virtual path "/androidx_webkit/example/
        // is used to host resources/assets is used for demonstration purpose only.
        // The developer should ALWAYS use a domain which they are in control of or use
        // the default androidplatform.net reserved by Google for this purpose.
        WebViewAssetLoader.Builder assetLoaderBuilder = new WebViewAssetLoader.Builder(this);
        // using the domain "example.com" instead of the default "appassets.androidplatfrom.net"
        assetLoaderBuilder.setDomain("example.com");
        // Host app resources ... under https://example.com/androidx_webkit/example/res/...
        assetLoaderBuilder.setResourcesHostingPath("/androidx_webkit/example/res/");
        // Host app assets under https://example.com/androidx_webkit/example/assets/...
        assetLoaderBuilder.setAssetsHostingPath("/androidx_webkit/example/assets/");
        mAssetLoader = assetLoaderBuilder.build();

        mWebView = findViewById(R.id.webview_asset_loader_webview);
        mWebView.setWebViewClient(new MyWebViewClient());

        WebSettings webViewSettings = mWebView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        // Setting this off for security. Off by default for SDK versions >= 16.
        webViewSettings.setAllowFileAccessFromFileURLs(false);
        webViewSettings.setAllowUniversalAccessFromFileURLs(false);
        // Keeping these off is less critical but still a good idea, especially
        // if your app is not using file:// or content:// URLs.
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);

        Uri path = mAssetLoader.getAssetsHttpsPrefix().buildUpon()
                                        .appendPath("www")
                                        .appendPath("ajax_requests.html").build();
        // Load the url http(s)://example.com/androidx_webkit/example/assets/www/ajax_requests.html
        mWebView.loadUrl(path.toString());
    }
}
