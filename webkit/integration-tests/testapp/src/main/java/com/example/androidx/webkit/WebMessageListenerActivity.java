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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.JavaScriptReplyProxy;
import androidx.webkit.WebMessageCompat;
import androidx.webkit.WebMessagePortCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;

import java.util.Arrays;
import java.util.HashSet;

/**
 * An {@link Activity} to exercise WebMessageListener related functionality.
 */
public class WebMessageListenerActivity extends AppCompatActivity {
    private TextView mTextView;
    private final Uri mExampleUri = new Uri.Builder()
            .scheme("https")
            .authority("example.com")
            .appendPath("androidx_webkit")
            .appendPath("example")
            .appendPath("assets")
            .build();
    private Button mReplyProxyButton;
    private Button mPortButton;

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
        public void onPostMessage(@NonNull WebView view, @NonNull WebMessageCompat message,
                @NonNull Uri sourceOrigin,
                boolean isMainFrame, @NonNull JavaScriptReplyProxy replyProxy) {
            if (message.getData().equals("initialization")) {
                mReplyProxy = replyProxy;
            }
        }
    }

    private static class MessagePortMessageListener implements WebViewCompat.WebMessageListener {
        private WebMessagePortCompat mPort;

        MessagePortMessageListener(Button button) {
            button.setOnClickListener((View v) -> {
                if (mPort == null) return;
                mPort.postMessage(new WebMessageCompat("Port button clicked."));
            });
        }

        @Override
        public void onPostMessage(@NonNull WebView view, @NonNull WebMessageCompat message,
                @NonNull Uri sourceOrigin,
                boolean isMainFrame, @NonNull JavaScriptReplyProxy replyProxy) {
            if (message.getData().equals("send port")) {
                mPort = message.getPorts()[0];
            }
        }
    }

    private static class ToastMessageListener implements WebViewCompat.WebMessageListener {
        private final Context mContext;

        ToastMessageListener(Context context) {
            mContext = context;
        }

        @Override
        public void onPostMessage(@NonNull WebView view, @NonNull WebMessageCompat message,
                @NonNull Uri sourceOrigin,
                boolean isMainFrame, @NonNull JavaScriptReplyProxy replyProxy) {
            Toast.makeText(mContext, "Toast: " + message.getData(), Toast.LENGTH_SHORT).show();
        }
    }

    private static class MultipleMessagesListener implements WebViewCompat.WebMessageListener {
        private final TextView mTextView;
        private int mCounter = 0;

        MultipleMessagesListener(TextView textView) {
            mTextView = textView;
        }

        @Override
        public void onPostMessage(@NonNull WebView view, @NonNull WebMessageCompat message,
                @NonNull Uri sourceOrigin,
                boolean isMainFrame, @NonNull JavaScriptReplyProxy replyProxy) {
            replyProxy.postMessage(message.getData());
            mCounter++;
            if (mCounter % 100 == 0) {
                mTextView.setText(TextUtils.concat(
                        createNativeTitle(), "\n", "" + mCounter + " messages received."));
            }
        }
    }

    @SuppressLint("SetJavascriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_message_listener);
        setTitle(R.string.web_message_listener_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebkitHelpers.showMessageInActivity(
                    WebMessageListenerActivity.this, R.string.webkit_api_not_available);
            return;
        }

        // Use WebViewAssetLoader to load html page from app's assets.
        WebViewAssetLoader assetLoader =
                new WebViewAssetLoader.Builder()
                        .setDomain("example.com")
                        .addPathHandler(mExampleUri.getPath() + "/", new AssetsPathHandler(this))
                        .build();

        mTextView = findViewById(R.id.textview);
        mTextView.setText(createNativeTitle());

        mReplyProxyButton = findViewById(R.id.button_reply_proxy);
        mPortButton = findViewById(R.id.button_port);

        WebView webView = findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebViewClient(assetLoader));
        webView.getSettings().setJavaScriptEnabled(true);

        HashSet<String> allowedOriginRules = new HashSet<>(Arrays.asList("https://example.com"));
        // Add WebMessageListeners.
        WebViewCompat.addWebMessageListener(webView, "replyObject", allowedOriginRules,
                new ReplyMessageListener(mReplyProxyButton));
        WebViewCompat.addWebMessageListener(webView, "replyWithMessagePortObject",
                allowedOriginRules, new MessagePortMessageListener(mPortButton));
        WebViewCompat.addWebMessageListener(
                webView, "toastObject", allowedOriginRules, new ToastMessageListener(this));
        WebViewCompat.addWebMessageListener(webView, "multipleMessagesObject", allowedOriginRules,
                new MultipleMessagesListener(mTextView));

        webView.loadUrl(
                Uri.withAppendedPath(mExampleUri, "www/web_message_listener.html").toString());
    }

    static CharSequence createNativeTitle() {
        final String title = "Native View";
        SpannableString ss = new SpannableString(title);
        ss.setSpan(new AbsoluteSizeSpan(55, true), 0, title.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ss;
    }
}
