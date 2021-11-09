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
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An Activity to demonstrate how to properly display fullscreen web content with WebView.
 */
public class FullscreenActivity extends AppCompatActivity {

    // Use YouTube as an example website, but any site with a video player should suffice to
    // demonstrate fullscreen usage.
    private static final String EXAMPLE_SITE_WITH_VIDEO_PLAYER = "https://m.youtube.com/";

    private WebView mWebView;
    private FullScreenWebChromeClient mWebChromeClient;

    /**
     * An example {@link WebChromeClient} implementation which supports showing web content in
     * fullscreen.
     */
    private static class FullScreenWebChromeClient extends WebChromeClient {
        private Window mWindow;

        // Store the View passed from onShowCustomView in a member variable, because we need to
        // access this again during onHideCustomView().
        private View mFullScreenView;

        // Optional: store the CustomViewCallback in a member variable in case the app needs to
        // force WebView to exit fullscreen mode.
        private CustomViewCallback mCustomViewCallback;

        /* package */ FullScreenWebChromeClient(Window window) {
            mWindow = window;
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // At this point, the WebView is no longer drawing the content. We should cover it up
            // with the new View.
            mFullScreenView = view;
            mWindow.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            mWindow.addContentView(mFullScreenView,
                    new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            mCustomViewCallback = callback;
        }

        @Override
        public void onHideCustomView() {
            // At this point, mFullScreenView is no longer drawing content. Remove this from the
            // layout to show the underlying WebView, and remove the reference to the View so it can
            // be GC'ed.
            mWindow.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            ((ViewGroup) mFullScreenView.getParent()).removeView(mFullScreenView);
            mFullScreenView = null;
            mCustomViewCallback = null;
        }

        /* package */ void exitFullScreen() {
            if (mCustomViewCallback == null) return;
            mCustomViewCallback.onCustomViewHidden();
        }

        /* package */ boolean inFullScreenMode() {
            return mFullScreenView != null;
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);
        setTitle(R.string.fullscreen_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);
        mWebView = findViewById(R.id.webview_supports_fullscreen);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new WebViewClient()); // Open links in this WebView.
        mWebChromeClient = new FullScreenWebChromeClient(getWindow());
        mWebView.setWebChromeClient(mWebChromeClient);

        mWebView.loadUrl(EXAMPLE_SITE_WITH_VIDEO_PLAYER);
    }

    @Override
    public void onBackPressed() {
        if (mWebChromeClient.inFullScreenMode()) {
            mWebChromeClient.exitFullScreen();
        } else if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
