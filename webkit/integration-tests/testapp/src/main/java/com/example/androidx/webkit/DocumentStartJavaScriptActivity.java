/*
 * Copyright 2020 The Android Open Source Project
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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.Collections;
import java.util.HashSet;

/**
 * An {@link Activity} to exercise
 * {@link WebViewCompat#addDocumentStartJavaScript(WebView, String, java.util.Set)} related
 * functionality.
 */
public class DocumentStartJavaScriptActivity extends AppCompatActivity {
    private final Uri mExampleUri = new Uri.Builder()
                                            .scheme("https")
                                            .authority("example.com")
                                            .appendPath("androidx_webkit")
                                            .appendPath("example")
                                            .appendPath("assets")
                                            .build();

    private static class MyWebViewClient extends WebViewClient {
        private final WebViewAssetLoader mAssetLoader;

        MyWebViewClient(WebViewAssetLoader loader) {
            mAssetLoader = loader;
        }

        @Override
        @RequiresApi(21)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(Api21Impl.getUrl(request));
        }

        @Override
        @SuppressWarnings("deprecation") // use the old one for compatibility with all API levels.
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return mAssetLoader.shouldInterceptRequest(Uri.parse(url));
        }
    }

    private static class ReplyMessageListener implements WebViewCompat.WebMessageListener {
        private JavaScriptReplyProxy mReplyProxy;

        ReplyMessageListener(Button button) {
            button.setOnClickListener((View v) -> {
                if (mReplyProxy == null) return;
                mReplyProxy.postMessage("ReplyProxy button clicked.");
            });
        }

        @Override
        public void onPostMessage(@NonNull WebView view, WebMessageCompat message,
                @NonNull Uri sourceOrigin,
                boolean isMainFrame, @NonNull JavaScriptReplyProxy replyProxy) {
            if ("initialization".equals(message.getData())) {
                mReplyProxy = replyProxy;
            }
        }
    }

    @SuppressLint("SetJavascriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_document_start_javascript);
        setTitle(R.string.document_start_javascript_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebkitHelpers.showMessageInActivity(
                    DocumentStartJavaScriptActivity.this, R.string.webkit_api_not_available);
            return;
        }

        // Use WebViewAssetLoader to load html page from app's assets.
        WebViewAssetLoader assetLoader =
                new WebViewAssetLoader.Builder()
                        .setDomain("example.com")
                        .addPathHandler(mExampleUri.getPath() + "/", new AssetsPathHandler(this))
                        .build();

        Button replyProxyButton = findViewById(R.id.button_reply_proxy);

        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebViewClient(assetLoader));
        webView.getSettings().setJavaScriptEnabled(true);

        HashSet<String> allowedOriginRules = new HashSet<>(
                Collections.singletonList("https://example.com"));
        // Add WebMessageListeners.
        WebViewCompat.addWebMessageListener(webView, "replyObject", allowedOriginRules,
                new ReplyMessageListener(replyProxyButton));
        final String jsCode = "replyObject.onmessage = function(event) {"
                + "    document.getElementById('result').innerHTML = event.data;"
                + "};"
                + "replyObject.postMessage('initialization');";
        WebViewCompat.addDocumentStartJavaScript(webView, jsCode, allowedOriginRules);
        webView.loadUrl(
                Uri.withAppendedPath(mExampleUri, "www/document_start_javascript.html").toString());
    }
}
