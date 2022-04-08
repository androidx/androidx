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

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.idling.net.UriIdlingResource;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler;
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler;

/**
 * An {@link Activity} to show a more useful use case: performing ajax calls to load files from
 * local app assets and resources in a safer way using WebViewAssetLoader.
 */
public class AssetLoaderAjaxActivity extends AppCompatActivity {
    private static final int MAX_IDLE_TIME_MS = 5000;

    private static class MyWebViewClient extends WebViewClient {
        private final WebViewAssetLoader mAssetLoader;
        private final UriIdlingResource mUriIdlingResource;

        MyWebViewClient(@NonNull WebViewAssetLoader assetLoader,
                @NonNull UriIdlingResource uriIdlingResource) {
            mAssetLoader = assetLoader;
            mUriIdlingResource = uriIdlingResource;
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
            Uri url = Api21Impl.getUrl(request);
            mUriIdlingResource.beginLoad(url.toString());
            WebResourceResponse response = mAssetLoader.shouldInterceptRequest(url);
            mUriIdlingResource.endLoad(url.toString());
            return response;
        }

        @Override
        @SuppressWarnings("deprecation") // use the old one for compatibility with all API levels.
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            mUriIdlingResource.beginLoad(url);
            WebResourceResponse response = mAssetLoader.shouldInterceptRequest(Uri.parse(url));
            mUriIdlingResource.endLoad(url);
            return response;
        }
    }

    private WebView mWebView;

    // IdlingResource that indicates that WebView has finished loading all WebResourceRequests
    // by waiting until there are no requests made for 5000ms.
    @NonNull
    private final UriIdlingResource mUriIdlingResource =
            new UriIdlingResource("AssetLoaderWebViewUriIdlingResource", MAX_IDLE_TIME_MS);

    @SuppressLint("SetJavaScriptEnabled")
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
        // use "example.com" instead of the default domain
        // Host app resources ... under https://example.com/androidx_webkit/example/res/...
        // Host app assets under https://example.com/androidx_webkit/example/assets/...
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain("example.com") // use "example.com" instead of the default domain
                // Host app resources ... under https://example.com/androidx_webkit/example/res/...
                .addPathHandler("/androidx_webkit/example/res/", new ResourcesPathHandler(this))
                // Host app assets under https://example.com/androidx_webkit/example/assets/...
                .addPathHandler("/androidx_webkit/example/assets/", new AssetsPathHandler(this))
                .build();

        mWebView = findViewById(R.id.webview_asset_loader_webview);
        mWebView.setWebViewClient(new MyWebViewClient(assetLoader, mUriIdlingResource));

        WebSettings webViewSettings = mWebView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        // Setting this off for security. Off by default for SDK versions >= 16.
        webViewSettings.setAllowFileAccessFromFileURLs(false);
        webViewSettings.setAllowUniversalAccessFromFileURLs(false);
        // Keeping these off is less critical but still a good idea, especially
        // if your app is not using file:// or content:// URLs.
        webViewSettings.setAllowFileAccess(false);
        webViewSettings.setAllowContentAccess(false);
    }

    /**
     * Load the url https://example.com/androidx_webkit/example/assets/www/ajax_requests.html.
     */
    public void loadUrl() {
        String mainPageUrl = new Uri.Builder()
                .scheme("https")
                .authority("example.com")
                .appendPath("androidx_webkit").appendPath("example").appendPath("assets")
                .appendPath("www").appendPath("ajax_requests.html")
                .build().toString();
        mWebView.loadUrl(mainPageUrl);
    }

    /**
     * Create and return {@link UriIdlingResource} which indicates if WebView has finished loading
     * all requested URIs.
     */
    @VisibleForTesting
    @NonNull
    public UriIdlingResource getUriIdlingResource() {
        return mUriIdlingResource;
    }
}
