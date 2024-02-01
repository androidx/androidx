/*
 * Copyright 2023 The Android Open Source Project
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
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.UserAgentMetadata;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewAssetLoader;
import androidx.webkit.WebViewFeature;

import java.util.Collections;

/**
 * Demo activity to demonstrate the behaviour of overriding user-agent metadata APIs.
 */
public class UserAgentMetadataActivity extends AppCompatActivity {

    private final Uri mExampleUri = new Uri.Builder()
            .scheme("https")
            .authority("example.com")
            .appendPath("androidx_webkit")
            .appendPath("example")
            .appendPath("assets")
            .build();

    /**
     * A WebViewClient to intercept the request to mock HTTPS response.
     */
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

    private WebView mWebView;


    @SuppressLint("SetJavascriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_agent_metadata);

        setTitle(R.string.user_agent_metadata_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        // Check if override user-agent metadata feature is enabled
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }

        mWebView = findViewById(R.id.user_agent_metadata_webview);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebView.getSettings().setJavaScriptEnabled(true);

        RadioGroup radioGroup = findViewById(R.id.user_agent_metadata_radio_group);
        radioGroup.check(R.id.user_agent_metadata_without_override_mode);
        radioGroup.setOnCheckedChangeListener(this::onRadioGroupChanged);

        // Initially send a request without overrides
        refreshView(false);
    }

    private void refreshView(boolean setOverrides) {
        UserAgentMetadata overrideSetting;
        if (setOverrides) {
            UserAgentMetadata.BrandVersion brandVersion = new UserAgentMetadata.BrandVersion
                    .Builder().setBrand("myBrand").setMajorVersion("1").setFullVersion("1.1.1.1")
                    .build();
            overrideSetting = new UserAgentMetadata.Builder()
                    .setBrandVersionList(Collections.singletonList(brandVersion))
                    .setFullVersion("1.1.1.1").setPlatform("myPlatform")
                    .setPlatformVersion("2.2.2.2").setArchitecture("myArch")
                    .setMobile(true).setModel("myModel").setBitness(32)
                    .setWow64(false).build();

        } else {
            overrideSetting = new UserAgentMetadata.Builder().build();
        }
        WebSettingsCompat.setUserAgentMetadata(mWebView.getSettings(), overrideSetting);

        // Use WebViewAssetLoader to load html page from app's assets.
        WebViewAssetLoader assetLoader =
                new WebViewAssetLoader.Builder()
                        .setDomain("example.com")
                        .addPathHandler(mExampleUri.getPath() + "/", new AssetsPathHandler(this))
                        .build();
        mWebView.setWebViewClient(new MyWebViewClient(assetLoader));
        mWebView.loadUrl(Uri.withAppendedPath(mExampleUri,
                "www/user_agent_metadata_main.html").toString());
    }

    /**
     * Handler for selecting w/o user-agent metadata mode through the radio group.
     * @param unused Triggering radio group
     * @param checkedId ID of checked radio button
     */
    public void onRadioGroupChanged(@NonNull RadioGroup unused, int checkedId) {
        refreshView(checkedId == R.id.user_agent_metadata_with_override_mode);
    }
}
