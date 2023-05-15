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

import static androidx.webkit.WebViewAssetLoader.AssetsPathHandler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * An {@link Activity} to show how WebMessageListener deals with malicious websites.
 */
public class WebMessageListenerMaliciousWebsiteActivity extends AppCompatActivity {
    private final Uri mMaliciousUrl = new Uri.Builder().scheme("https").authority(
            "malicious.com").appendPath("androidx_webkit").appendPath("example").appendPath(
            "assets").build();

    private static class MyWebViewClient extends WebViewClient {
        private final WebViewAssetLoader[] mAssetLoaders;

        MyWebViewClient(WebViewAssetLoader[] loaders) {
            mAssetLoaders = loaders;
        }

        @Override
        @RequiresApi(21)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                WebResourceRequest request) {
            for (WebViewAssetLoader loader : mAssetLoaders) {
                WebResourceResponse response = loader.shouldInterceptRequest(
                        Api21Impl.getUrl(request));
                if (response != null) {
                    return response;
                }
            }
            return null;
        }

        @Override
        @SuppressWarnings("deprecation") // use the old one for compatibility with all API levels.
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            for (WebViewAssetLoader loader : mAssetLoaders) {
                WebResourceResponse response = loader.shouldInterceptRequest(Uri.parse(url));
                if (response != null) {
                    return response;
                }
            }
            return null;
        }
    }

    private static class AvailableInAllFramesMessageListener implements
            WebViewCompat.WebMessageListener {
        private final Context mContext;
        private final List<String> mBadAuthorities;

        AvailableInAllFramesMessageListener(Context context, List<String> badAuthorities) {
            mContext = context;
            mBadAuthorities = badAuthorities;
        }

        @Override
        public void onPostMessage(@NonNull WebView view, @NonNull WebMessageCompat message,
                @NonNull Uri sourceOrigin, boolean isMainFrame,
                @NonNull JavaScriptReplyProxy replyProxy) {
            for (String badAuthority : mBadAuthorities) {
                if (sourceOrigin.getAuthority().equals(badAuthority)) {
                    Toast.makeText(mContext, "Message from known bad website, no response.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            replyProxy.postMessage("Reply from app for " + message.getData());
        }
    }

    @SuppressLint("SetJavascriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_message_listener_malicious_website);
        setTitle(R.string.web_message_listener_malicious_website_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebkitHelpers.showMessageInActivity(WebMessageListenerMaliciousWebsiteActivity.this,
                    R.string.webkit_api_not_available);
            return;
        }

        // Use WebViewAssetLoader to load html page from app's assets.
        WebViewAssetLoader assetLoaderMalicious = new WebViewAssetLoader.Builder().setDomain(
                "malicious.com").addPathHandler(mMaliciousUrl.getPath() + "/",
                new AssetsPathHandler(this)).build();
        WebViewAssetLoader assetLoaderGenuine = new WebViewAssetLoader.Builder().setDomain(
                "example.com").addPathHandler("/androidx_webkit/example/assets/",
                new AssetsPathHandler(this)).build();

        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebViewClient(
                new WebViewAssetLoader[]{assetLoaderMalicious, assetLoaderGenuine}));
        webView.getSettings().setJavaScriptEnabled(true);

        // If you only intend to communicate with a limited number of origins, prefer only injecting
        // the listener in those frames.
        WebViewCompat.addWebMessageListener(webView, "restrictedObject",
                new HashSet<>(Arrays.asList("https://example.com")),
                (view, message, sourceOrigin, isMainFrame, replyProxy) ->
                        replyProxy.postMessage("Hello"));

        // If you need to communicate with a wider set of origins but are aware of some origins
        // matching your filter that you need to block communication with, you can check the sending
        // frame's origin on the Java side in onPostMessage().
        WebViewCompat.addWebMessageListener(webView, "allFramesObject",
                new HashSet<>(Arrays.asList("*")),
                new AvailableInAllFramesMessageListener(this, Arrays.asList("malicious.com")));

        webView.loadUrl(Uri.withAppendedPath(mMaliciousUrl,
                "www/web_message_listener_malicious.html").toString());
    }
}
