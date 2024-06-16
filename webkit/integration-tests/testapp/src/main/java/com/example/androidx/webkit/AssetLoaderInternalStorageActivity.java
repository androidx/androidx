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

import static java.nio.charset.StandardCharsets.UTF_8;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewAssetLoader.InternalStoragePathHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * An {@link Activity} to show case a use case of using {@link InternalStoragePathHandler}.
 */
public class AssetLoaderInternalStorageActivity extends AppCompatActivity {
    private static final String DEMO_HTML_CONTENT =
            "<h3 id=\"data_success_msg\">Successfully loaded html from app files dir!</h3>";

    @NonNull private File mPublicDir;
    @NonNull private File mDemoFile;
    @NonNull private WebView mWebView;

    private static class MyWebViewClient extends WebViewClient {
        private final WebViewAssetLoader mAssetLoader;

        MyWebViewClient(@NonNull WebViewAssetLoader assetLoader) {
            mAssetLoader = assetLoader;
        }

        /** @noinspection RedundantSuppression*/
        @Override
        @SuppressWarnings("deprecation") // use the old one for compatibility with all API levels.
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return false;
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                            WebResourceRequest request) {
            return mAssetLoader.shouldInterceptRequest(request.getUrl());
        }

        /** @noinspection RedundantSuppression*/
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
        setTitle(R.string.asset_loader_internal_storage_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        mWebView = findViewById(R.id.webview_asset_loader_webview);

        mPublicDir = new File(getFilesDir(), "public");
        mDemoFile = new File(mPublicDir, "some_text.html");

        // Host "files/public/" in app's data directory under:
        // http://appassets.androidplatform.net/public_data/...
        WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder().addPathHandler(
                "/public_data/", new InternalStoragePathHandler(this, mPublicDir)).build();

        mWebView.setWebViewClient(new MyWebViewClient(assetLoader));

        // Write the demo file asynchronously and then load the file after it's written.
        Executors.newSingleThreadExecutor().execute(() -> {
                    writeFileOnBackgroundThread();
                    new Handler(Looper.getMainLooper()).post(this::loadFileAssetInWebView);
                }
        );
    }

    private void writeFileOnBackgroundThread() {
        //noinspection ResultOfMethodCallIgnored
        Objects.requireNonNull(mDemoFile.getParentFile()).mkdirs();
        try (FileOutputStream fos = new FileOutputStream(mDemoFile)) {
            fos.write(DEMO_HTML_CONTENT.getBytes(UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadFileAssetInWebView() {
        Uri path = new Uri.Builder()
                .scheme("https")
                .authority(WebViewAssetLoader.DEFAULT_DOMAIN)
                .appendPath("public_data")
                .appendPath("some_text.html")
                .build();
        mWebView.loadUrl(path.toString());
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean the test/demo file for tests.
        mDemoFile.delete();
        mPublicDir.delete();
    }

}
