/*
 * Copyright 2025 The Android Open Source Project
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
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.ProcessGlobalConfig;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.webkit.WebViewStartUpConfig;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executors;

/**
 * An {@link Activity} which makes use of {@link
 * androidx.webkit.ProcessGlobalConfig#setUiThreadStartupMode(Context, int)}.
 */
public class UiThreadStartupModeActivity extends AppCompatActivity {

    @OptIn(markerClass = WebViewCompat.ExperimentalAsyncStartUp.class)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.ui_thread_startup_mode_activity_title);
        WebkitHelpers.appendWebViewVersionToTitle(this);

        if (!WebViewFeature.isStartupFeatureSupported(
            this, WebViewFeature.STARTUP_FEATURE_SET_UI_THREAD_STARTUP_MODE_V2)) {
            WebkitHelpers.showMessageInActivity(this, R.string.webkit_api_not_available);
            return;
        }
        ProcessGlobalConfig config = new ProcessGlobalConfig();
        config.setUiThreadStartupMode(
            this, ProcessGlobalConfig.UI_THREAD_STARTUP_MODE_ASYNC_WITHOUT_MULTI_PROCESS_STARTUP);
        ProcessGlobalConfig.apply(config);
        setContentView(R.layout.activity_ui_thread_startup_mode);

        WebViewStartUpConfig startupConfig =
                new WebViewStartUpConfig.Builder(Executors.newSingleThreadExecutor()).build();
        WebViewCompat.WebViewStartUpCallback callback =
                result -> {
                    WebView wv = new WebView(this);
                    LinearLayout.LayoutParams params =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.MATCH_PARENT);
                    LinearLayout layout =
                            (LinearLayout) findViewById(R.id.ui_thread_startup_mode_webview);
                    layout.addView(wv, params);
                    wv.setWebViewClient(new WebViewClient());
                    wv.loadUrl("www.example.com");
                };

        WebViewCompat.startUpWebView(getApplicationContext(), startupConfig, callback);
    }
}
